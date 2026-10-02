package com.booknest.account;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_admin_identity")
public class AdminIdentity {

	@Id
	private Short singletonId = 1;

	@OneToOne(optional = false)
	@JoinColumn(name = "account_id", nullable = false, unique = true)
	private StaffAccount account;

	protected AdminIdentity() {
	}

	public AdminIdentity(StaffAccount account) {
		this.account = account;
	}

	public StaffAccount getAccount() {
		return account;
	}
}
