package com.booknest.security;

import com.booknest.account.AccountService;
import com.booknest.account.StaffAccountRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {

	@Bean
	SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(StaffAccountRepository staffAccountRepository) {
		return username -> staffAccountRepository
				.findByUsername(AccountService.normalizeUsername(username))
				.map(account -> new BookNestUserDetails(
						account.getId(),
						account.getUsername(),
						account.getPasswordHash(),
						account.getCredentialVersion(),
						account.isPasswordChangeRequired()
								? "ROLE_PASSWORD_CHANGE_REQUIRED"
								: "ROLE_" + account.getRole().name()
				))
				.orElseThrow(() -> new UsernameNotFoundException("Invalid username or password."));
	}

	@Bean
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	SecurityFilterChain securityFilterChain(
			org.springframework.security.config.annotation.web.builders.HttpSecurity http,
			SessionRegistry sessionRegistry,
			StaffAccountRepository staffAccountRepository
	) throws Exception {
		HttpSessionCsrfTokenRepository csrfTokenRepository = new HttpSessionCsrfTokenRepository();
		csrfTokenRepository.setHeaderName("X-CSRF-TOKEN");

		return http
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfTokenRepository)
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
						.requestMatchers("/api/auth/login").permitAll()
						.requestMatchers("/api/auth/password", "/api/auth/me", "/api/auth/logout").authenticated()
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/", "/index.html", "/favicon.ico", "/css/**", "/js/**", "/images/**")
						.permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/members/me").hasAnyRole("PATRON", "STAFF", "ADMIN")
						.requestMatchers("/api/members/**").hasAnyRole("STAFF", "ADMIN")
						.requestMatchers("/api/activity/**").hasAnyRole("PATRON", "STAFF", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/loans/*/return").hasAnyRole("STAFF", "ADMIN")
						.requestMatchers("/api/loans/**").hasAnyRole("PATRON", "STAFF", "ADMIN")
						.requestMatchers("/api/reservations/**").hasAnyRole("PATRON", "STAFF", "ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/books", "/api/books/**").permitAll()
						.requestMatchers("/api/books/**").hasAnyRole("STAFF", "ADMIN")
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll())
				.formLogin(form -> form
						.loginProcessingUrl("/api/auth/login")
						.usernameParameter("username")
						.passwordParameter("password")
						.successHandler((request, response, authentication) -> {
							response.setStatus(HttpServletResponse.SC_OK);
							response.setContentType("application/json");
							response.getWriter().write("{\"authenticated\":true}");
						})
						.failureHandler((request, response, exception) -> {
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setContentType("application/json");
							response.getWriter().write("{\"error\":\"invalid_credentials\"}");
						}))
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.logoutSuccessHandler((request, response, authentication) ->
								response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
				.sessionManagement(session -> session
						.sessionFixation(fixation -> fixation.migrateSession())
						.maximumSessions(-1)
						.sessionRegistry(sessionRegistry)
						.expiredSessionStrategy(event -> {
							event.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							event.getResponse().setContentType("application/json");
							event.getResponse().getWriter().write("{\"error\":\"session_expired\"}");
						}))
				.addFilterAfter(new SessionVersionFilter(staffAccountRepository),
						org.springframework.security.web.context.SecurityContextHolderFilter.class)
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) -> {
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setContentType("application/json");
							response.getWriter().write("{\"error\":\"unauthorized\"}");
						})
						.accessDeniedHandler((request, response, exception) -> {
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							response.setContentType("application/json");
							response.getWriter().write("{\"error\":\"forbidden\"}");
						}))
				.build();
	}
}
