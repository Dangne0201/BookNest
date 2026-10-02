package com.booknest.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AccountController {

	private final AccountService accountService;
	private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public StaffAccountResponse register(@Valid @RequestBody RegistrationRequest request) {
		return accountService.registerPatron(request);
	}

	@PostMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(
			@Valid @RequestBody ChangePasswordRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse
	) {
		accountService.changePassword(authentication.getName(), request);
		logoutHandler.logout(httpRequest, httpResponse, authentication);
	}

	@GetMapping("/csrf")
	public CsrfTokenResponse csrfToken(@RequestAttribute("_csrf") CsrfToken csrfToken) {
		return new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken());
	}

	@GetMapping("/me")
	public StaffAccountResponse currentStaffAccount(Authentication authentication) {
		return accountService.getByUsername(authentication.getName());
	}

	public record CsrfTokenResponse(String headerName, String token) {
	}
}
