package com.booknest.member;

import com.booknest.account.StaffAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "members")
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;

	@Column(length = 254)
	private String email;

	@Column(length = 30)
	private String phone;

	@Column(length = 1000)
	private String notes;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", unique = true)
	private StaffAccount account;

	protected Member() {
	}

	public Member(String fullName, String email, String phone, String notes) {
		this.fullName = fullName;
		this.email = email;
		this.phone = phone;
		this.notes = notes;
	}

	public void updateDetails(String fullName, String email, String phone, String notes) {
		this.fullName = fullName;
		this.email = email;
		this.phone = phone;
		this.notes = notes;
	}

	public void linkAccount(StaffAccount account) {
		this.account = account;
	}

	public void updateLinkedProfile(String fullName, String email, String phone, String notes) {
		updateDetails(fullName, email, phone, notes);
	}

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public String getNotes() {
		return notes;
	}

	public StaffAccount getAccount() {
		return account;
	}
}
