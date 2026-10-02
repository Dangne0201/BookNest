package com.booknest.reservation;

import java.security.Principal;
import java.util.List;

import com.booknest.loan.LoanResponse;
import com.booknest.loan.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

	private final ReservationService reservationService;
	private final LoanService loanService;

	public ReservationController(ReservationService reservationService, LoanService loanService) {
		this.reservationService = reservationService;
		this.loanService = loanService;
	}

	@GetMapping
	public List<ReservationResponse> findAll(Principal principal) {
		return reservationService.findAll(principal.getName());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReservationResponse create(
			@Valid @RequestBody ReservationRequest request,
			Principal principal
	) {
		return reservationService.create(request, principal.getName());
	}

	@DeleteMapping("/{reservationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@PathVariable long reservationId, Principal principal) {
		reservationService.cancel(reservationId, principal.getName());
	}

	@PostMapping("/{reservationId}/checkout")
	@ResponseStatus(HttpStatus.CREATED)
	public LoanResponse checkout(@PathVariable long reservationId, Principal principal) {
		return loanService.checkoutReservation(reservationId, principal.getName());
	}
}
