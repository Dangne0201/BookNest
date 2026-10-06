import { apiRequest, userMessage } from "./api.js";

const STATUS_LABELS = {
	AVAILABLE: "Sẵn sàng",
	ON_LOAN: "Đang được mượn",
	MAINTENANCE: "Đang bảo trì",
	RETIRED: "Ngừng sử dụng",
	ON_HOLD: "Đang được giữ"
};

export function initializeBooks(showToast, onLoansChanged = () => Promise.resolve()) {
	const bookDialog = document.querySelector("#book-dialog");
	const bookForm = document.querySelector("#book-form");
	const bookDialogTitle = document.querySelector("#book-dialog-title");
	const bookFormError = document.querySelector("#book-form-error");
	const copiesDialog = document.querySelector("#copies-dialog");
	const copiesDialogTitle = document.querySelector("#copies-dialog-title");
	const bookDetails = document.querySelector("#book-details");
	const copiesSummary = document.querySelector("#copies-summary");
	const copiesList = document.querySelector("#copies-list");
	const copiesFeedback = document.querySelector("#copies-feedback");
	const copiesLoading = document.querySelector("#copies-loading");
	const copiesEmpty = document.querySelector("#copies-empty");
	const addCopyForm = document.querySelector("#copy-add-form");
	const booksList = document.querySelector("#books-list");
	const booksTable = document.querySelector("#books-table-wrap");
	const booksEmpty = document.querySelector("#books-empty");
	const booksLoading = document.querySelector("#books-loading");
	const booksFeedback = document.querySelector("#books-feedback");
	const booksSearchForm = document.querySelector("#books-search-form");
	const booksQuery = document.querySelector("#books-query");
	const booksGenre = document.querySelector("#books-genre");
	const booksAvailability = document.querySelector("#books-availability");
	const booksSort = document.querySelector("#books-sort");
	const booksDirection = document.querySelector("#books-direction");
	const booksPagination = document.querySelector("#books-pagination");
	const booksPageStatus = document.querySelector("#books-page-status");

	let books = [];
	let bookPage = 0;
	let bookPageResult = null;
	let selectedBookId = null;
	let role = "PUBLIC";

	function canManage() {
		return role === "STAFF" || role === "ADMIN";
	}

	function setFeedback(element, message, isError = false) {
		element.textContent = message;
		element.classList.toggle("error", isError);
		element.classList.toggle("success", !isError);
		element.hidden = !message;
	}

	function setButtonBusy(button, busy) {
		button.disabled = busy;
	}

	function makeCell(className, text) {
		const cell = document.createElement("td");
		if (className) {
			cell.className = className;
		}
		cell.textContent = text;
		return cell;
	}

	function makeActionButton(label, action, bookId, className = "button button-quiet button-small") {
		const button = document.createElement("button");
		button.type = "button";
		button.className = className;
		button.textContent = label;
		button.dataset.action = action;
		button.dataset.bookId = String(bookId);
		return button;
	}

	function renderBook(book) {
		const row = document.createElement("tr");
		const titleCell = document.createElement("td");
		const title = document.createElement("span");
		title.className = "book-title";
		title.textContent = book.title;
		titleCell.append(title);

		const author = document.createElement("span");
		author.className = "book-author";
		author.textContent = book.author;
		titleCell.append(author);
		if (book.isbn) {
			const isbn = document.createElement("span");
			isbn.className = "book-isbn";
			isbn.textContent = `ISBN ${book.isbn}`;
			titleCell.append(isbn);
		}
		row.append(titleCell, makeCell("", book.genre || "—"));

		const totalCell = document.createElement("td");
		const totalPill = document.createElement("span");
		totalPill.className = "count-pill";
		totalPill.textContent = String(book.totalCopies);
		totalCell.append(totalPill);
		row.append(totalCell);

		const availableCell = document.createElement("td");
		const availablePill = document.createElement("span");
		availablePill.className = "count-pill available";
		availablePill.textContent = String(book.availableCopies);
		availableCell.append(availablePill);
		row.append(availableCell);

		const actionsCell = document.createElement("td");
		const actions = document.createElement("div");
		actions.className = "row-actions";
		actions.append(makeActionButton("Chi tiết", "copies", book.id));
		if (canManage()) {
			actions.append(
				makeActionButton("Sửa", "edit", book.id),
				makeActionButton("Xóa", "delete", book.id, "button button-danger button-small")
			);
		} else if (role === "PATRON" && book.availableCopies === 0) {
			actions.append(makeActionButton("Đặt trước", "reserve", book.id, "button button-primary button-small"));
		}
		actionsCell.append(actions);
		row.append(actionsCell);
		return row;
	}

	function updateSummary() {
		document.querySelector("#book-total").textContent = String(bookPageResult?.totalElements || 0);
		document.querySelector("#copy-total").textContent =
			String(books.reduce((sum, book) => sum + book.totalCopies, 0));
		document.querySelector("#copy-available").textContent =
			String(books.reduce((sum, book) => sum + book.availableCopies, 0));
	}

	function renderBooks() {
		booksList.replaceChildren(...books.map(renderBook));
		updateSummary();
		booksLoading.hidden = true;
		booksTable.hidden = books.length === 0;
		booksEmpty.hidden = books.length !== 0;
		if (books.length === 0) {
			const hasFilters = booksQuery.value || booksGenre.value || booksAvailability.value;
			document.querySelector("#books-empty h3").textContent = hasFilters
				? "Không tìm thấy đầu sách phù hợp"
				: "Thư viện chưa có đầu sách";
			document.querySelector("#books-empty p").textContent = hasFilters
				? "Thử thay đổi từ khóa hoặc bộ lọc."
				: "Thêm đầu sách đầu tiên để bắt đầu quản lý các bản sách.";
		}
		booksPagination.hidden = !bookPageResult || bookPageResult.totalPages <= 1;
		if (bookPageResult) {
			booksPageStatus.textContent = `Trang ${bookPageResult.page + 1} / ${Math.max(bookPageResult.totalPages, 1)} · ${bookPageResult.totalElements} đầu sách`;
			document.querySelector("#books-previous").disabled = bookPageResult.first;
			document.querySelector("#books-next").disabled = bookPageResult.last;
		}
	}

	async function loadBooks() {
		booksLoading.hidden = false;
		booksTable.hidden = true;
		booksEmpty.hidden = true;
		setFeedback(booksFeedback, "");
		try {
			const params = new URLSearchParams({
				q: booksQuery.value.trim(),
				genre: booksGenre.value.trim(),
				availability: booksAvailability.value,
				page: String(bookPage),
				size: "20",
				sort: booksSort.value,
				direction: booksDirection.value
			});
			bookPageResult = await apiRequest(`/api/books?${params}`);
			if (bookPageResult.totalPages > 0 && bookPage >= bookPageResult.totalPages) {
				bookPage = bookPageResult.totalPages - 1;
				return loadBooks();
			}
			books = bookPageResult.items;
			renderBooks();
		} catch (error) {
			booksLoading.hidden = true;
			setFeedback(booksFeedback, userMessage(error), true);
		}

		booksSearchForm.addEventListener("submit", event => {
			event.preventDefault();
			bookPage = 0;
			loadBooks();
		});
		document.querySelector("#books-reset").addEventListener("click", () => {
			booksSearchForm.reset();
			bookPage = 0;
			loadBooks();
		});
		document.querySelector("#books-previous").addEventListener("click", () => {
			bookPage = Math.max(0, bookPage - 1);
			loadBooks();
		});
		document.querySelector("#books-next").addEventListener("click", () => {
			bookPage += 1;
			loadBooks();
		});
	}

	function setRole(nextRole) {
		role = nextRole;
		document.querySelector("#add-book-button").hidden = !canManage();
		document.querySelector('[data-action="add-book"]').hidden = !canManage();
		addCopyForm.hidden = !canManage();
		renderBooks();
	}

	async function borrowCopy(copyId, button) {
		button.disabled = true;
		try {
			await apiRequest("/api/loans", {
				method: "POST",
				body: JSON.stringify({ copyId })
			});
			copiesDialog.close();
			showToast("Đã mượn sách. Hạn trả sau 14 ngày.");
			await Promise.all([loadBooks(), onLoansChanged()]);
		} catch (error) {
			setFeedback(copiesFeedback, userMessage(error), true);
			button.disabled = false;
		}
	}

	async function reserveBook(book, button) {
		button.disabled = true;
		try {
			await apiRequest("/api/reservations", {
				method: "POST",
				body: JSON.stringify({ bookId: book.id })
			});
			showToast("Đã vào hàng chờ đặt trước.");
			await onLoansChanged();
		} catch (error) {
			showToast(userMessage(error), true);
			button.disabled = false;
		}
	}

	function showBookForm(book = null) {
		bookForm.reset();
		bookFormError.textContent = "";
		bookFormError.hidden = true;
		bookForm.elements.id.value = book ? String(book.id) : "";
		bookForm.elements.title.value = book?.title || "";
		bookForm.elements.author.value = book?.author || "";
		bookForm.elements.isbn.value = book?.isbn || "";
		bookForm.elements.genre.value = book?.genre || "";
		bookForm.elements.publicationYear.value = book?.publicationYear ?? "";
		bookForm.elements.publicationYear.max = String(new Date().getFullYear() + 1);
		bookForm.elements.description.value = book?.description || "";
		bookDialogTitle.textContent = book ? "Chỉnh sửa đầu sách" : "Thêm đầu sách";
		bookDialog.showModal();
		bookForm.elements.title.focus();
	}

	function appendCopyRow(copy) {
		const row = document.createElement("li");
		row.className = "copy-row";
		const identifier = document.createElement("span");
		identifier.className = "copy-id";
		identifier.textContent = `Bản sách #${copy.id}`;

		const select = document.createElement("select");
		select.className = "copy-status";
		select.setAttribute("aria-label", `Trạng thái bản sách ${copy.id}`);
		Object.entries(STATUS_LABELS).forEach(([value, label]) => {
			const option = document.createElement("option");
			option.value = value;
			option.textContent = label;
			select.append(option);
		});
		select.value = copy.status;
		select.disabled = copy.status === "ON_LOAN" || copy.status === "ON_HOLD";

		const actions = document.createElement("div");
		actions.className = "copy-actions";
		const save = document.createElement("button");
		save.type = "button";
		save.className = "button button-secondary button-small";
		save.textContent = "Lưu trạng thái";
		save.disabled = copy.status === "ON_LOAN";
		save.addEventListener("click", async () => {
			setButtonBusy(save, true);
			try {
				await apiRequest(`/api/books/${selectedBookId}/copies/${copy.id}`, {
					method: "PUT",
					body: JSON.stringify({ status: select.value })
				});
				setFeedback(copiesFeedback, "Đã cập nhật trạng thái bản sách.");
				await loadCopies();
				await loadBooks();
			} catch (error) {
				setFeedback(copiesFeedback, userMessage(error), true);
			} finally {
				setButtonBusy(save, false);
			}
		});

		const remove = document.createElement("button");
		remove.type = "button";
		remove.className = "button button-danger button-small";
		remove.textContent = "Xóa";
		remove.setAttribute("aria-label", `Xóa bản sách ${copy.id}`);
		remove.addEventListener("click", async () => {
			if (!window.confirm(`Xóa bản sách #${copy.id}? Thao tác này không thể hoàn tác.`)) {
				return;
			}
			setButtonBusy(remove, true);
			try {
				await apiRequest(`/api/books/${selectedBookId}/copies/${copy.id}`, { method: "DELETE" });
				setFeedback(copiesFeedback, "Đã xóa bản sách.");
				await loadCopies();
				await loadBooks();
			} catch (error) {
				setFeedback(copiesFeedback, userMessage(error), true);
			} finally {
				setButtonBusy(remove, false);
			}
		});

		if (canManage()) {
			actions.append(save, remove);
			row.append(identifier, select, actions);
		} else {
			row.append(identifier);
			if (role === "PATRON" && copy.status === "AVAILABLE") {
				const borrow = document.createElement("button");
				borrow.type = "button";
				borrow.className = "button button-primary button-small";
				borrow.textContent = "Mượn bản này";
				borrow.addEventListener("click", () => borrowCopy(copy.id, borrow));
				row.append(borrow);
			} else {
				const status = document.createElement("span");
				status.className = "copy-status-label";
				status.textContent = STATUS_LABELS[copy.status];
				row.append(status);
			}
		}
		return row;
	}

	async function loadCopies() {
		if (!selectedBookId) {
			return;
		}
		copiesLoading.hidden = false;
		copiesEmpty.hidden = true;
		copiesList.replaceChildren();
		try {
			const copies = await apiRequest(`/api/books/${selectedBookId}/copies`);
			copiesList.replaceChildren(...copies.map(appendCopyRow));
			copiesEmpty.hidden = copies.length !== 0;
			copiesSummary.textContent = `${copies.length} bản sách trong thư viện`;
		} catch (error) {
			setFeedback(copiesFeedback, userMessage(error), true);
		} finally {
			copiesLoading.hidden = true;
		}
	}

	async function showCopies(book) {
		selectedBookId = book.id;
		copiesDialogTitle.textContent = book.title;
		renderBookDetails(book);
		copiesSummary.textContent = "";
		copiesList.replaceChildren();
		copiesEmpty.hidden = true;
		setFeedback(copiesFeedback, "");
		copiesDialog.showModal();
		await loadCopies();
	}

	function renderBookDetails(book) {
		const details = [
			["Tác giả", book.author],
			["ISBN", book.isbn],
			["Thể loại", book.genre],
			["Năm xuất bản", book.publicationYear]
		];
		bookDetails.replaceChildren();
		details.forEach(([label, value]) => {
			const term = document.createElement("dt");
			term.textContent = label;
			const description = document.createElement("dd");
			description.textContent = value || "Chưa có thông tin";
			bookDetails.append(term, description);
		});
		if (book.description) {
			const term = document.createElement("dt");
			term.textContent = "Mô tả";
			const description = document.createElement("dd");
			description.className = "book-description";
			description.textContent = book.description;
			bookDetails.append(term, description);
		}
	}

	document.querySelector("#add-book-button").addEventListener("click", () => showBookForm());
	document.querySelector('[data-action="add-book"]').addEventListener("click", () => showBookForm());
	document.querySelectorAll('[data-close="book-dialog"], [data-close="copies-dialog"]').forEach(button => {
		button.addEventListener("click", () => document.querySelector(`#${button.dataset.close}`).close());
	});

	booksList.addEventListener("click", async event => {
		const button = event.target.closest("button[data-action]");
		if (!button) {
			return;
		}
		const book = books.find(item => item.id === Number(button.dataset.bookId));
		if (!book) {
			return;
		}
		if (button.dataset.action === "copies") {
			await showCopies(book);
		} else if (button.dataset.action === "reserve") {
			await reserveBook(book, button);
		} else if (button.dataset.action === "edit") {
			showBookForm(book);
		} else if (button.dataset.action === "delete") {
			if (book.totalCopies > 0) {
				showToast("Xóa các bản sách thuộc đầu sách này trước.", true);
				return;
			}
			if (!window.confirm(`Xóa đầu sách “${book.title}”? Thao tác này không thể hoàn tác.`)) {
				return;
			}
			setButtonBusy(button, true);
			try {
				await apiRequest(`/api/books/${book.id}`, { method: "DELETE" });
				showToast("Đã xóa đầu sách.");
				await loadBooks();
			} catch (error) {
				showToast(userMessage(error), true);
			} finally {
				setButtonBusy(button, false);
			}
		}
	});

	bookForm.addEventListener("submit", async event => {
		event.preventDefault();
		bookFormError.textContent = "";
		bookFormError.hidden = true;
		if (!bookForm.reportValidity()) {
			return;
		}
		const submitButton = bookForm.querySelector('[type="submit"]');
		setButtonBusy(submitButton, true);
		const form = new FormData(bookForm);
		const id = form.get("id");
		const year = form.get("publicationYear");
		const body = JSON.stringify({
			title: form.get("title"),
			author: form.get("author"),
			isbn: form.get("isbn") || null,
			genre: form.get("genre") || null,
			publicationYear: year ? Number(year) : null,
			description: form.get("description") || null
		});
		try {
			await apiRequest(id ? `/api/books/${id}` : "/api/books", {
				method: id ? "PUT" : "POST",
				body
			});
			bookDialog.close();
			showToast(id ? "Đã cập nhật đầu sách." : "Đã thêm đầu sách.");
			await loadBooks();
			if (selectedBookId && copiesDialog.open) {
				const updatedBook = books.find(book => book.id === selectedBookId);
				if (updatedBook) {
					copiesDialogTitle.textContent = updatedBook.title;
				}
			}
		} catch (error) {
			bookFormError.textContent = userMessage(error);
			bookFormError.hidden = false;
		} finally {
			setButtonBusy(submitButton, false);
		}
	});

	addCopyForm.addEventListener("submit", async event => {
		event.preventDefault();
		const submitButton = addCopyForm.querySelector('[type="submit"]');
		setButtonBusy(submitButton, true);
		try {
			await apiRequest(`/api/books/${selectedBookId}/copies`, {
				method: "POST",
				body: JSON.stringify({ status: addCopyForm.elements.status.value })
			});
			setFeedback(copiesFeedback, "Đã thêm bản sách.");
			await loadCopies();
			await loadBooks();
		} catch (error) {
			setFeedback(copiesFeedback, userMessage(error), true);
		} finally {
			setButtonBusy(submitButton, false);
		}
	});

	setRole("PUBLIC");
	return { loadBooks, setRole };
}
