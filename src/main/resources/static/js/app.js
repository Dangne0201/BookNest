import { initializeAuth } from "./auth.js";
import { initializeBooks } from "./books.js";
import { initializeMembers } from "./members.js";
import { initializeLoans } from "./loans.js";
import { initializeReservations } from "./reservations.js";
import { initializeAdmin } from "./admin.js";
import { initializeProfile } from "./profile.js";
import { initializeDashboard } from "./dashboard.js";

const toast = document.querySelector("#app-feedback");
let toastTimer;

function showToast(message, isError = false) {
	window.clearTimeout(toastTimer);
	toast.textContent = message;
	toast.classList.toggle("error", isError);
	toast.hidden = !message;
	if (message) {
		toastTimer = window.setTimeout(() => {
			toast.hidden = true;
		}, 4500);
	}
}

document.querySelectorAll("dialog:not(#temporary-password-dialog)").forEach(dialog => {
	dialog.addEventListener("keydown", event => {
		if (event.key === "Escape" && !event.defaultPrevented) {
			event.preventDefault();
			dialog.close();
		}
	});
});

let loans;
let reservations;
const dashboard = initializeDashboard();
const books = initializeBooks(showToast, async () => {
	await Promise.all([
		loans.loadLoans(),
		reservations.loadReservations(),
		loans.loadActivity(),
		dashboard.loadSummary()
	]);
}, () => dashboard.loadSummary());
const members = initializeMembers(showToast);
loans = initializeLoans(
	showToast,
	() => Promise.all([books.loadBooks(), dashboard.loadSummary()]),
	() => dashboard.loadSummary()
);
reservations = initializeReservations(
	showToast,
	() => books.loadBooks(),
	() => loans.loadLoans(),
	() => loans.loadActivity(),
	() => dashboard.loadSummary()
);
const admin = initializeAdmin(showToast);
const profile = initializeProfile(showToast);
const auth = initializeAuth({
	onAuthenticated: async account => {
		applyRole(account.role);
		const jobs = [books.loadBooks(), loans.loadLoans(), loans.loadActivity()];
		if (account.role === "PATRON") {
			jobs.push(reservations.loadReservations());
		} else {
			jobs.push(members.loadMembers());
			jobs.push(reservations.loadReservations());
			if (account.role === "ADMIN") {
				jobs.push(admin.loadAccounts());
			}
		}
		await Promise.all(jobs);
	},
	onLoggedOut: () => {
		dashboard.setRole("PUBLIC");
		books.setRole("PUBLIC");
		loans.setRole("PUBLIC");
		reservations.setRole("PUBLIC");
		showToast("");
	},
	onPublicView: async () => {
		applyRole("PUBLIC");
		await books.loadBooks();
	},
	showToast
});

let sessionRefresh = null;
window.addEventListener("booknest-session-expired", () => {
	if (!sessionRefresh) {
		sessionRefresh = auth.restoreSession().finally(() => {
			sessionRefresh = null;
		});
	}
});

const booksTab = document.querySelector("#books-tab");
const membersTab = document.querySelector("#members-tab");
const loansTab = document.querySelector("#loans-tab");
const adminTab = document.querySelector("#admin-tab");
const booksPanel = document.querySelector("#books-panel");
const membersPanel = document.querySelector("#members-panel");
const loansPanel = document.querySelector("#loans-panel");
const adminPanel = document.querySelector("#admin-panel");
const workspaceTabs = document.querySelector(".workspace-tabs");

function selectWorkspaceTab(activeTab) {
	const booksActive = activeTab === booksTab;
	const membersActive = activeTab === membersTab;
	const loansActive = activeTab === loansTab;
	const adminActive = activeTab === adminTab;
	booksTab.classList.toggle("is-active", booksActive);
	membersTab.classList.toggle("is-active", membersActive);
	loansTab.classList.toggle("is-active", loansActive);
	adminTab.classList.toggle("is-active", adminActive);
	booksTab.setAttribute("aria-selected", String(booksActive));
	membersTab.setAttribute("aria-selected", String(membersActive));
	loansTab.setAttribute("aria-selected", String(loansActive));
	adminTab.setAttribute("aria-selected", String(adminActive));
	booksPanel.hidden = !booksActive;
	membersPanel.hidden = !membersActive;
	loansPanel.hidden = !loansActive;
	adminPanel.hidden = !adminActive;
	if (membersActive) {
		members.loadMembers();
	} else if (loansActive) {
		loans.loadLoans();
		loans.loadActivity();
		reservations.loadReservations();
	} else if (adminActive) {
		admin.loadAccounts();
	} else if (booksActive) {
		dashboard.loadSummary();
	}
}

function applyRole(role) {
	const patron = role === "PATRON";
	workspaceTabs.hidden = role === "PUBLIC";
	membersTab.hidden = patron || role === "PUBLIC";
	adminTab.hidden = role !== "ADMIN";
	dashboard.setRole(role);
	booksTab.textContent = role === "STAFF" || role === "ADMIN" ? "Kho sách" : "Danh mục";
	loansTab.textContent = patron ? "Hoạt động của tôi" : "Mượn / Trả";
	document.querySelector("#loans-panel h1").textContent = patron ? "Lượt mượn của tôi" : "Mượn / Trả";
	books.setRole(role);
	loans.setRole(role);
	reservations.setRole(role);
	profile.setRole(role);
	selectWorkspaceTab(booksTab);
}

[booksTab, membersTab, loansTab, adminTab].forEach(tab => {
	tab.addEventListener("click", () => selectWorkspaceTab(tab));
	tab.addEventListener("keydown", event => {
		if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
			event.preventDefault();
			const direction = event.key === "ArrowRight" ? 1 : -1;
			const visibleTabs = [booksTab, membersTab, loansTab, adminTab].filter(item => !item.hidden);
			const index = visibleTabs.indexOf(tab);
			const nextTab = visibleTabs[(index + direction + visibleTabs.length) % visibleTabs.length];
			selectWorkspaceTab(nextTab);
			nextTab.focus();
		}
	});
});

applyRole("PUBLIC");
auth.restoreSession();
