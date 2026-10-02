package com.booknest.account;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminRecoveryCommand implements ApplicationRunner {

	private final AccountService accountService;

	public AdminRecoveryCommand(AccountService accountService) {
		this.accountService = accountService;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (args.getOptionValues("booknest.admin-recovery") == null
				|| args.getOptionValues("booknest.admin-recovery").stream().noneMatch(Boolean::parseBoolean)) {
			return;
		}
		TemporaryPasswordResponse recovery = accountService.recoverAdmin();
		System.out.printf(
				"Admin recovery completed for '%s'. Temporary password (shown once): %s%n",
				recovery.username(),
				recovery.temporaryPassword()
		);
	}
}
