package com.booknest.account;

public record TemporaryPasswordResponse(long accountId, String username, String temporaryPassword) {
}
