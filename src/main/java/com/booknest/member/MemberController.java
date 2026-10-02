package com.booknest.member;

import java.util.List;
import java.security.Principal;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {

	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@GetMapping
	public List<MemberResponse> findAll() {
		return memberService.findAll();
	}

	@GetMapping("/me")
	public MemberResponse findMine(Principal principal) {
		return memberService.findMine(principal.getName());
	}

	@PutMapping("/me")
	public MemberResponse updateMine(Principal principal, @Valid @RequestBody MemberRequest request) {
		return memberService.updateMine(principal.getName(), request);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MemberResponse create(@Valid @RequestBody MemberRequest request) {
		return memberService.create(request);
	}

	@GetMapping("/{memberId}")
	public MemberResponse findById(@PathVariable long memberId) {
		return memberService.findById(memberId);
	}

	@PutMapping("/{memberId}")
	public MemberResponse update(@PathVariable long memberId, @Valid @RequestBody MemberRequest request) {
		return memberService.update(memberId, request);
	}

	@DeleteMapping("/{memberId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable long memberId) {
		memberService.delete(memberId);
	}
}
