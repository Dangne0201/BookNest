package com.booknest.account;

import java.security.Principal;
import java.util.List;
import java.security.Principal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
public class AdminController {

	private final AccountService accountService;

	public AdminController(AccountService accountService) {
		this.accountService = accountService;
	}

	@GetMapping
	public List<AdminAccountResponse> findAccounts() {
		return accountService.findAccounts();
	}

	@PostMapping("/staff")
	@ResponseStatus(HttpStatus.CREATED)
	public TemporaryPasswordResponse createStaff(@Valid @RequestBody CreateStaffRequest request, Principal principal) {
		return accountService.createStaff(request.username(), principal.getName());
	}

	@PostMapping("/{accountId}/password-reset")
	public TemporaryPasswordResponse resetPassword(@PathVariable long accountId, Principal principal) {
		return accountService.resetPassword(accountId, principal.getName());
	}

	public record CreateStaffRequest(
			@NotBlank
			@Size(min = 3, max = 50)
			String username
	) {
	}
}
