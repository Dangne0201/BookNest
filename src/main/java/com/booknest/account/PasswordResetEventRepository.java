package com.booknest.account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetEventRepository extends JpaRepository<PasswordResetEvent, Long> {
}
