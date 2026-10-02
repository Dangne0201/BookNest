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

	let loans = [];
	let role = "STAFF";

	function setFeedback(message, isError = false) {
		loansFeedback.textContent = message;
		loansFeedback.classList.toggle("error", isError);
		loansFeedback.classList.toggle("success", !isError);
		loansFeedback.hidden = !message;
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

		const actionsCell = document.createElement("td");
		if (loan.active && role !== "PATRON") {
			const returnButton = document.createElement("button");
			returnButton.type = "button";
			returnButton.className = "button button-secondary button-small";
			returnButton.textContent = "Trả sách";
			returnButton.setAttribute("aria-label", `Trả sách ${loan.bookTitle} cho ${loan.memberName}`);
			returnButton.addEventListener("click", () => returnLoan(loan, returnButton));
			actionsCell.append(returnButton);
		} else {
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
	}

	async function loadLoans() {
		loansLoading.hidden = false;
		loansTable.hidden = true;
		loansEmpty.hidden = true;
		setFeedback("");
		try {
			loans = await apiRequest("/api/loans");
			renderLoans();
		} catch (error) {
			loansLoading.hidden = true;
			setFeedback(userMessage(error), true);
		}
	}

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
				apiRequest("/api/books")
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
			await Promise.all([loadLoans(), onInventoryChanged()]);
			showToast("Đã ghi nhận trả sách.");
		} catch (error) {
			showToast(userMessage(error), true);
			await loadLoans();
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
			await Promise.all([loadLoans(), onInventoryChanged()]);
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
		document.querySelector("#loans-empty p").textContent = role === "PATRON"
			? "Các lượt mượn và hạn trả của bạn sẽ xuất hiện tại đây."
			: "Tạo thành viên, đầu sách và bản sách sẵn sàng, rồi bắt đầu cho mượn.";
	}

	return { loadLoans, setRole };
}
