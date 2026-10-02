package com.booknest.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class BookNestUserDetails implements UserDetails {

	private final long accountId;
	private final String username;
	private final String passwordHash;
	private final long credentialVersion;
	private final Collection<? extends GrantedAuthority> authorities;

	public BookNestUserDetails(
			long accountId,
			String username,
			String passwordHash,
			long credentialVersion,
			String authority
	) {
		this.accountId = accountId;
		this.username = username;
		this.passwordHash = passwordHash;
		this.credentialVersion = credentialVersion;
		this.authorities = List.of(new SimpleGrantedAuthority(authority));
	}

	public long getAccountId() {
		return accountId;
	}

	public long getCredentialVersion() {
		return credentialVersion;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BookNestUserDetails details && username.equals(details.username);
	}

	@Override
	public int hashCode() {
		return username.hashCode();
	}
}
