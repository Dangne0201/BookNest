import { apiRequest, userMessage } from "./api.js";

export function initializeProfile(showToast) {
	const button = document.querySelector("#profile-button");
	const dialog = document.querySelector("#profile-dialog");
	const form = document.querySelector("#profile-form");
	const errorBox = document.querySelector("#profile-form-error");

	function setError(message) {
		errorBox.textContent = message;
		errorBox.hidden = !message;
	}

	button.addEventListener("click", async () => {
		form.reset();
		setError("");
		button.disabled = true;
		try {
			const profile = await apiRequest("/api/members/me");
			form.elements.fullName.value = profile.fullName;
			form.elements.email.value = profile.email || "";
			form.elements.phone.value = profile.phone || "";
			form.elements.notes.value = profile.notes || "";
			button.disabled = false;
			button.focus();
			dialog.showModal();
			form.elements.fullName.focus();
		} catch (error) {
			showToast(userMessage(error), true);
		} finally {
			button.disabled = false;
		}
	});

	form.addEventListener("input", () => setError(""));
	form.addEventListener("submit", async event => {
		event.preventDefault();
		setError("");
		if (!form.reportValidity()) {
			return;
		}
		const submit = form.querySelector('[type="submit"]');
		submit.disabled = true;
		try {
			await apiRequest("/api/members/me", {
				method: "PUT",
				body: JSON.stringify({
					fullName: form.elements.fullName.value.trim(),
					email: form.elements.email.value.trim() || null,
					phone: form.elements.phone.value.trim() || null,
					notes: form.elements.notes.value.trim() || null
				})
			});
			dialog.close();
			showToast("Đã cập nhật hồ sơ của bạn.");
		} catch (error) {
			setError(userMessage(error));
		} finally {
			submit.disabled = false;
		}
	});

	document.querySelectorAll('[data-close="profile-dialog"]').forEach(closeButton => {
		closeButton.addEventListener("click", () => dialog.close());
	});

	return {
		setRole(role) {
			button.hidden = role !== "PATRON";
		}
	};
}
