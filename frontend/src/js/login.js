import { initTheme, toggleTheme, getTheme } from "./theme.js";
initTheme();

import { doSignIn, doResendSignUp, isAdmin, fetchUserAttributes, doSignOut } from "./auth.js";
import { warmUpAllServices, warmUpOtherServices, warmUpLambdas } from "./api.js";

// Warm every service Lambda via health endpoints immediately on page load,
// independent of whether the user signs in as customer or admin.
warmUpAllServices();

const themeBtn = document.getElementById("themeToggle");
themeBtn.textContent = getTheme() === "dark" ? "Light mode" : "Dark mode";
themeBtn.addEventListener("click", () => {
  const next = toggleTheme();
  themeBtn.textContent = next === "dark" ? "Light mode" : "Dark mode";
});

const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");
const msg = document.getElementById("msg");
const customerTab = document.getElementById("customerTab");
const adminTab = document.getElementById("adminTab");
const signupLink = document.getElementById("signupLink");

// Password visibility toggle
const passwordToggle = document.getElementById('passwordToggle');
const eyeIcon = document.getElementById('eyeIcon');
if (passwordToggle) {
  passwordToggle.addEventListener('click', () => {
    if (passwordInput.type === 'password') {
      passwordInput.type = 'text';
      eyeIcon.innerHTML = `<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>`;
    } else {
      passwordInput.type = 'password';
      eyeIcon.innerHTML = `<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>`;
    }
  });
}

// Remember me
const rememberMe = document.getElementById("rememberMe");
const savedEmail = localStorage.getItem("shopvibe_remembered_email");
if (savedEmail) {
  emailInput.value = savedEmail;
  if (rememberMe) rememberMe.checked = true;
}

const forgotPasswordBtn = document.getElementById("forgotPasswordBtn");
if (forgotPasswordBtn) {
  forgotPasswordBtn.addEventListener("click", (e) => {
    e.preventDefault();
    const email = emailInput.value.trim();
    if (!email) {
      alert("Please enter your email address first.");
    } else {
      alert(`Password reset instructions sent to ${email}`);
    }
  });
}

let mode = "customer";

customerTab.addEventListener("click", () => {
  mode = "customer";
  customerTab.classList.add("active");
  adminTab.classList.remove("active");
  signupLink.style.display = "block";
});

adminTab.addEventListener("click", () => {
  mode = "admin";
  adminTab.classList.add("active");
  customerTab.classList.remove("active");
  signupLink.style.display = "none";
  // Warm every service lambda the moment the user picks Admin login so the
  // dashboard renders fast right after sign-in.
  warmUpLambdas();
});

const loginForm = document.getElementById("loginForm");

async function handleLoginSubmit(e) {
  if (e) e.preventDefault();
  msg.textContent = "";
  const email = emailInput.value.trim();
  const password = passwordInput.value;

  if (!email || !password) {
    msg.textContent = "Email and password required.";
    msg.className = "error";
    return;
  }

  const loginBtn = document.getElementById("loginBtn");
  const originalHTML = loginBtn.innerHTML;
  loginBtn.disabled = true;
  loginBtn.innerHTML = '<span class="btn-spinner"></span> Signing you in...';
  loginBtn.classList.add("btn-loading");

  const result = await doSignIn(email, password);

  if (!result.success) {
    const errCode = result.code || "";
    if (errCode === "UserNotConfirmedException") {
      sessionStorage.setItem("pendingEmail", email);
      sessionStorage.setItem("pendingPassword", password);
      try { await doSignOut(); } catch (e) {}
      await doResendSignUp(email);
      window.location.href = "/confirm.html";
      return;
    }
    loginBtn.disabled = false;
    loginBtn.innerHTML = originalHTML;
    loginBtn.classList.remove("btn-loading");
    msg.textContent = result.error;
    msg.className = "error";
    return;
  }

  let attrs;
  try { attrs = await fetchUserAttributes(); } catch (e) { attrs = {}; }

  if (attrs.email_verified !== "true") {
    sessionStorage.setItem("pendingEmail", email);
    sessionStorage.setItem("pendingPassword", password);
    try { await doSignOut(); } catch (e) {}
    await doResendSignUp(email);
    window.location.href = "/confirm.html";
    return;
  }

  const admin = await isAdmin();

  if (mode === "admin" && !admin) {
    msg.textContent = "This account is not an Admin. Use Customer Login instead.";
    msg.className = "error";
    loginBtn.disabled = false;
    loginBtn.innerHTML = originalHTML;
    loginBtn.classList.remove("btn-loading");
    return;
  }

  // Handle remember me
  if (rememberMe && rememberMe.checked) {
    localStorage.setItem("shopvibe_remembered_email", email);
  } else {
    localStorage.removeItem("shopvibe_remembered_email");
  }

  localStorage.setItem("userEmail", email);

  msg.textContent = "Login successful! Redirecting...";
  msg.className = "success";
  warmUpOtherServices();

  setTimeout(() => {
    window.location.href = admin ? "/admin/dashboard.html" : "/home.html";
  }, 600);
}

if (loginForm) {
  loginForm.addEventListener("submit", handleLoginSubmit);
}
