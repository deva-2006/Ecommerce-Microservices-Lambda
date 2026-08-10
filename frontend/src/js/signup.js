import { initTheme } from "./theme.js";
initTheme();

import { doSignUp } from "./auth.js";
import { warmUpProductsService } from "./api.js";

// Fire product service health warm-up immediately before login / registration
warmUpProductsService();

const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");
const msg = document.getElementById("msg");

const strengthContainer = document.getElementById("strengthContainer");
const strengthBar = document.getElementById("strengthBar");
const strengthText = document.getElementById("strengthText");

passwordInput?.addEventListener("input", () => {
  const val = passwordInput.value;
  if (!val) {
    strengthContainer.style.display = "none";
    return;
  }
  strengthContainer.style.display = "block";
  let score = 0;
  if (val.length >= 8) score++;
  if (/[A-Z]/.test(val)) score++;
  if (/[0-9]/.test(val)) score++;
  if (/[^A-Za-z0-9]/.test(val)) score++;

  if (score <= 1) {
    strengthBar.className = "password-strength-bar strength-weak";
    strengthText.textContent = "Weak password";
    strengthText.style.color = "var(--danger)";
  } else if (score <= 3) {
    strengthBar.className = "password-strength-bar strength-med";
    strengthText.textContent = "Medium password";
    strengthText.style.color = "var(--warning)";
  } else {
    strengthBar.className = "password-strength-bar strength-strong";
    strengthText.textContent = "Strong password ✓";
    strengthText.style.color = "var(--success)";
  }
});

const signupForm = document.getElementById("signupForm");

async function handleSignupSubmit(e) {
  if (e) e.preventDefault();
  msg.textContent = "";
  const email = emailInput.value.trim();
  const password = passwordInput.value;

  if (!email || !password) {
    msg.textContent = "Email and password required.";
    msg.className = "error";
    return;
  }

  const signupBtn = document.getElementById("signupBtn");
  if (signupBtn) signupBtn.disabled = true;

  const result = await doSignUp(email, password);

  if (signupBtn) signupBtn.disabled = false;

  if (result.success) {
    msg.textContent = "Signup successful! Check your email for a code.";
    msg.className = "success";
    sessionStorage.setItem("pendingEmail", email);
    setTimeout(() => (window.location.href = "/confirm.html"), 1500);
  } else if (result.code === "UsernameExistsException") {
    sessionStorage.setItem("pendingEmail", email);
    sessionStorage.setItem("pendingPassword", password);
    await doResendSignUp(email);
    window.location.href = "/confirm.html";
  } else {
    msg.textContent = result.error;
    msg.className = "error";
  }
}

if (signupForm) {
  signupForm.addEventListener("submit", handleSignupSubmit);
}
