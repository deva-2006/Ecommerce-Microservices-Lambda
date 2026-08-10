import { initTheme } from "./theme.js";
initTheme();

import { doConfirmSignUp, doResendSignUp, doSignIn, isAdmin } from "./auth.js";
import api, { warmUpLambdas } from "./api.js";

const emailInput = document.getElementById("email");
const codeInput = document.getElementById("code");
const msg = document.getElementById("msg");
const resendBtn = document.getElementById("resendBtn");

const pending = sessionStorage.getItem("pendingEmail");
if (pending) emailInput.value = pending;

document.getElementById("confirmBtn").addEventListener("click", async () => {
  msg.textContent = "";
  const email = emailInput.value.trim();
  const code = codeInput.value.trim();

  if (!email || !code) {
    msg.textContent = "Email and code required.";
    msg.className = "error";
    return;
  }

  const result = await doConfirmSignUp(email, code);

  if (result.success) {
    const password = sessionStorage.getItem("pendingPassword");
    sessionStorage.removeItem("pendingEmail");
    sessionStorage.removeItem("pendingPassword");

    if (password) {
      msg.textContent = "Verified! Logging you in...";
      msg.className = "success";
      const loginResult = await doSignIn(email, password);
      if (loginResult.success) {
        const admin = await isAdmin();
        warmUpLambdas();
        setTimeout(() => {
          window.location.href = admin ? "/admin/dashboard.html" : "/home.html";
        }, 600);
      } else {
        msg.textContent = "Verified! Redirecting to login...";
        msg.className = "success";
        setTimeout(() => (window.location.href = "/login.html"), 1000);
      }
    } else {
      msg.textContent = "Verified! Redirecting to login...";
      msg.className = "success";
      setTimeout(() => (window.location.href = "/login.html"), 1000);
    }
  } else {
    msg.textContent = result.error;
    msg.className = "error";
  }
});

resendBtn.addEventListener("click", async () => {
  const email = emailInput.value.trim();
  if (!email) {
    msg.textContent = "Enter your email first.";
    msg.className = "error";
    return;
  }

  resendBtn.disabled = true;
  resendBtn.textContent = "Sending...";

  const result = await doResendSignUp(email);

  if (result.success) {
    msg.textContent = `A new code has been sent to ${email}.`;
    msg.className = "success";
  } else {
    msg.textContent = result.error;
    msg.className = "error";
  }

  resendBtn.textContent = "Resend Code";
  setTimeout(() => { resendBtn.disabled = false; }, 30000);
});
