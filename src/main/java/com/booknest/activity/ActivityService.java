package com.booknest.activity;

import java.util.Locale;

import com.booknest.account.StaffAccount;
import com.booknest.account.StaffAccountRepository;
import com.booknest.account.StaffAccount.Role;
import com.booknest.common.PageRequestFactory;
import com.booknest.common.PageResponse;
import com.booknest.loan.Loan;
import com.booknest.reservation.Reservation;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivityService {

	private final ActivityEventRepository activityEventRepository;
	private final StaffAccountRepository staffAccountRepository;

	public ActivityService(
			ActivityEventRepository activityEventRepository,
			StaffAccountRepository staffAccountRepository
	) {
		this.activityEventRepository = activityEventRepository;
		this.staffAccountRepository = staffAccountRepository;
	}

	@Transactional
	public void recordLoanEvent(ActivityEventType type, Loan loan, String actorUsername) {
		activityEventRepository.save(new ActivityEvent(
				type,
				actorUsername,
				loan.getMember(),
				null,
				loan.getBookCopy(),
				loan.getId(),
				null
		));
	}

	@Transactional
	public void recordReservationEvent(ActivityEventType type, Reservation reservation, String actorUsername) {
		activityEventRepository.save(new ActivityEvent(
				type,
				actorUsername,
				reservation.getMember(),
				reservation.getBook(),
				reservation.getCopy(),
				null,
				reservation.getId()
		));
	}

	@Transactional(readOnly = true)
	public PageResponse<ActivityResponse> findAll(
			String username,
			String query,
			String eventType,
			int page,
			int size
	) {
		StaffAccount account = staffAccountRepository.findByUsername(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"));
		ActivityEventType type = parseType(eventType);
		PageRequest pageable = PageRequestFactory.create(
				page,
				size,
				"occurredAt",
				"desc",
				java.util.Map.of("occurredAt", "occurredAt"),
				"occurredAt",
				"desc"
		);
		return PageResponse.from(activityEventRepository.search(
				account.getRole() == Role.PATRON,
				account.getId(),
				type,
				query == null ? "" : query.trim(),
				pageable
		).map(ActivityService::toResponse));
	}

	private static ActivityEventType parseType(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return ActivityEventType.valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_activity_type");
		}
	}

	private static ActivityResponse toResponse(ActivityEvent event) {
		return new ActivityResponse(
				event.getId(),
				event.getType(),
				event.getOccurredAt(),
				event.getActorUsername(),
				event.getMemberId(),
				event.getMemberName(),
				event.getBookId(),
				event.getBookTitle(),
				event.getCopyId(),
				event.getLoanId(),
				event.getReservationId()
		);
	}
}
