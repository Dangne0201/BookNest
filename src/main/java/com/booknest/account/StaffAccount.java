package com.booknest.account;

import com.booknest.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "staff_accounts",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_staff_accounts_username",
				columnNames = "username"
		)
)
public class StaffAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 60)
	private String passwordHash;

	@Column(nullable = false, length = 16)
	@Enumerated(EnumType.STRING)
	private Role role = Role.STAFF;

	@Column(name = "password_change_required", nullable = false)
	private boolean passwordChangeRequired;

	@Column(name = "credential_version", nullable = false)
	private long credentialVersion;

	@OneToOne(mappedBy = "account", fetch = jakarta.persistence.FetchType.LAZY)
	private Member member;

	protected StaffAccount() {
	}

	public StaffAccount(String username, String passwordHash) {
		this(username, passwordHash, Role.STAFF, false);
	}

	public StaffAccount(String username, String passwordHash, Role role, boolean passwordChangeRequired) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.role = role;
		this.passwordChangeRequired = passwordChangeRequired;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public boolean isPasswordChangeRequired() {
		return passwordChangeRequired;
	}

	public long getCredentialVersion() {
		return credentialVersion;
	}

	public Member getMember() {
		return member;
	}

	public void linkMember(Member member) {
		this.member = member;
		member.linkAccount(this);
	}

	public void updatePasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
		this.passwordChangeRequired = false;
	}

	public void resetPassword(String passwordHash) {
		this.passwordHash = passwordHash;
		this.passwordChangeRequired = true;
		this.credentialVersion++;
	}

	public void requirePasswordChange() {
		this.passwordChangeRequired = true;
	}

	public void updateRole(Role role) {
		this.role = role;
	}

	public enum Role {
		ADMIN,
		STAFF,
		PATRON
	}
}
