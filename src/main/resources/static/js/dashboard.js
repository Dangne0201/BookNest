import { apiRequest, userMessage } from "./api.js";

export function initializeDashboard() {
	const section = document.querySelector("#dashboard-section");
	const loading = document.querySelector("#dashboard-loading");
	const feedback = document.querySelector("#dashboard-feedback");
	const cards = document.querySelector("#dashboard-cards");
	let role = "PUBLIC";

	function setRole(nextRole) {
		role = nextRole;
		section.hidden = role !== "STAFF" && role !== "ADMIN";
	}

	async function loadSummary() {
		if (role !== "STAFF" && role !== "ADMIN") {
			return;
		}
		loading.hidden = false;
		cards.hidden = true;
		feedback.hidden = true;
		section.setAttribute("aria-busy", "true");
		try {
			const summary = await apiRequest("/api/dashboard");
			document.querySelector("#dashboard-available").textContent = String(summary.availableCopies);
			document.querySelector("#dashboard-active-loans").textContent = String(summary.activeLoans);
			document.querySelector("#dashboard-overdue-loans").textContent = String(summary.overdueLoans);
			document.querySelector("#dashboard-reservations").textContent = String(summary.activeReservations);
			cards.hidden = false;
		} catch (error) {
			feedback.textContent = userMessage(error);
			feedback.classList.add("error");
			feedback.hidden = false;
		} finally {
			loading.hidden = true;
			section.removeAttribute("aria-busy");
		}
	}

	return { loadSummary, setRole };
}
