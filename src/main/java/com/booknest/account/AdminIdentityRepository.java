package com.booknest.account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminIdentityRepository extends JpaRepository<AdminIdentity, Short> {
}
