import { apiRequest, clearCsrfToken, userMessage } from "./api.js";

export function initializeAuth({ onAuthenticated, onLoggedOut, onPublicView, showToast }) {
	const authView = document.querySelector("#auth-view");
	const accountBar = document.querySelector("#account-bar");
	const usernameLabel = document.querySelector("#current-username");
	const workspace = document.querySelector("#workspace-view");
	const loginTab = document.querySelector("#login-tab");
	const registerTab = document.querySelector("#register-tab");
	const loginPanel = document.querySelector("#login-panel");
	const registerPanel = document.querySelector("#register-panel");
	const loginForm = document.querySelector("#login-form");
	const registerForm = document.querySelector("#register-form");
	const logoutButton = document.querySelector("#logout-button");
	const changePasswordButton = document.querySelector("#change-password-button");
	const passwordDialog = document.querySelector("#password-dialog");
	const passwordForm = document.querySelector("#password-form");
	const passwordFormError = document.querySelector("#password-form-error");
	const forgotPasswordButton = document.querySelector("#forgot-password-button");
	const forgotPasswordHelp = document.querySelector("#forgot-password-help");
	const passwordCloseButtons = document.querySelectorAll('[data-close="password-dialog"]');
	let passwordChangeRequired = false;

	function setTab(activeTab) {
		const loginActive = activeTab === loginTab;
		loginTab.classList.toggle("is-active", loginActive);
		registerTab.classList.toggle("is-active", !loginActive);
		loginTab.setAttribute("aria-selected", String(loginActive));
		registerTab.setAttribute("aria-selected", String(!loginActive));
		loginPanel.hidden = !loginActive;
		registerPanel.hidden = loginActive;
		(loginActive ? loginTab : registerTab).focus();
	}

	function setFormError(form, message) {
		const error = form.querySelector(".form-error");
		if (error) {
			error.textContent = message;
			error.hidden = !message;
		}
	}

	function setFormBusy(form, busy) {
		const submitButton = form.querySelector('[type="submit"]');
		if (submitButton) {
			submitButton.disabled = busy;
		}
	}

	function showSignedOut(message) {
		passwordChangeRequired = false;
		clearCsrfToken();
		if (passwordDialog.open) {
			passwordDialog.close();
		}
		authView.hidden = false;
		accountBar.hidden = true;
		workspace.hidden = false;
		onLoggedOut();
		onPublicView();
		if (message) {
			showToast(message, true);
		}
	}

	async function showSignedIn(account) {
		usernameLabel.textContent = account.username;
		authView.hidden = true;
		accountBar.hidden = false;
		passwordChangeRequired = account.passwordChangeRequired;
		changePasswordButton.hidden = false;
		changePasswordButton.textContent = passwordChangeRequired
			? "Change temporary password"
			: "Change password";
		if (passwordChangeRequired) {
			workspace.hidden = true;
			passwordForm.reset();
			setFormError(passwordForm, "");
			passwordDialog.showModal();
			passwordForm.elements.currentPassword.focus();
			showToast("You are using a temporary password. Change it to continue.");
			return;
		}
		passwordCloseButtons.forEach(button => {
			button.hidden = false;
		});
		workspace.hidden = false;
		await onAuthenticated(account);
	}

	loginTab.addEventListener("click", () => setTab(loginTab));
	registerTab.addEventListener("click", () => setTab(registerTab));
	[loginTab, registerTab].forEach(tab => {
		tab.addEventListener("keydown", event => {
			if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
				event.preventDefault();
				setTab(tab === loginTab ? registerTab : loginTab);
			}
		});
	});

	loginForm.addEventListener("input", () => setFormError(loginForm, ""));
	registerForm.addEventListener("input", () => setFormError(registerForm, ""));
	passwordForm.addEventListener("input", () => setFormError(passwordForm, ""));
	forgotPasswordButton.addEventListener("click", () => {
		forgotPasswordHelp.hidden = !forgotPasswordHelp.hidden;
		forgotPasswordButton.setAttribute("aria-expanded", String(!forgotPasswordHelp.hidden));
	});

	loginForm.addEventListener("submit", async event => {
		event.preventDefault();
		setFormError(loginForm, "");
		setFormBusy(loginForm, true);
		try {
			await apiRequest("/api/auth/login", {
				method: "POST",
				body: new URLSearchParams(new FormData(loginForm))
			});
			clearCsrfToken();
			const account = await apiRequest("/api/auth/me");
			await showSignedIn(account);
			loginForm.reset();
			if (!account.passwordChangeRequired) {
				showToast("Signed in successfully.");
			}
		} catch (error) {
			setFormError(loginForm, userMessage(error));
		} finally {
			setFormBusy(loginForm, false);
		}
	});

	registerForm.addEventListener("submit", async event => {
		event.preventDefault();
		setFormError(registerForm, "");
		setFormBusy(registerForm, true);
		const formData = new FormData(registerForm);
		try {
			await apiRequest("/api/auth/register", {
				method: "POST",
				body: JSON.stringify({
					username: formData.get("username"),
					password: formData.get("password"),
					fullName: formData.get("fullName").trim(),
					email: formData.get("email").trim() || null,
					phone: formData.get("phone").trim() || null,
					notes: null
				})
			});
			const username = formData.get("username");
			registerForm.reset();
			setTab(loginTab);
			loginForm.elements.username.value = username;
			loginForm.elements.password.focus();
			showToast("Registration successful. You can now sign in and borrow books.");
		} catch (error) {
			setFormError(registerForm, userMessage(error));
		} finally {
			setFormBusy(registerForm, false);
		}
	});

	logoutButton.addEventListener("click", async () => {
		logoutButton.disabled = true;
		try {
			await apiRequest("/api/auth/logout", { method: "POST" });
			clearCsrfToken();
			showSignedOut();
			setTab(loginTab);
			showToast("You have signed out.");
		} catch (error) {
			showToast(userMessage(error), true);
		} finally {
			logoutButton.disabled = false;
		}
	});

	changePasswordButton.addEventListener("click", () => {
		passwordForm.reset();
		setFormError(passwordForm, "");
		passwordDialog.showModal();
		passwordForm.elements.currentPassword.focus();
	});
	passwordCloseButtons.forEach(button => {
		button.addEventListener("click", () => passwordDialog.close());
	});

	passwordForm.addEventListener("submit", async event => {
		event.preventDefault();
		setFormError(passwordForm, "");
		if (!passwordForm.reportValidity()) {
			return;
		}
		if (passwordForm.elements.newPassword.value !== passwordForm.elements.confirmPassword.value) {
			setFormError(passwordForm, "The passwords do not match.");
			passwordForm.elements.confirmPassword.focus();
			return;
		}

		setFormBusy(passwordForm, true);
		try {
			await apiRequest("/api/auth/password", {
				method: "POST",
				body: JSON.stringify({
					currentPassword: passwordForm.elements.currentPassword.value,
					newPassword: passwordForm.elements.newPassword.value
				})
			});
			passwordDialog.close();
			passwordForm.reset();
			passwordChangeRequired = false;
			clearCsrfToken();
			showSignedOut();
			setTab(loginTab);
			showToast("Password changed. Please sign in again with your new password.");
		} catch (error) {
			setFormError(passwordForm, userMessage(error));
		} finally {
			setFormBusy(passwordForm, false);
		}
	});

	async function restoreSession() {
		try {
			const account = await apiRequest("/api/auth/me");
			await showSignedIn(account);
		} catch (error) {
			if (error.status === 401) {
				showSignedOut();
				return;
			}
			showSignedOut(userMessage(error));
		}
	}

	return { restoreSession };
}
