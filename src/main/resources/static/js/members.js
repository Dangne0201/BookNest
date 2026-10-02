import { apiRequest, userMessage } from "./api.js";

export function initializeMembers(showToast) {
	const membersPanel = document.querySelector("#members-panel");
	const membersList = document.querySelector("#members-list");
	const membersTable = document.querySelector("#members-table-wrap");
	const membersEmpty = document.querySelector("#members-empty");
	const membersLoading = document.querySelector("#members-loading");
	const membersFeedback = document.querySelector("#members-feedback");
	const memberDialog = document.querySelector("#member-dialog");
	const memberDialogTitle = document.querySelector("#member-dialog-title");
	const memberForm = document.querySelector("#member-form");
	const memberFormError = document.querySelector("#member-form-error");

	let members = [];

	function setFeedback(message, isError = false) {
		membersFeedback.textContent = message;
		membersFeedback.classList.toggle("error", isError);
		membersFeedback.classList.toggle("success", !isError);
		membersFeedback.hidden = !message;
	}

	function makeActionButton(label, action, memberId, className) {
		const button = document.createElement("button");
		button.type = "button";
		button.className = className;
		button.textContent = label;
		button.dataset.action = action;
		button.dataset.memberId = String(memberId);
		return button;
	}

	function renderMember(member) {
		const row = document.createElement("tr");

		const nameCell = document.createElement("td");
		const name = document.createElement("span");
		name.className = "member-name";
		name.textContent = member.fullName;
		nameCell.append(name);

		const contactCell = document.createElement("td");
		const contactDetails = [member.email, member.phone].filter(Boolean);
		if (contactDetails.length === 0) {
			contactCell.textContent = "Chưa có thông tin";
			contactCell.className = "muted";
		} else {
			contactDetails.forEach(value => {
				const detail = document.createElement("span");
				detail.className = "member-contact";
				detail.textContent = value;
				contactCell.append(detail);
			});
		}

		const notesCell = document.createElement("td");
		if (member.notes) {
			const notes = document.createElement("span");
			notes.className = "member-note";
			notes.textContent = member.notes;
			notesCell.append(notes);
		} else {
			notesCell.textContent = "—";
			notesCell.className = "muted";
		}

		const actionsCell = document.createElement("td");
		const actions = document.createElement("div");
		actions.className = "row-actions";
		const editButton = makeActionButton("Sửa", "edit", member.id, "button button-quiet button-small");
		editButton.setAttribute("aria-label", `Sửa thành viên ${member.fullName}`);
		const deleteButton = makeActionButton("Xóa", "delete", member.id, "button button-danger button-small");
		deleteButton.setAttribute("aria-label", `Xóa thành viên ${member.fullName}`);
		actions.append(editButton, deleteButton);
		actionsCell.append(actions);
		row.append(nameCell, contactCell, notesCell, actionsCell);
		return row;
	}

	function renderMembers() {
		membersList.replaceChildren(...members.map(renderMember));
		membersLoading.hidden = true;
		membersTable.hidden = members.length === 0;
		membersEmpty.hidden = members.length !== 0;
	}

	async function loadMembers() {
		membersLoading.hidden = false;
		membersTable.hidden = true;
		membersEmpty.hidden = true;
		setFeedback("");
		try {
			members = await apiRequest("/api/members");
			renderMembers();
		} catch (error) {
			membersLoading.hidden = true;
			setFeedback(userMessage(error), true);
		}
	}

	function showMemberForm(member = null) {
		memberForm.reset();
		memberFormError.textContent = "";
		memberFormError.hidden = true;
		memberForm.elements.id.value = member ? String(member.id) : "";
		memberForm.elements.fullName.value = member?.fullName || "";
		memberForm.elements.email.value = member?.email || "";
		memberForm.elements.phone.value = member?.phone || "";
		memberForm.elements.notes.value = member?.notes || "";
		memberDialogTitle.textContent = member ? "Chỉnh sửa thành viên" : "Thêm thành viên";
		memberDialog.showModal();
		memberForm.elements.fullName.focus();
	}

	document.querySelector("#add-member-button").addEventListener("click", () => showMemberForm());
	document.querySelector('[data-action="add-member"]').addEventListener("click", () => showMemberForm());
	document.querySelectorAll('[data-close="member-dialog"]').forEach(button => {
		button.addEventListener("click", () => memberDialog.close());
	});

	membersList.addEventListener("click", async event => {
		const button = event.target.closest("button[data-action]");
		if (!button) {
			return;
		}
		const member = members.find(item => item.id === Number(button.dataset.memberId));
		if (!member) {
			return;
		}
		if (button.dataset.action === "edit") {
			showMemberForm(member);
			return;
		}
		if (button.dataset.action !== "delete"
				|| !window.confirm(`Xóa hồ sơ thành viên "${member.fullName}"? Thao tác này không thể hoàn tác.`)) {
			return;
		}

		button.disabled = true;
		try {
			await apiRequest(`/api/members/${member.id}`, { method: "DELETE" });
			await loadMembers();
			showToast("Đã xóa thành viên.");
		} catch (error) {
			showToast(userMessage(error), true);
		} finally {
			button.disabled = false;
		}
	});

	memberForm.addEventListener("input", () => {
		memberFormError.textContent = "";
		memberFormError.hidden = true;
	});

	memberForm.addEventListener("submit", async event => {
		event.preventDefault();
		memberFormError.textContent = "";
		memberFormError.hidden = true;
		if (!memberForm.reportValidity()) {
			return;
		}

		const submitButton = memberForm.querySelector('[type="submit"]');
		submitButton.disabled = true;
		const form = new FormData(memberForm);
		const id = form.get("id");
		const optionalValue = name => {
			const value = form.get(name).trim();
			return value || null;
		};
		const body = JSON.stringify({
			fullName: form.get("fullName").trim(),
			email: optionalValue("email"),
			phone: optionalValue("phone"),
			notes: optionalValue("notes")
		});

		try {
			await apiRequest(id ? `/api/members/${id}` : "/api/members", {
				method: id ? "PUT" : "POST",
				body
			});
			memberDialog.close();
			await loadMembers();
			showToast(id ? "Đã cập nhật thành viên." : "Đã thêm thành viên.");
		} catch (error) {
			memberFormError.textContent = userMessage(error);
			memberFormError.hidden = false;
		} finally {
			submitButton.disabled = false;
		}
	});

	return { loadMembers };
}
