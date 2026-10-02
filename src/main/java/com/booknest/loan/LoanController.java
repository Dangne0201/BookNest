package com.booknest.loan;

import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

	private final LoanService loanService;

	public LoanController(LoanService loanService) {
		this.loanService = loanService;
	}

	@GetMapping
	public List<LoanResponse> findAll(Principal principal) {
		return loanService.findAll(principal.getName());
	}

	@GetMapping("/{loanId}")
	public LoanResponse findById(@PathVariable long loanId, Principal principal) {
		return loanService.findById(loanId, principal.getName());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LoanResponse checkout(@Valid @RequestBody CheckoutRequest request, Principal principal) {
		return loanService.checkout(request, principal.getName());
	}

	@PostMapping("/{loanId}/return")
	public LoanResponse returnLoan(@PathVariable long loanId, Principal principal) {
		return loanService.returnLoan(loanId, principal.getName());
	}
}
