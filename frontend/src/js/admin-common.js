import { requireAdmin, doSignOut } from "./auth.js";
import { hideLoader } from "../components/loader.js";
import { initTheme, toggleTheme, getTheme } from "./theme.js";
import api, { warmUpLambdas } from "./api.js";

initTheme();

const path = window.location.pathname;

function wireThemeToggle() {
  const btn = document.getElementById("adminThemeToggle");
  if (!btn) return;
  btn.addEventListener("click", () => {
    const next = toggleTheme();
    btn.textContent = next === "dark" ? "Light" : "Dark";
  });
}

function closeSidebar() {
  const sidebar = document.getElementById("adminSidebar");
  const backdrop = document.getElementById("adminSidebarBackdrop");
  if (sidebar) sidebar.classList.remove("open");
  if (backdrop) backdrop.classList.remove("open");
}

const navEl = document.getElementById("navbar");
if (navEl) {
  navEl.innerHTML = `
    <header class="header">
      <div class="header-inner">
        <a href="/admin/dashboard.html" class="nav-brand">
          <span class="nav-brand-logo-badge">
            <svg width="20" height="20" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="32" height="32" rx="9" fill="url(#svNavGradAdmin)"/>
              <path d="M21 11.5C21 9.567 19.433 8 17.5 8H13.5C11.567 8 10 9.567 10 11.5C10 13.433 11.567 15 13.5 15H18.5C20.433 15 22 16.567 22 18.5C22 20.433 20.433 22 18.5 22H14.5C12.567 22 11 20.433 11 18.5" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
              <defs>
                <linearGradient id="svNavGradAdmin" x1="0" y1="0" x2="32" y2="32" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#4f46e5"/>
                  <stop offset="0.5" stop-color="#7c3aed"/>
                  <stop offset="1" stop-color="#e11d48"/>
                </linearGradient>
              </defs>
            </svg>
          </span>
          ShopVibe Admin
        </a>
        <nav class="nav-actions">
          <button id="adminThemeToggle" class="btn btn-ghost btn-sm">${getTheme() === "dark" ? "Light" : "Dark"}</button>
          <span class="nav-user-avatar" id="adminProfileTrigger">A</span>
          <button id="adminLogoutBtn" class="btn btn-ghost btn-sm sign-out-desktop">Sign Out</button>
        </nav>
      </div>
    </header>

    <div class="mobile-profile-dropdown" id="adminMobileProfileDropdown">
      <div class="mobile-profile-dropdown-item" style="pointer-events:none;opacity:0.6;font-weight:600;">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        Admin
      </div>
      <button id="adminMobileThemeToggle" class="mobile-profile-dropdown-item">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
        ${getTheme() === "dark" ? "Light Mode" : "Dark Mode"}
      </button>
      <a href="/home.html" class="mobile-profile-dropdown-item">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        View Store
      </a>
      <button id="adminMobileLogoutBtn" class="mobile-profile-dropdown-item">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        Sign Out
      </button>
    </div>

    <nav class="mobile-bottom-nav admin-bottom-nav">
      <a href="/admin/dashboard.html" class="mobile-bottom-nav-item${path === "/admin/dashboard.html" ? " active" : ""}">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
        <span>Dashboard</span>
      </a>
      <a href="/admin/products.html" class="mobile-bottom-nav-item${path === "/admin/products.html" ? " active" : ""}">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/><path d="m3.3 7 8.7 5 8.7-5M12 22V12"/></svg>
        <span>Products</span>
      </a>
      <a href="/admin/inventory.html" class="mobile-bottom-nav-item${path === "/admin/inventory.html" ? " active" : ""}">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
        <span>Inventory</span>
      </a>
      <a href="/admin/orders.html" class="mobile-bottom-nav-item${path === "/admin/orders.html" ? " active" : ""}">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
        <span>Orders</span>
      </a>
      <a href="/admin/reviews.html" class="mobile-bottom-nav-item${path === "/admin/reviews.html" ? " active" : ""}">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
        <span>Reviews</span>
      </a>
    </nav>
    <div class="site-credit">Deva S</div>`;

  wireThemeToggle();

  const mobileThemeBtn = document.getElementById("adminMobileThemeToggle");
  if (mobileThemeBtn) {
    mobileThemeBtn.addEventListener("click", () => {
      const next = toggleTheme();
      mobileThemeBtn.childNodes[mobileThemeBtn.childNodes.length - 1].textContent = next === "dark" ? "Light Mode" : "Dark Mode";
      const desktopBtn = document.getElementById("adminThemeToggle");
      if (desktopBtn) desktopBtn.textContent = next === "dark" ? "Light" : "Dark";
    });
  }

  const logoutBtn = document.getElementById("adminLogoutBtn");
  if (logoutBtn) {
    logoutBtn.addEventListener("click", async () => {
      await doSignOut();
      window.location.href = "/login.html";
    });
  }

  const mobileLogoutBtn = document.getElementById("adminMobileLogoutBtn");
  if (mobileLogoutBtn) {
    mobileLogoutBtn.addEventListener("click", async () => {
      await doSignOut();
      window.location.href = "/login.html";
    });
  }

  const profileTrigger = document.getElementById("adminProfileTrigger");
  const profileDropdown = document.getElementById("adminMobileProfileDropdown");
  if (profileTrigger && profileDropdown) {
    profileTrigger.addEventListener("click", (e) => {
      e.preventDefault();
      e.stopPropagation();
      profileDropdown.classList.toggle("open");
    });
    document.addEventListener("click", (e) => {
      if (!profileDropdown.contains(e.target) && !profileTrigger.contains(e.target)) {
        profileDropdown.classList.remove("open");
      }
    });
  }
}

function warmUpAdminEndpoints() {
  warmUpLambdas();
  api.get("/orders/all").catch(() => {});
  api.get("/products").catch(() => {});
  api.get("/inventory").catch(() => {});
}

// Immediate execution on module load
warmUpAdminEndpoints();

requireAdmin().then((ok) => {
  if (ok) {
    hideLoader("loader");
    warmUpAdminEndpoints();
  }
});

const sidebarToggle = document.getElementById("adminSidebarToggle");
const adminSidebar = document.getElementById("adminSidebar");
const sidebarBackdrop = document.getElementById("adminSidebarBackdrop");
if (sidebarToggle && adminSidebar) {
  sidebarToggle.addEventListener("click", () => {
    adminSidebar.classList.toggle("open");
    if (sidebarBackdrop) sidebarBackdrop.classList.toggle("open");
  });
  if (sidebarBackdrop) {
    sidebarBackdrop.addEventListener("click", closeSidebar);
  }
  adminSidebar.querySelectorAll("a").forEach((link) => {
    link.addEventListener("click", closeSidebar);
  });
}
