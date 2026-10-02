package com.booknest.account;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAccountRepository extends JpaRepository<StaffAccount, Long> {

	Optional<StaffAccount> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByRole(StaffAccount.Role role);

	List<StaffAccount> findAllByOrderByUsernameAsc();
}
