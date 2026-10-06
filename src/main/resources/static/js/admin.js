import { apiRequest, userMessage } from "./api.js";

const ROLE_LABELS = {
	ADMIN: "Quản trị viên",
	STAFF: "Nhân viên",
	PATRON: "Bạn đọc"
};

export function initializeAdmin(showToast) {
	const form = document.querySelector("#staff-create-form");
	const accountList = document.querySelector("#admin-accounts-list");
	const loading = document.querySelector("#admin-accounts-loading");
	const feedback = document.querySelector("#admin-feedback");
	const credentialDialog = document.querySelector("#temporary-password-dialog");
	const credentialUsername = document.querySelector("#temporary-password-account");
	const credentialValue = document.querySelector("#temporary-password-value");

	function setFeedback(message, isError = false) {
		feedback.textContent = message;
		feedback.classList.toggle("error", isError);
		feedback.classList.toggle("success", !isError);
		feedback.hidden = !message;
	}

	function clearTemporaryPassword() {
		credentialValue.textContent = "";
		credentialUsername.textContent = "";
	}

	function showTemporaryPassword(account) {
		credentialUsername.textContent = `Tài khoản: ${account.username}`;
		credentialValue.textContent = account.temporaryPassword;
		credentialDialog.showModal();
	}

	function createAccountRow(account) {
		const row = document.createElement("tr");
		const usernameCell = document.createElement("td");
		usernameCell.textContent = account.username;
		const roleCell = document.createElement("td");
		roleCell.textContent = ROLE_LABELS[account.role] || account.role;
		const nameCell = document.createElement("td");
		nameCell.textContent = account.fullName || "—";
		const actionsCell = document.createElement("td");

		if (account.role !== "ADMIN") {
			const resetButton = document.createElement("button");
			resetButton.type = "button";
			resetButton.className = "button button-secondary button-small";
			resetButton.textContent = account.passwordChangeRequired ? "Cấp lại mật khẩu tạm" : "Đặt lại mật khẩu";
			resetButton.setAttribute("aria-label", `Đặt lại mật khẩu cho ${account.username}`);
			resetButton.addEventListener("click", async () => {
				if (!window.confirm(`Hãy chắc chắn bạn đã xác minh danh tính của ${account.username}. Tiếp tục cấp mật khẩu tạm?`)) {
					return;
				}
				resetButton.disabled = true;
				try {
					const temporary = await apiRequest(
						`/api/admin/accounts/${account.id}/password-reset`,
						{ method: "POST" }
					);
					showTemporaryPassword(temporary);
					await loadAccounts();
				} catch (error) {
					showToast(userMessage(error), true);
				} finally {
					resetButton.disabled = false;
				}
			});
			actionsCell.append(resetButton);
		} else {
			actionsCell.textContent = "Khôi phục trên máy chủ";
			actionsCell.className = "muted";
		}
		row.append(usernameCell, roleCell, nameCell, actionsCell);
		return row;
	}

	async function loadAccounts() {
		loading.hidden = false;
		accountList.replaceChildren();
		setFeedback("");
		try {
			const accounts = await apiRequest("/api/admin/accounts");
			accountList.replaceChildren(...accounts.map(createAccountRow));
		} catch (error) {
			setFeedback(userMessage(error), true);
		} finally {
			loading.hidden = true;
		}
	}

	form.addEventListener("input", () => setFeedback(""));
	form.addEventListener("submit", async event => {
		event.preventDefault();
		setFeedback("");
		if (!form.reportValidity()) {
			return;
		}
		const submit = form.querySelector('[type="submit"]');
		submit.disabled = true;
		try {
			const account = await apiRequest("/api/admin/accounts/staff", {
				method: "POST",
				body: JSON.stringify({ username: form.elements.username.value.trim() })
			});
			form.reset();
			showTemporaryPassword(account);
			await loadAccounts();
		} catch (error) {
			setFeedback(userMessage(error), true);
		} finally {
			submit.disabled = false;
		}
	});

	document.querySelectorAll('[data-close="temporary-password-dialog"]').forEach(button => {
		button.addEventListener("click", () => {
			clearTemporaryPassword();
			credentialDialog.close();
		});
	});
	credentialDialog.addEventListener("close", () => {
		clearTemporaryPassword();
	});

	return { loadAccounts };
}
