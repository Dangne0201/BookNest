package com.booknest.account;

import java.util.Locale;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import com.booknest.member.Member;
import com.booknest.member.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

	private final StaffAccountRepository staffAccountRepository;
	private final MemberRepository memberRepository;
	private final AdminIdentityRepository adminIdentityRepository;
	private final PasswordResetEventRepository passwordResetEventRepository;
	private final PasswordEncoder passwordEncoder;
	private final SessionRegistry sessionRegistry;
	private final SecureRandom secureRandom = new SecureRandom();

	public AccountService(
			StaffAccountRepository staffAccountRepository,
			MemberRepository memberRepository,
			AdminIdentityRepository adminIdentityRepository,
			PasswordResetEventRepository passwordResetEventRepository,
			PasswordEncoder passwordEncoder,
			SessionRegistry sessionRegistry
	) {
		this.staffAccountRepository = staffAccountRepository;
		this.memberRepository = memberRepository;
		this.adminIdentityRepository = adminIdentityRepository;
		this.passwordResetEventRepository = passwordResetEventRepository;
		this.passwordEncoder = passwordEncoder;
		this.sessionRegistry = sessionRegistry;
	}

	@Transactional
	public StaffAccountResponse registerPatron(RegistrationRequest request) {
		String username = normalizeUsername(request.username());
		assertUsernameAvailable(username);
		StaffAccount account = staffAccountRepository.save(new StaffAccount(
				username,
				passwordEncoder.encode(request.password()),
				StaffAccount.Role.PATRON,
				false
		));
		Member member = new Member(
				request.fullName().trim(),
				trimToNull(request.email()),
				trimToNull(request.phone()),
				trimToNull(request.notes())
		);
		account.linkMember(member);
		memberRepository.save(member);
		return toResponse(account);
	}

	@Transactional
	public TemporaryPasswordResponse createStaff(String username, String actorUsername) {
		String normalizedUsername = normalizeUsername(username);
		assertUsernameAvailable(normalizedUsername);
		String temporaryPassword = generateTemporaryPassword();
		StaffAccount account = staffAccountRepository.save(new StaffAccount(
				normalizedUsername,
				passwordEncoder.encode(temporaryPassword),
				StaffAccount.Role.STAFF,
				true
		));
		StaffAccount actor = staffAccountRepository.findByUsername(normalizeUsername(actorUsername))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"));
		passwordResetEventRepository.save(new PasswordResetEvent(
				actor,
				account,
				PasswordResetEvent.EventType.STAFF_CREATED
		));
		return new TemporaryPasswordResponse(account.getId(), account.getUsername(), temporaryPassword);
	}

	@Transactional(readOnly = true)
	public List<AdminAccountResponse> findAccounts() {
		return staffAccountRepository.findAllByOrderByUsernameAsc().stream()
				.map(account -> new AdminAccountResponse(
						account.getId(),
						account.getUsername(),
						account.getRole(),
						account.isPasswordChangeRequired(),
						account.getMember() == null ? null : account.getMember().getFullName()
				))
				.toList();
	}

	@Transactional
	public TemporaryPasswordResponse resetPassword(long targetId, String actorUsername) {
		StaffAccount actor = staffAccountRepository.findByUsername(normalizeUsername(actorUsername))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"));
		StaffAccount target = staffAccountRepository.findById(targetId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "account_not_found"));
		if (target.getRole() == StaffAccount.Role.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin_recovery_requires_host_access");
		}

		String temporaryPassword = generateTemporaryPassword();
		target.resetPassword(passwordEncoder.encode(temporaryPassword));
		passwordResetEventRepository.save(new PasswordResetEvent(
				actor,
				target,
				PasswordResetEvent.EventType.PASSWORD_RESET
		));
		expireSessions(target.getUsername());
		return new TemporaryPasswordResponse(target.getId(), target.getUsername(), temporaryPassword);
	}

	@Transactional
	public TemporaryPasswordResponse recoverAdmin() {
		AdminIdentity identity = adminIdentityRepository.findById((short) 1).orElse(null);
		StaffAccount account;
		if (identity != null) {
			account = identity.getAccount();
		} else {
			if (staffAccountRepository.existsByRole(StaffAccount.Role.ADMIN)) {
				throw new IllegalStateException("An administrator exists without the system admin identity.");
			}
			account = staffAccountRepository.findByUsername("admin").orElseGet(() ->
					staffAccountRepository.save(new StaffAccount(
							"admin",
							passwordEncoder.encode(generateTemporaryPassword()),
							StaffAccount.Role.ADMIN,
							true
					)));
			account.updateRole(StaffAccount.Role.ADMIN);
			if (!account.isPasswordChangeRequired()) {
				account.requirePasswordChange();
			}
			adminIdentityRepository.save(new AdminIdentity(account));
		}

		String temporaryPassword = generateTemporaryPassword();
		account.resetPassword(passwordEncoder.encode(temporaryPassword));
		passwordResetEventRepository.save(new PasswordResetEvent(
				null,
				account,
				PasswordResetEvent.EventType.ADMIN_RECOVERY
		));
		expireSessions(account.getUsername());
		return new TemporaryPasswordResponse(account.getId(), account.getUsername(), temporaryPassword);
	}

	@Transactional(readOnly = true)
	public StaffAccountResponse getByUsername(String username) {
		return staffAccountRepository.findByUsername(normalizeUsername(username))
				.map(AccountService::toResponse)
				.orElseThrow(() -> new IllegalStateException("Authenticated staff account was not found."));
	}

	@Transactional
	public void changePassword(String username, ChangePasswordRequest request) {
		StaffAccount account = staffAccountRepository.findByUsername(normalizeUsername(username))
				.orElseThrow(() -> new IllegalStateException("Authenticated staff account was not found."));
		if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_current_password");
		}
		if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password_unchanged");
		}

		account.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
	}

	public static String normalizeUsername(String username) {
		return username.trim().toLowerCase(Locale.ROOT);
	}

	private void assertUsernameAvailable(String username) {
		if (username.equals("admin") || staffAccountRepository.existsByUsername(username)) {
			throw new StaffAccountAlreadyExistsException(username);
		}
	}

	private String generateTemporaryPassword() {
		byte[] randomBytes = new byte[24];
		secureRandom.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	private void expireSessions(String username) {
		for (Object principal : sessionRegistry.getAllPrincipals()) {
			if (principal instanceof UserDetails details && details.getUsername().equals(username)) {
				for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
					session.expireNow();
				}
			}
		}
	}

	private static String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private static StaffAccountResponse toResponse(StaffAccount account) {
		return new StaffAccountResponse(
				account.getId(),
				account.getUsername(),
				account.getRole(),
				account.getMember() == null ? null : account.getMember().getId(),
				account.isPasswordChangeRequired()
		);
	}
}
