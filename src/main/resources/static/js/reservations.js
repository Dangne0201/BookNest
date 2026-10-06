import { apiRequest, userMessage } from "./api.js";

export function initializeReservations(showToast, onBooksChanged, onLoansChanged, onActivityChanged = () => Promise.resolve()) {
	const list = document.querySelector("#reservations-list");
	const empty = document.querySelector("#reservations-empty");
	const feedback = document.querySelector("#reservations-feedback");
	let role = "PATRON";

	function setFeedback(message, isError = false) {
		feedback.textContent = message;
		feedback.classList.toggle("error", isError);
		feedback.classList.toggle("success", !isError);
		feedback.hidden = !message;
	}

	function renderReservation(reservation) {
		const item = document.createElement("article");
		item.className = "reservation-card";
		const details = document.createElement("div");
		const title = document.createElement("strong");
		title.textContent = reservation.bookTitle;
		const status = document.createElement("p");
		status.className = "muted";
		status.textContent = reservation.status === "HELD"
			? `Đã giữ bản sách #${reservation.copyId} cho bạn.`
			: reservation.status === "WAITING"
				? `Đang chờ${reservation.queuePosition ? ` — vị trí ${reservation.queuePosition}` : ""}.`
				: reservation.status === "FULFILLED" ? "Đã chuyển thành lượt mượn." : "Đã hủy.";
		details.append(title, status);
		item.append(details);

		const actions = document.createElement("div");
		actions.className = "row-actions";
		if (reservation.status === "HELD" && role === "PATRON") {
			const checkout = document.createElement("button");
			checkout.type = "button";
			checkout.className = "button button-primary button-small";
			checkout.textContent = "Nhận sách";
			checkout.addEventListener("click", async () => {
				checkout.disabled = true;
				try {
					await apiRequest(`/api/reservations/${reservation.id}/checkout`, { method: "POST" });
					showToast("Đã mượn sách theo lượt đặt trước.");
					await Promise.all([
						loadReservations(),
						onBooksChanged(),
						onLoansChanged(),
						onActivityChanged()
					]);
				} catch (error) {
					showToast(userMessage(error), true);
					checkout.disabled = false;
				}
			});
			actions.append(checkout);
		}
		if (reservation.status === "WAITING" || reservation.status === "HELD") {
			const cancel = document.createElement("button");
			cancel.type = "button";
			cancel.className = "button button-quiet button-small";
			cancel.textContent = "Hủy đặt trước";
			cancel.addEventListener("click", async () => {
				if (!window.confirm(`Hủy yêu cầu đặt trước “${reservation.bookTitle}”?`)) {
					return;
				}
				cancel.disabled = true;
				try {
					await apiRequest(`/api/reservations/${reservation.id}`, { method: "DELETE" });
					await Promise.all([loadReservations(), onBooksChanged(), onActivityChanged()]);
					showToast("Đã hủy yêu cầu đặt trước.");
				} catch (error) {
					showToast(userMessage(error), true);
					cancel.disabled = false;
				}
			});
			actions.append(cancel);
		}
		item.append(actions);
		return item;
	}

	async function loadReservations() {
		setFeedback("");
		try {
			const reservations = await apiRequest("/api/reservations");
			list.replaceChildren(...reservations.map(renderReservation));
			empty.hidden = reservations.length !== 0;
		} catch (error) {
			list.replaceChildren();
			empty.hidden = true;
			setFeedback(userMessage(error), true);
		}
	}

	function setRole(nextRole) {
		role = nextRole;
		document.querySelector("#my-reservations-section").hidden = role === "PUBLIC";
		document.querySelector("#my-reservations-section .muted").textContent = role === "PATRON"
			? "Theo dõi các yêu cầu đặt trước của bạn."
			: "Theo dõi hàng chờ và các bản đang được giữ cho bạn đọc.";
	}

	return { loadReservations, setRole };
}
