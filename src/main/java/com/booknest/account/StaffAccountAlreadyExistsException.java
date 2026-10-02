package com.booknest.account;

public class StaffAccountAlreadyExistsException extends RuntimeException {

	public StaffAccountAlreadyExistsException(String username) {
		super("A staff account with username '%s' already exists.".formatted(username));
	}
}
