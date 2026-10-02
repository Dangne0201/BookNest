package com.booknest.member;

import java.util.List;

import com.booknest.loan.LoanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemberService {

	private final MemberRepository memberRepository;
	private final LoanRepository loanRepository;

	public MemberService(MemberRepository memberRepository, LoanRepository loanRepository) {
		this.memberRepository = memberRepository;
		this.loanRepository = loanRepository;
	}

	@Transactional(readOnly = true)
	public List<MemberResponse> findAll() {
		return memberRepository.findAllByOrderByFullNameAscIdAsc().stream()
				.map(MemberService::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public MemberResponse findById(long id) {
		return toResponse(getMember(id));
	}

	@Transactional(readOnly = true)
	public MemberResponse findMine(String username) {
		return memberRepository.findByAccountUsername(username)
				.map(MemberService::toResponse)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "member_not_found"));
	}

	@Transactional
	public MemberResponse updateMine(String username, MemberRequest request) {
		Member member = memberRepository.findByAccountUsername(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "member_not_found"));
		member.updateLinkedProfile(
				request.fullName().trim(),
				trimToNull(request.email()),
				trimToNull(request.phone()),
				trimToNull(request.notes())
		);
		return toResponse(member);
	}

	@Transactional
	public MemberResponse create(MemberRequest request) {
		Member member = new Member(
				request.fullName().trim(),
				trimToNull(request.email()),
				trimToNull(request.phone()),
				trimToNull(request.notes())
		);
		return toResponse(memberRepository.save(member));
	}

	@Transactional
	public MemberResponse update(long id, MemberRequest request) {
		Member member = getMember(id);
		member.updateDetails(
				request.fullName().trim(),
				trimToNull(request.email()),
				trimToNull(request.phone()),
				trimToNull(request.notes())
		);
		return toResponse(memberRepository.save(member));
	}

	@Transactional
	public void delete(long id) {
		Member member = getMember(id);
		if (member.getAccount() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "member_account_linked");
		}
		if (loanRepository.existsByMemberId(id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "member_has_loan_history");
		}
		memberRepository.delete(member);
	}

	private Member getMember(long id) {
		return memberRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "member_not_found"));
	}

	private static MemberResponse toResponse(Member member) {
		return new MemberResponse(
				member.getId(),
				member.getFullName(),
				member.getEmail(),
				member.getPhone(),
				member.getNotes()
		);
	}

	private static String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
