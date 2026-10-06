import { apiRequest, userMessage } from "./api.js";

const STATUS_LABELS = {
	AVAILABLE: "Available",
	ON_LOAN: "On loan",
	MAINTENANCE: "Under maintenance",
	RETIRED: "Retired",
	ON_HOLD: "On hold"
};

export function initializeBooks(
	showToast,
	onLoansChanged = () => Promise.resolve(),
	onDashboardChanged = () => Promise.resolve()
) {
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
	const booksCards = document.querySelector("#books-cards");
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
		actions.append(makeActionButton("Details", "copies", book.id));
		if (canManage()) {
			actions.append(
				makeActionButton("Edit", "edit", book.id),
				makeActionButton("Delete", "delete", book.id, "button button-danger button-small")
			);
		} else if (role === "PATRON" && book.availableCopies === 0) {
			actions.append(makeActionButton("Reserve", "reserve", book.id, "button button-primary button-small"));
		}
		actionsCell.append(actions);
		row.append(actionsCell);
		return row;
	}

	function renderBookCard(book) {
		const card = document.createElement("article");
		card.className = "book-card";
		card.setAttribute("role", "listitem");
		card.setAttribute("aria-labelledby", `book-card-title-${book.id}`);

		const cover = document.createElement("div");
		cover.className = `book-cover ${getCoverPalette(book.genre)}`;
		cover.setAttribute("aria-hidden", "true");
		const coverKicker = document.createElement("span");
		coverKicker.className = "book-cover-kicker";
		coverKicker.textContent = "BOOKNEST · SHARED LIBRARY";
		const coverMark = document.createElement("span");
		coverMark.className = "book-cover-mark";
		coverMark.textContent = Array.from(book.title.trim())[0]?.toLocaleUpperCase() || "B";
		cover.append(coverKicker, coverMark);

		const content = document.createElement("div");
		content.className = "book-card-content";
		const genre = document.createElement("span");
		genre.className = "book-genre";
		genre.textContent = book.genre || "Uncategorized";

		const title = document.createElement("h3");
		title.className = "book-card-title";
		title.id = `book-card-title-${book.id}`;
		title.textContent = book.title;

		const author = document.createElement("p");
		author.className = "book-card-author";
		author.textContent = book.author;

		const metadata = document.createElement("p");
		metadata.className = "book-card-metadata";
		metadata.textContent = book.publicationYear
			? `Published ${book.publicationYear}`
			: "Publication year unavailable";

		const description = document.createElement("p");
		description.className = "book-card-description";
		description.textContent = book.description || "No description is available for this title.";

		const availability = document.createElement("p");
		availability.className = `book-card-availability${book.availableCopies > 0 ? " is-available" : ""}`;
		availability.textContent = book.availableCopies > 0
			? `${book.availableCopies} ${book.availableCopies === 1 ? "copy" : "copies"} available`
			: "No copies currently available";

		const actions = document.createElement("div");
		actions.className = "book-card-actions";
		actions.append(makeActionButton(
			role === "PATRON" && book.availableCopies > 0 ? "View copies to borrow" : "View details",
			"copies",
			book.id,
			"button button-primary button-small"
		));
		if (role === "PATRON" && book.availableCopies === 0) {
			actions.append(makeActionButton(
				"Reserve",
				"reserve",
				book.id,
				"button button-secondary button-small"
			));
		}

		content.append(genre, title, author, metadata, description, availability, actions);
		card.append(cover, content);
		return card;
	}

	function getCoverPalette(genre) {
		const paletteCount = 5;
		let hash = 0;
		for (const character of genre || "") {
			hash = (hash * 31 + character.codePointAt(0)) >>> 0;
		}
		return `book-cover-palette-${hash % paletteCount}`;
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
		booksCards.replaceChildren(...books.map(renderBookCard));
		updateSummary();
		booksLoading.hidden = true;
		booksTable.hidden = books.length === 0 || !canManage();
		booksCards.hidden = books.length === 0 || canManage();
		booksEmpty.hidden = books.length !== 0;
		if (books.length === 0) {
			const hasFilters = booksQuery.value || booksGenre.value || booksAvailability.value;
			document.querySelector("#books-empty h3").textContent = hasFilters
				? "No matching books found"
				: "No books in the library yet";
			document.querySelector("#books-empty p").textContent = hasFilters
				? "Try changing your search or filters."
				: "Add the first title to start managing copies.";
		}
		booksPagination.hidden = !bookPageResult || bookPageResult.totalPages <= 1;
		if (bookPageResult) {
			booksPageStatus.textContent = `Page ${bookPageResult.page + 1} of ${Math.max(bookPageResult.totalPages, 1)} · ${bookPageResult.totalElements} titles`;
			document.querySelector("#books-previous").disabled = bookPageResult.first;
			document.querySelector("#books-next").disabled = bookPageResult.last;
		}
	}

	async function loadBooks() {
		booksLoading.hidden = false;
		booksTable.hidden = true;
		booksCards.hidden = true;
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
			showToast("Book checked out. It is due in 14 days.");
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
			showToast("You have joined the reservation queue.");
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
		bookDialogTitle.textContent = book ? "Edit book" : "Add a book";
		bookDialog.showModal();
		bookForm.elements.title.focus();
	}

	function appendCopyRow(copy) {
		const row = document.createElement("li");
		row.className = "copy-row";
		const identifier = document.createElement("span");
		identifier.className = "copy-id";
		identifier.textContent = `Copy #${copy.id}`;

		const select = document.createElement("select");
		select.className = "copy-status";
		select.setAttribute("aria-label", `Status for copy ${copy.id}`);
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
		save.textContent = "Save status";
		save.disabled = copy.status === "ON_LOAN";
		save.addEventListener("click", async () => {
			setButtonBusy(save, true);
			try {
				await apiRequest(`/api/books/${selectedBookId}/copies/${copy.id}`, {
					method: "PUT",
					body: JSON.stringify({ status: select.value })
				});
				setFeedback(copiesFeedback, "Copy status updated.");
				await loadCopies();
				await loadBooks();
				await onDashboardChanged();
			} catch (error) {
				setFeedback(copiesFeedback, userMessage(error), true);
			} finally {
				setButtonBusy(save, false);
			}
		});

		const remove = document.createElement("button");
		remove.type = "button";
		remove.className = "button button-danger button-small";
		remove.textContent = "Delete";
		remove.setAttribute("aria-label", `Delete copy ${copy.id}`);
		remove.addEventListener("click", async () => {
			if (!window.confirm(`Delete copy #${copy.id}? This cannot be undone.`)) {
				return;
			}
			setButtonBusy(remove, true);
			try {
				await apiRequest(`/api/books/${selectedBookId}/copies/${copy.id}`, { method: "DELETE" });
				setFeedback(copiesFeedback, "Copy deleted.");
				await loadCopies();
				await loadBooks();
				await onDashboardChanged();
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
				borrow.textContent = "Borrow this copy";
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
			copiesSummary.textContent = `${copies.length} ${copies.length === 1 ? "copy" : "copies"} in the library`;
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
			["Author", book.author],
			["ISBN", book.isbn],
			["Genre", book.genre],
			["Publication year", book.publicationYear]
		];
		bookDetails.replaceChildren();
		details.forEach(([label, value]) => {
			const term = document.createElement("dt");
			term.textContent = label;
			const description = document.createElement("dd");
			description.textContent = value || "Not available";
			bookDetails.append(term, description);
		});
		if (book.description) {
			const term = document.createElement("dt");
			term.textContent = "Description";
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

	async function handleBookAction(event) {
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
				showToast("Delete this title's copies before deleting the title.", true);
				return;
			}
			if (!window.confirm(`Delete "${book.title}"? This cannot be undone.`)) {
				return;
			}
			setButtonBusy(button, true);
			try {
				await apiRequest(`/api/books/${book.id}`, { method: "DELETE" });
				showToast("Book deleted.");
				await loadBooks();
			} catch (error) {
				showToast(userMessage(error), true);
			} finally {
				setButtonBusy(button, false);
			}
		}
	}

	booksList.addEventListener("click", handleBookAction);
	booksCards.addEventListener("click", handleBookAction);

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
			showToast(id ? "Book updated." : "Book added.");
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
			setFeedback(copiesFeedback, "Copy added.");
			await loadCopies();
			await loadBooks();
			await onDashboardChanged();
		} catch (error) {
			setFeedback(copiesFeedback, userMessage(error), true);
		} finally {
			setButtonBusy(submitButton, false);
		}
	});

	setRole("PUBLIC");
	return { loadBooks, setRole };
}
