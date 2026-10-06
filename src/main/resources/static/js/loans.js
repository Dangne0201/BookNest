import { apiRequest, userMessage } from "./api.js";

export function initializeLoans(showToast, onInventoryChanged) {
	const loansList = document.querySelector("#loans-list");
	const loansTable = document.querySelector("#loans-table-wrap");
	const loansEmpty = document.querySelector("#loans-empty");
	const loansLoading = document.querySelector("#loans-loading");
	const loansFeedback = document.querySelector("#loans-feedback");
	const checkoutDialog = document.querySelector("#checkout-dialog");
	const checkoutForm = document.querySelector("#checkout-form");
	const checkoutMember = document.querySelector("#checkout-member");
	const checkoutCopy = document.querySelector("#checkout-copy");
	const checkoutFormError = document.querySelector("#checkout-form-error");
	const loansSearchForm = document.querySelector("#loans-search-form");
	const loansQuery = document.querySelector("#loans-query");
	const loansState = document.querySelector("#loans-state");
	const loansSort = document.querySelector("#loans-sort");
	const loansDirection = document.querySelector("#loans-direction");
	const loansPagination = document.querySelector("#loans-pagination");
	const loansPageStatus = document.querySelector("#loans-page-status");
	const activityList = document.querySelector("#activity-list");
	const activityLoading = document.querySelector("#activity-loading");
	const activityEmpty = document.querySelector("#activity-empty");
	const activityFeedback = document.querySelector("#activity-feedback");
	const activitySearchForm = document.querySelector("#activity-search-form");
	const activityQuery = document.querySelector("#activity-query");
	const activityType = document.querySelector("#activity-type");
	const activityPagination = document.querySelector("#activity-pagination");
	const activityPageStatus = document.querySelector("#activity-page-status");

	let loans = [];
	let loanPage = 0;
	let loanPageResult = null;
	let activities = [];
	let activityPage = 0;
	let activityPageResult = null;
	let role = "STAFF";

	function setFeedback(message, isError = false) {
		loansFeedback.textContent = message;
		loansFeedback.classList.toggle("error", isError);
		loansFeedback.classList.toggle("success", !isError);
		loansFeedback.hidden = !message;
	}

	function setActivityFeedback(message, isError = false) {
		activityFeedback.textContent = message;
		activityFeedback.classList.toggle("error", isError);
		activityFeedback.classList.toggle("success", !isError);
		activityFeedback.hidden = !message;
	}

	function formatDate(value) {
		if (!value) {
			return "—";
		}
		const [year, month, day] = value.split("-");
		return `${day}/${month}/${year}`;
	}

	function makeCell(text, className = "") {
		const cell = document.createElement("td");
		cell.textContent = text;
		if (className) {
			cell.className = className;
		}
		return cell;
	}

	function renderLoan(loan) {
		const row = document.createElement("tr");
		const memberCell = document.createElement("td");
		const member = document.createElement("span");
		member.className = "member-name";
		member.textContent = loan.memberName;
		memberCell.append(member);

		const bookCell = document.createElement("td");
		const book = document.createElement("span");
		book.className = "book-title";
		book.textContent = loan.bookTitle;
		const copy = document.createElement("span");
		copy.className = "book-author";
		copy.textContent = `Bản sách #${loan.copyId}`;
		bookCell.append(book, copy);

		const checkoutCell = makeCell(formatDate(loan.checkoutDate));
		const dueCell = makeCell(formatDate(loan.dueDate));
		if (loan.overdue) {
			dueCell.classList.add("loan-overdue-date");
		}

		const statusCell = document.createElement("td");
		const status = document.createElement("span");
		status.className = `loan-status ${loan.overdue ? "overdue" : loan.active ? "active" : "returned"}`;
		status.textContent = loan.overdue ? "Quá hạn" : loan.active ? "Đang mượn" : "Đã trả";
		statusCell.append(status);
		const actor = document.createElement("span");
		actor.className = "book-author loan-actor";
		actor.textContent = loan.active
			? `Lập phiếu: ${loan.checkedOutBy}`
			: `Nhận trả: ${loan.returnedBy || "—"}`;
		statusCell.append(actor);
		if (loan.renewed) {
			const renewalActor = document.createElement("span");
			renewalActor.className = "book-author loan-actor";
			renewalActor.textContent = `Gia hạn bởi: ${loan.renewedBy || "—"}`;
			statusCell.append(renewalActor);
		}

		const actionsCell = document.createElement("td");
		let hasAction = false;
		if (loan.active && role !== "PATRON") {
			const returnButton = document.createElement("button");
			returnButton.type = "button";
			returnButton.className = "button button-secondary button-small";
			returnButton.textContent = "Trả sách";
			returnButton.setAttribute("aria-label", `Trả sách ${loan.bookTitle} cho ${loan.memberName}`);
			returnButton.addEventListener("click", () => returnLoan(loan, returnButton));
			actionsCell.append(returnButton);
			hasAction = true;
		}
		if (loan.renewalEligible) {
			const renewButton = document.createElement("button");
			renewButton.type = "button";
			renewButton.className = "button button-secondary button-small";
			renewButton.textContent = "Gia hạn";
			renewButton.setAttribute("aria-label", `Gia hạn ${loan.bookTitle} cho ${loan.memberName}`);
			renewButton.addEventListener("click", () => renewLoan(loan, renewButton));
			actionsCell.append(renewButton);
			hasAction = true;
		}
		if (!hasAction) {
			actionsCell.textContent = "—";
			actionsCell.className = "muted";
		}

		row.append(memberCell, bookCell, checkoutCell, dueCell, statusCell, actionsCell);
		return row;
	}

	function renderLoans() {
		loansList.replaceChildren(...loans.map(renderLoan));
		loansLoading.hidden = true;
		loansTable.hidden = loans.length === 0;
		loansEmpty.hidden = loans.length !== 0;
		if (loans.length === 0) {
			const hasFilters = loansQuery.value || loansState.value;
			document.querySelector("#loans-empty h3").textContent = hasFilters
				? "Không tìm thấy lượt mượn phù hợp"
				: "Chưa có lượt mượn";
			document.querySelector("#loans-empty p").textContent = hasFilters
				? "Thử thay đổi từ khóa hoặc bộ lọc."
				: role === "PATRON"
					? "Các lượt mượn và hạn trả của bạn sẽ xuất hiện tại đây."
					: "Tạo thành viên, đầu sách và bản sách sẵn sàng, rồi bắt đầu cho mượn.";
		}
		loansPagination.hidden = !loanPageResult || loanPageResult.totalPages <= 1;
		if (loanPageResult) {
			loansPageStatus.textContent = `Trang ${loanPageResult.page + 1} / ${Math.max(loanPageResult.totalPages, 1)} · ${loanPageResult.totalElements} lượt mượn`;
			document.querySelector("#loans-previous").disabled = loanPageResult.first;
			document.querySelector("#loans-next").disabled = loanPageResult.last;
		}
	}

	async function loadLoans() {
		loansLoading.hidden = false;
		loansTable.hidden = true;
		loansEmpty.hidden = true;
		setFeedback("");
		try {
			const params = new URLSearchParams({
				q: loansQuery.value.trim(),
				state: loansState.value,
				page: String(loanPage),
				size: "20",
				sort: loansSort.value,
				direction: loansDirection.value
			});
			loanPageResult = await apiRequest(`/api/loans?${params}`);
			if (loanPageResult.totalPages > 0 && loanPage >= loanPageResult.totalPages) {
				loanPage = loanPageResult.totalPages - 1;
				return loadLoans();
			}
			loans = loanPageResult.items;
			renderLoans();
		} catch (error) {
			loansLoading.hidden = true;
			setFeedback(userMessage(error), true);
		}
	}

	function activityLabel(type) {
		return ({
			LOAN_CHECKED_OUT: "Đã mượn sách",
			LOAN_RETURNED: "Đã trả sách",
			LOAN_RENEWED: "Đã gia hạn lượt mượn",
			RESERVATION_PLACED: "Đã đặt trước",
			RESERVATION_HELD: "Đã giữ bản sách",
			RESERVATION_CANCELLED: "Đã hủy đặt trước",
			RESERVATION_FULFILLED: "Đã nhận sách đặt trước"
		})[type] || "Hoạt động thư viện";
	}

	function renderActivity(event) {
		const item = document.createElement("li");
		const heading = document.createElement("strong");
		heading.textContent = activityLabel(event.type);
		const details = document.createElement("p");
		const parts = [];
		if (event.bookTitle) {
			parts.push(event.bookTitle);
		}
		if (event.memberName) {
			parts.push(event.memberName);
		}
		if (event.copyId) {
			parts.push(`Bản sách #${event.copyId}`);
		}
		const description = document.createElement("span");
		description.textContent = parts.join(" · ") || "Thao tác thư viện";
		const actor = document.createElement("span");
		actor.className = "activity-actor";
		actor.textContent = ` · Thực hiện bởi ${event.actorUsername}`;
		const timestamp = document.createElement("time");
		timestamp.dateTime = event.occurredAt;
		timestamp.textContent = new Date(event.occurredAt).toLocaleString("vi-VN");
		details.append(description, actor);
		item.append(heading, details, timestamp);
		return item;
	}

	function renderActivities() {
		activityList.replaceChildren(...activities.map(renderActivity));
		activityLoading.hidden = true;
		activityEmpty.hidden = activities.length !== 0;
		activityPagination.hidden = !activityPageResult || activityPageResult.totalPages <= 1;
		if (activityPageResult) {
			activityPageStatus.textContent = `Trang ${activityPageResult.page + 1} / ${Math.max(activityPageResult.totalPages, 1)} · ${activityPageResult.totalElements} hoạt động`;
			document.querySelector("#activity-previous").disabled = activityPageResult.first;
			document.querySelector("#activity-next").disabled = activityPageResult.last;
		}
	}

	async function loadActivity() {
		activityLoading.hidden = false;
		activityEmpty.hidden = true;
		activityList.replaceChildren();
		setActivityFeedback("");
		try {
			const params = new URLSearchParams({
				q: activityQuery.value.trim(),
				type: activityType.value,
				page: String(activityPage),
				size: "20"
			});
			activityPageResult = await apiRequest(`/api/activity?${params}`);
			if (activityPageResult.totalPages > 0 && activityPage >= activityPageResult.totalPages) {
				activityPage = activityPageResult.totalPages - 1;
				return loadActivity();
			}
			activities = activityPageResult.items;
			renderActivities();
		} catch (error) {
			activityLoading.hidden = true;
			setActivityFeedback(userMessage(error), true);
		}
	}

	loansSearchForm.addEventListener("submit", event => {
		event.preventDefault();
		loanPage = 0;
		loadLoans();
	});
	document.querySelector("#loans-reset").addEventListener("click", () => {
		loansSearchForm.reset();
		loanPage = 0;
		loadLoans();
	});
	document.querySelector("#loans-previous").addEventListener("click", () => {
		loanPage = Math.max(0, loanPage - 1);
		loadLoans();
	});
	document.querySelector("#loans-next").addEventListener("click", () => {
		loanPage += 1;
		loadLoans();
	});
	activitySearchForm.addEventListener("submit", event => {
		event.preventDefault();
		activityPage = 0;
		loadActivity();
	});
	document.querySelector("#activity-reset").addEventListener("click", () => {
		activitySearchForm.reset();
		activityPage = 0;
		loadActivity();
	});
	document.querySelector("#activity-previous").addEventListener("click", () => {
		activityPage = Math.max(0, activityPage - 1);
		loadActivity();
	});
	document.querySelector("#activity-next").addEventListener("click", () => {
		activityPage += 1;
		loadActivity();
	});

	function appendPlaceholder(select, text) {
		const option = document.createElement("option");
		option.value = "";
		option.textContent = text;
		option.disabled = true;
		option.selected = true;
		select.append(option);
	}

	async function loadCheckoutOptions() {
		checkoutFormError.textContent = "";
		checkoutFormError.hidden = true;
		checkoutForm.reset();
		checkoutMember.replaceChildren();
		checkoutCopy.replaceChildren();
		checkoutMember.disabled = true;
		checkoutCopy.disabled = true;
		appendPlaceholder(checkoutMember, "Đang tải thành viên…");
		appendPlaceholder(checkoutCopy, "Đang tải bản sách…");
		checkoutDialog.showModal();

		try {
			const [members, books] = await Promise.all([
				apiRequest("/api/members"),
				apiRequest("/api/books/options")
			]);
			const copiesByBook = await Promise.all(books.map(async book => ({
				book,
				copies: await apiRequest(`/api/books/${book.id}/copies`)
			})));
			const availableCopies = copiesByBook.flatMap(({ book, copies }) =>
				copies.filter(copy => copy.status === "AVAILABLE").map(copy => ({
					id: copy.id,
					label: `${book.title} — Bản sách #${copy.id}`
				}))
			);

			checkoutMember.replaceChildren();
			appendPlaceholder(checkoutMember,
				members.length ? "Chọn thành viên…" : "Chưa có thành viên");
			members.forEach(member => {
				const option = document.createElement("option");
				option.value = String(member.id);
				option.textContent = member.fullName;
				checkoutMember.append(option);
			});
			checkoutCopy.replaceChildren();
			appendPlaceholder(checkoutCopy,
				availableCopies.length ? "Chọn bản sách…" : "Không có bản sách sẵn sàng");
			availableCopies.forEach(copy => {
				const option = document.createElement("option");
				option.value = String(copy.id);
				option.textContent = copy.label;
				checkoutCopy.append(option);
			});
			checkoutMember.disabled = members.length === 0;
			checkoutCopy.disabled = availableCopies.length === 0;
			const submitButton = checkoutForm.querySelector('[type="submit"]');
			submitButton.disabled = members.length === 0 || availableCopies.length === 0;
			if (members.length === 0 || availableCopies.length === 0) {
				checkoutFormError.textContent = members.length === 0
					? "Hãy thêm thành viên trước khi lập phiếu mượn."
					: "Hiện không có bản sách nào sẵn sàng để mượn.";
				checkoutFormError.hidden = false;
			}
			checkoutMember.focus();
		} catch (error) {
			checkoutFormError.textContent = userMessage(error);
			checkoutFormError.hidden = false;
			checkoutForm.querySelector('[type="submit"]').disabled = true;
		}
	}

	async function returnLoan(loan, button) {
		if (!window.confirm(`Xác nhận trả “${loan.bookTitle}” của ${loan.memberName}?`)) {
			return;
		}
		button.disabled = true;
		try {
			await apiRequest(`/api/loans/${loan.id}/return`, { method: "POST" });
			await Promise.all([loadLoans(), loadActivity(), onInventoryChanged()]);
			showToast("Đã ghi nhận trả sách.");
		} catch (error) {
			showToast(userMessage(error), true);
			await loadLoans();
		} finally {
			button.disabled = false;
		}
	}

	async function renewLoan(loan, button) {
		if (!window.confirm(`Gia hạn “${loan.bookTitle}” thêm 14 ngày từ hạn hiện tại?`)) {
			return;
		}
		button.disabled = true;
		try {
			const renewedLoan = await apiRequest(`/api/loans/${loan.id}/renew`, { method: "POST" });
			await Promise.all([loadLoans(), loadActivity()]);
			showToast(`Đã gia hạn. Hạn trả mới: ${formatDate(renewedLoan.dueDate)}.`);
		} catch (error) {
			showToast(userMessage(error), true);
			await Promise.all([loadLoans(), loadActivity()]);
		} finally {
			button.disabled = false;
		}
	}

	document.querySelector("#checkout-button").addEventListener("click", loadCheckoutOptions);
	document.querySelector('[data-action="checkout"]').addEventListener("click", loadCheckoutOptions);
	document.querySelectorAll('[data-close="checkout-dialog"]').forEach(button => {
		button.addEventListener("click", () => checkoutDialog.close());
	});

	checkoutForm.addEventListener("submit", async event => {
		event.preventDefault();
		checkoutFormError.textContent = "";
		checkoutFormError.hidden = true;
		if (!checkoutForm.reportValidity()) {
			return;
		}
		const submitButton = checkoutForm.querySelector('[type="submit"]');
		submitButton.disabled = true;
		try {
			await apiRequest("/api/loans", {
				method: "POST",
				body: JSON.stringify({
					memberId: Number(checkoutMember.value),
					copyId: Number(checkoutCopy.value)
				})
			});
			checkoutDialog.close();
			await Promise.all([loadLoans(), loadActivity(), onInventoryChanged()]);
			showToast("Đã lập phiếu mượn. Hạn trả sau 14 ngày.");
		} catch (error) {
			checkoutFormError.textContent = userMessage(error);
			checkoutFormError.hidden = false;
		} finally {
			submitButton.disabled = false;
		}
	});

	function setRole(nextRole) {
		role = nextRole;
		const canManage = role === "STAFF" || role === "ADMIN";
		document.querySelector("#checkout-button").hidden = !canManage;
		document.querySelector('[data-action="checkout"]').hidden = !canManage;
	}

	return { loadLoans, loadActivity, setRole };
}
