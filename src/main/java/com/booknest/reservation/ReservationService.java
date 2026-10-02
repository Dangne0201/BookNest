package com.booknest.reservation;

import java.util.List;

import com.booknest.account.StaffAccount;
import com.booknest.account.StaffAccountRepository;
import com.booknest.book.Book;
import com.booknest.book.BookCopy;
import com.booknest.book.BookCopyRepository;
import com.booknest.book.BookRepository;
import com.booknest.member.Member;
import com.booknest.member.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final StaffAccountRepository staffAccountRepository;
	private final MemberRepository memberRepository;
	private final BookRepository bookRepository;
	private final BookCopyRepository bookCopyRepository;

	public ReservationService(
			ReservationRepository reservationRepository,
			StaffAccountRepository staffAccountRepository,
			MemberRepository memberRepository,
			BookRepository bookRepository,
			BookCopyRepository bookCopyRepository
	) {
		this.reservationRepository = reservationRepository;
		this.staffAccountRepository = staffAccountRepository;
		this.memberRepository = memberRepository;
		this.bookRepository = bookRepository;
		this.bookCopyRepository = bookCopyRepository;
	}

	@Transactional(readOnly = true)
	public List<ReservationResponse> findAll(String username) {
		StaffAccount account = getAccount(username);
		List<Reservation> reservations = account.getRole() == StaffAccount.Role.PATRON
				? reservationRepository.findAllByMemberAccountUsernameOrderByIdDesc(username)
				: reservationRepository.findAllByOrderByIdDesc();
		return reservations.stream().map(this::toResponse).toList();
	}

	@Transactional
	public ReservationResponse create(ReservationRequest request, String username) {
		StaffAccount actor = getAccount(username);
		Book book = bookRepository.findByIdForUpdate(request.bookId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book_not_found"));
		Member member;
		if (actor.getRole() == StaffAccount.Role.PATRON) {
			member = memberRepository.findByAccountUsername(username)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "patron_profile_missing"));
		} else {
			if (request.memberId() == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "member_required");
			}
			member = memberRepository.findById(request.memberId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "member_not_found"));
		}

		if (bookCopyRepository.countByBookIdAndStatus(book.getId(), BookCopy.Status.AVAILABLE) > 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "book_available_for_checkout");
		}

		String activeKey = Reservation.activeKey(member.getId(), book.getId());
		if (reservationRepository.existsByActiveMemberBookKey(activeKey)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "reservation_already_active");
		}
		Reservation reservation = reservationRepository.save(new Reservation(member, book));
		return toResponse(reservation);
	}

	@Transactional
	public void cancel(long reservationId, String username) {
		Reservation initial = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "reservation_not_found"));
		bookRepository.findByIdForUpdate(initial.getBook().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "reservation_book_missing"));
		Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "reservation_not_found"));
		StaffAccount actor = getAccount(username);
		if (actor.getRole() == StaffAccount.Role.PATRON
				&& (reservation.getMember().getAccount() == null
				|| !reservation.getMember().getAccount().getUsername().equals(username))) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "reservation_not_found");
		}
		if (reservation.getStatus() != Reservation.Status.WAITING
				&& reservation.getStatus() != Reservation.Status.HELD) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "reservation_not_active");
		}

		BookCopy heldCopy = reservation.getCopy();
		reservation.cancel();
		if (heldCopy != null) {
			BookCopy copy = bookCopyRepository.findByIdForUpdate(heldCopy.getId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "reservation_copy_missing"));
			copy.updateStatus(BookCopy.Status.AVAILABLE);
			holdNext(reservation.getBook().getId(), copy);
		}
	}

	@Transactional
	public Reservation getHeldForCheckout(long reservationId, String username) {
		Reservation initial = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "reservation_not_found"));
		bookRepository.findByIdForUpdate(initial.getBook().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "reservation_book_missing"));
		Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "reservation_not_found"));
		if (reservation.getMember().getAccount() == null
				|| !reservation.getMember().getAccount().getUsername().equals(username)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "reservation_not_found");
		}
		if (reservation.getStatus() != Reservation.Status.HELD || reservation.getCopy() == null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "reservation_not_ready");
		}
		return reservation;
	}

	@Transactional
	public boolean holdNext(long bookId, BookCopy copy) {
		List<Reservation> waiting = reservationRepository.findWaitingByBookForUpdate(
				bookId,
				Reservation.Status.WAITING
		);
		if (waiting.isEmpty()) {
			copy.updateStatus(BookCopy.Status.AVAILABLE);
			return false;
		}
		waiting.get(0).hold(copy);
		copy.updateStatus(BookCopy.Status.ON_HOLD);
		return true;
	}

	@Transactional
	public void markFulfilled(Reservation reservation) {
		reservation.fulfill();
	}

	private StaffAccount getAccount(String username) {
		return staffAccountRepository.findByUsername(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"));
	}

	private ReservationResponse toResponse(Reservation reservation) {
		Long position = reservation.getStatus() == Reservation.Status.WAITING
				? reservationRepository.countByBookIdAndStatusAndIdLessThan(
						reservation.getBook().getId(),
						Reservation.Status.WAITING,
						reservation.getId()
				) + 1
				: null;
		return new ReservationResponse(
				reservation.getId(),
				reservation.getBook().getId(),
				reservation.getBook().getTitle(),
				reservation.getMember().getId(),
				reservation.getMember().getFullName(),
				reservation.getStatus(),
				position,
				reservation.getCopy() == null ? null : reservation.getCopy().getId(),
				reservation.getCreatedAt()
		);
	}
}
