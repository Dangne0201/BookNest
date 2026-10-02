package com.booknest.account;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_events")
public class PasswordResetEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_account_id")
	private StaffAccount actor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "target_account_id", nullable = false)
	private StaffAccount target;

	@Column(name = "event_type", nullable = false, length = 32)
	@Enumerated(jakarta.persistence.EnumType.STRING)
	private EventType eventType;

	private Instant resetAt = Instant.now();

	protected PasswordResetEvent() {
	}

	public PasswordResetEvent(StaffAccount actor, StaffAccount target, EventType eventType) {
		this.actor = actor;
		this.target = target;
		this.eventType = eventType;
	}

	public enum EventType {
		STAFF_CREATED,
		PASSWORD_RESET,
		ADMIN_RECOVERY
	}
}
