package com.booknest.security;

import java.io.IOException;

import com.booknest.account.StaffAccount;
import com.booknest.account.StaffAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionVersionFilter extends OncePerRequestFilter {

	private final StaffAccountRepository accountRepository;

	public SessionVersionFilter(StaffAccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null
				&& authentication.isAuthenticated()
				&& authentication.getPrincipal() instanceof BookNestUserDetails principal) {
			StaffAccount account = accountRepository.findById(principal.getAccountId()).orElse(null);
			boolean staleTemporaryAuthority = principal.getAuthorities()
					.contains(new SimpleGrantedAuthority("ROLE_PASSWORD_CHANGE_REQUIRED"))
					&& account != null
					&& !account.isPasswordChangeRequired();
			if (account == null
					|| account.getCredentialVersion() != principal.getCredentialVersion()
					|| staleTemporaryAuthority) {
				SecurityContextHolder.clearContext();
				if (request.getSession(false) != null) {
					request.getSession(false).invalidate();
				}
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.setHeader("Cache-Control", "no-store");
				response.getWriter().write("{\"error\":\"session_expired\"}");
				return;
			}
			if (account.isPasswordChangeRequired() && !allowedDuringPasswordChange(request)) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.setHeader("Cache-Control", "no-store");
				response.getWriter().write("{\"error\":\"password_change_required\"}");
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

	private static boolean allowedDuringPasswordChange(HttpServletRequest request) {
		String method = request.getMethod();
		String path = request.getRequestURI();
		return path.equals("/api/auth/csrf") && method.equals("GET")
				|| path.equals("/api/auth/me") && method.equals("GET")
				|| path.equals("/api/auth/password") && method.equals("POST")
				|| path.equals("/api/auth/logout") && method.equals("POST");
	}
}
