package com.booknest.demo;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "demo_seed_runs")
public class DemoSeedRun {

	@Id
	@Column(name = "seed_key", nullable = false, length = 50)
	private String seedKey;

	@Column(name = "completed_at", nullable = false)
	private Instant completedAt;

	protected DemoSeedRun() {
	}

	public DemoSeedRun(String seedKey) {
		this.seedKey = seedKey;
		this.completedAt = Instant.now();
	}
}
