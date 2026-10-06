package com.booknest.loan;

import java.security.Principal;
import java.util.List;

import com.booknest.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	public PageResponse<LoanResponse> findAll(
			Principal principal,
			@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "") String state,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "checkoutDate") String sort,
			@RequestParam(defaultValue = "desc") String direction
	) {
		return loanService.findAll(principal.getName(), q, state, page, size, sort, direction);
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

	@PostMapping("/{loanId}/renew")
	public LoanResponse renewLoan(@PathVariable long loanId, Principal principal) {
		return loanService.renewLoan(loanId, principal.getName());
	}
}
