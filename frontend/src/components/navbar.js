import { doSignOut, getAccessToken, getCachedUserAttributes } from "../js/auth.js";
import { initTheme, toggleTheme, getTheme } from "../js/theme.js";
import api, { getProductsCached } from "../js/api.js";

initTheme();

function getCachedEmail() {
  return localStorage.getItem("userEmail") || localStorage.getItem("shopvibe_remembered_email") || "";
}

function cacheEmail(email) {
  if (!email) return;
  try { localStorage.setItem("userEmail", email); } catch {}
}

export async function renderNavbar(containerId) {
  const container = document.getElementById(containerId);
  if (!container) return;

  const path = window.location.pathname;
  const isUnauthPage = path === "/login.html" || path === "/signup.html" || path === "/confirm.html";
  const isAdminRoute = path.includes("/admin/");

  const cachedEmail = getCachedEmail();
  const cachedInitial = cachedEmail ? cachedEmail.charAt(0).toUpperCase() : "?";

  const logoSvg = `
    <svg width="20" height="20" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="32" height="32" rx="9" fill="url(#svNavGrad)"/>
      <path d="M21 11.5C21 9.567 19.433 8 17.5 8H13.5C11.567 8 10 9.567 10 11.5C10 13.433 11.567 15 13.5 15H18.5C20.433 15 22 16.567 22 18.5C22 20.433 20.433 22 18.5 22H14.5C12.567 22 11 20.433 11 18.5" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
      <defs>
        <linearGradient id="svNavGrad" x1="0" y1="0" x2="32" y2="32" gradientUnits="userSpaceOnUse">
          <stop stop-color="#4f46e5"/><stop offset="0.5" stop-color="#7c3aed"/><stop offset="1" stop-color="#e11d48"/>
        </linearGradient>
      </defs>
    </svg>`;

  if (isUnauthPage) {
    container.innerHTML = `
      <header class="header">
        <div class="header-inner">
          <a href="/home.html" class="nav-brand">
            <span class="nav-brand-logo-badge">${logoSvg}</span>
            ShopVibe
          </a>
          <div class="nav-search-wrapper">
            <div class="product-autocomplete">
              <input class="nav-search" id="navSearchInput" placeholder="Search products..." autocomplete="off" />
              <div class="autocomplete-dropdown" id="searchAutocomplete"></div>
            </div>
          </div>
          <nav class="nav-actions">
            <a href="/cart.html" class="nav-cart-btn" title="Cart" aria-label="Cart">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
              <span class="nav-cart-badge" id="cartBadge" style="display:inline-flex;">0</span>
            </a>
            <button id="themeToggle" class="btn btn-ghost btn-sm" aria-label="Toggle Theme">${getTheme() === "dark" ? "Light" : "Dark"}</button>
            <a href="/login.html" class="btn btn-ghost btn-sm">Sign In</a>
            <a href="/signup.html" class="btn btn-primary btn-sm">Sign Up</a>
          </nav>
        </div>
      </header>
      <div class="mobile-search-area">
        <div class="product-autocomplete">
          <input class="nav-search" id="mobileNavSearchInput" placeholder="Search products..." autocomplete="off" />
          <div class="autocomplete-dropdown" id="mobileSearchAutocomplete"></div>
        </div>
      </div>
      <nav class="mobile-bottom-nav">
        <a href="/home.html" class="mobile-bottom-nav-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          <span>Home</span>
        </a>
        <a href="/login.html" class="mobile-bottom-nav-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>
          <span>Sign In</span>
        </a>
        <a href="/signup.html" class="mobile-bottom-nav-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
          <span>Sign Up</span>
        </a>
      </nav>
      <div class="site-credit">Deva S</div>`;
    wireThemeToggle();
    wireGlobalSearch();
    renderSubNav();
    initMobileSearch();
    return;
  }

  if (isAdminRoute) {
    container.innerHTML = `
      <header class="header admin-header">
        <div class="header-inner">
          <div style="display:flex; align-items:center; gap:12px;">
            <a href="/admin/dashboard.html" class="nav-brand">
              <span class="nav-brand-logo-badge">${logoSvg}</span>
              ShopVibe Admin
            </a>
          </div>
          <nav class="nav-actions">
            <a href="/home.html" class="btn btn-outline btn-sm admin-desktop-only">&larr; Back to Store</a>
            <div class="nav-user">
              <span class="nav-user-avatar" id="mobileProfileTrigger" style="background:var(--purple); color:white; cursor:pointer;" title="Account menu">${cachedInitial}</span>
              <span class="nav-user-email admin-desktop-only" id="navUserEmail">${cachedEmail}</span>
            </div>
            <button id="logoutBtn" class="btn btn-ghost btn-sm admin-desktop-only">Sign Out</button>
            <button id="themeToggle" class="btn btn-ghost btn-sm admin-desktop-only" aria-label="Toggle Theme">${getTheme() === "dark" ? "Light" : "Dark"}</button>
          </nav>
        </div>
      </header>
      <div class="mobile-profile-dropdown" id="mobileProfileDropdown">
        <a href="/home.html" class="mobile-profile-dropdown-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          Back to Store
        </a>
        <button id="mobileThemeBtn" class="mobile-profile-dropdown-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
          Toggle Theme
        </button>
        <button id="mobileLogoutBtn" class="mobile-profile-dropdown-item" style="color:var(--danger);">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          Sign Out
        </button>
      </div>
      <div class="site-credit">Deva S</div>`;
    wireThemeToggle();
    wireLogout();
    wireMobileProfile();
    const token = await getAccessToken();
    if (!token) return;
    let email = "";
    try { const a = await getCachedUserAttributes(); email = a.email || ""; } catch {}
    const initial = email ? email.charAt(0).toUpperCase() : "?";
    const avatar = container.querySelector("#mobileProfileTrigger");
    if (avatar) avatar.textContent = initial;
    const emailEl = container.querySelector("#navUserEmail");
    if (emailEl) emailEl.textContent = email;
    return;
  }

  container.innerHTML = `
      <header class="header">
        <div class="header-inner">
          <a href="/home.html" class="nav-brand">
            <span class="nav-brand-logo-badge">${logoSvg}</span>
            ShopVibe
          </a>
          <ul class="nav-links">
            <li><a href="/home.html" ${path === "/home.html" ? 'class="active"' : ""}>Home</a></li>
            <li><a href="/orders.html" ${path === "/orders.html" ? 'class="active"' : ""}>Orders</a></li>
          </ul>
          <div class="nav-search-wrapper">
            <div class="product-autocomplete">
              <input class="nav-search" id="navSearchInput" placeholder="Search products..." autocomplete="off" />
              <div class="autocomplete-dropdown" id="searchAutocomplete"></div>
            </div>
          </div>
          <nav class="nav-actions">
            <a href="/cart.html" class="nav-cart-btn" title="Cart" aria-label="Cart">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
              <span class="nav-cart-badge" id="cartBadge" style="display:inline-flex;">0</span>
            </a>
            <div class="nav-user-dropdown-wrapper">
              <button class="nav-user-avatar-btn" id="mobileProfileTrigger" title="Account Menu">
                <span class="nav-user-avatar" id="navUserAvatarCircle">${cachedInitial}</span>
                <span class="nav-user-email" id="navUserEmail">${cachedEmail}</span>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
              </button>
              <div class="nav-user-dropdown-menu" id="mobileProfileDropdown">
                <div class="dropdown-user-header">
                  <div class="dropdown-user-name" id="dropdownUserEmail">${cachedEmail || 'My Account'}</div>
                  <div class="dropdown-user-label">Account Menu</div>
                </div>
                <a href="/profile.html" class="dropdown-item">
                  <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  <span>My Profile</span>
                </a>
                <button id="logoutBtn" class="dropdown-item logout-item" style="width:100%; border:none; background:none; text-align:left; cursor:pointer;">
                  <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                  <span>Sign Out</span>
                </button>
              </div>
            </div>
            <button id="themeToggle" class="btn btn-ghost btn-sm" aria-label="Toggle Theme">${getTheme() === "dark" ? "Light" : "Dark"}</button>
          </nav>
        </div>
      </header>
      <div class="mobile-search-area">
        <div class="product-autocomplete">
          <input class="nav-search" id="mobileNavSearchInput" placeholder="Search products..." autocomplete="off" />
          <div class="autocomplete-dropdown" id="mobileSearchAutocomplete"></div>
        </div>
      </div>
      <nav class="mobile-bottom-nav">
        <a href="/home.html" class="mobile-bottom-nav-item${path === "/home.html" ? " active" : ""}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          <span>Home</span>
        </a>
        <a href="/orders.html" class="mobile-bottom-nav-item${path === "/orders.html" ? " active" : ""}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
          <span>Orders</span>
        </a>
        <a href="/cart.html" class="mobile-bottom-nav-item${path === "/cart.html" ? " active" : ""}">
          <div style="position:relative; display:inline-flex; align-items:center; justify-content:center;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
            <span class="nav-cart-badge" id="mobileCartBadge" style="display:inline-flex;">0</span>
          </div>
          <span>Cart</span>
        </a>
      </nav>
      <div class="site-credit">Deva S</div>`;

  wireThemeToggle();
  wireLogout();
  wireMobileProfile();
  wireGlobalSearch();
  renderSubNav();
  initMobileSearch();

  const cachedCount = parseInt(localStorage.getItem("cache:cart:count")) || 0;
  updateCartBadge(cachedCount);
  api.get("/cart").then((res) => {
    const items = res?.data || [];
    const totalQty = Array.isArray(items) ? items.reduce((sum, i) => sum + (parseInt(i.quantity) || 1), 0) : 0;
    updateCartBadge(totalQty);
  }).catch(() => {});

  const token = await getAccessToken();
  if (!token) return;

  let email = "";
  try {
    const attrs = await getCachedUserAttributes();
    email = attrs.email || "";
  } catch (err) {
    console.error("Could not fetch user attributes:", err);
  }
  const initial = email ? email.charAt(0).toUpperCase() : "?";
  const shortName = email ? (email.split("@")[0].split(".")[0] || email) : "Account";
  const displayName = shortName.charAt(0).toUpperCase() + shortName.slice(1);

  cacheEmail(email);

  const avatar = container.querySelector("#navUserAvatarCircle");
  if (avatar) avatar.textContent = initial;
  const emailEl = container.querySelector("#navUserEmail");
  if (emailEl) emailEl.textContent = displayName;
  const dropEmail = container.querySelector("#dropdownUserEmail");
  if (dropEmail) dropEmail.textContent = email;
}

function wireLogout() {
  const handler = async () => {
    await doSignOut();
    window.location.href = "/login.html";
  };
  document.getElementById("logoutBtn")?.addEventListener("click", handler);
  document.getElementById("mobileLogoutBtn")?.addEventListener("click", handler);
}

function wireThemeToggle() {
  const handler = () => {
    const next = toggleTheme();
    const btn = document.getElementById("themeToggle");
    if (btn) btn.textContent = next === "dark" ? "Light" : "Dark";
  };
  document.getElementById("themeToggle")?.addEventListener("click", handler);
  document.getElementById("mobileThemeBtn")?.addEventListener("click", handler);
}

function wireMobileProfile() {
  const profileTrigger = document.getElementById("mobileProfileTrigger");
  const profileDropdown = document.getElementById("mobileProfileDropdown");
  if (profileTrigger && profileDropdown) {
    profileTrigger.addEventListener("click", (e) => {
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

function debounce(fn, ms) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), ms);
  };
}

let _sharedProducts = null;
async function fetchProductsForAutocomplete() {
  if (_sharedProducts) return _sharedProducts;
  try {
    const res = await getProductsCached();
    _sharedProducts = (res.data || []).filter(Boolean);
    return _sharedProducts;
  } catch {
    return [];
  }
}

function renderAutocomplete(items, inputEl, dropdown) {
  if (!items.length) {
    dropdown.innerHTML = '<div class="autocomplete-empty">No products found</div>';
    return;
  }
  dropdown.innerHTML = items.slice(0, 8).map((p, i) => `
    <div class="autocomplete-item" data-index="${i}" data-id="${p.productId}">
      <img src="${p.imageUrl || '/placeholder.svg'}" alt="${p.name || ''}" onerror="this.src='/placeholder.svg'" />
      <div class="autocomplete-item-info">
        <div class="autocomplete-item-name">${p.name || ''}</div>
        <div class="autocomplete-item-id">₹${p.price || 0}</div>
      </div>
    </div>
  `).join("");
}

function navigateToProduct(productId) {
  if (productId) {
    window.location.href = `/product.html?id=${encodeURIComponent(productId)}`;
  }
}

function initMobileSearch() {
  const mobileInput = document.getElementById("mobileNavSearchInput");
  const mobileDropdown = document.getElementById("mobileSearchAutocomplete");
  if (!mobileInput || !mobileDropdown) return;

  let products = [];
  let highlightedIndex = -1;

  fetchProductsForAutocomplete().then((p) => { products = p; });

  const closeDropdown = () => {
    mobileDropdown.classList.remove("open");
    highlightedIndex = -1;
  };

  const doSearch = () => {
    const q = mobileInput.value.trim();
    if (q) {
      closeDropdown();
      window.location.href = `/home.html?q=${encodeURIComponent(q)}`;
    }
  };

  const handleInput = debounce(() => {
    const q = mobileInput.value.trim().toLowerCase();
    if (!q || !products.length) {
      closeDropdown();
      return;
    }
    const matches = products.filter(
      (p) =>
        (p.name || "").toLowerCase().includes(q) ||
        (p.category || "").toLowerCase().includes(q)
    );
    if (!matches.length) {
      mobileDropdown.innerHTML = '<div class="autocomplete-empty">No products found</div>';
      mobileDropdown.classList.add("open");
      highlightedIndex = -1;
      return;
    }
    renderAutocomplete(matches, mobileInput, mobileDropdown);
    highlightedIndex = -1;
    mobileDropdown.classList.add("open");
  }, 200);

  mobileInput.addEventListener("input", handleInput);

  mobileInput.addEventListener("keydown", (e) => {
    const items = mobileDropdown.querySelectorAll(".autocomplete-item");
    if (e.key === "Enter") {
      if (highlightedIndex >= 0 && items[highlightedIndex]) {
        e.preventDefault();
        const id = items[highlightedIndex].dataset.id;
        closeDropdown();
        navigateToProduct(id);
      } else {
        doSearch();
      }
      return;
    }
    if (!mobileDropdown.classList.contains("open") || !items.length) return;

    if (e.key === "ArrowDown") {
      e.preventDefault();
      highlightedIndex = Math.min(highlightedIndex + 1, items.length - 1);
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      highlightedIndex = Math.max(highlightedIndex - 1, 0);
    } else if (e.key === "Escape") {
      closeDropdown();
      mobileInput.blur();
      return;
    } else {
      return;
    }

    items.forEach((el, i) => {
      el.classList.toggle("highlighted", i === highlightedIndex);
    });
    if (highlightedIndex >= 0 && items[highlightedIndex]) {
      items[highlightedIndex].scrollIntoView({ block: "nearest" });
    }
  });

  mobileDropdown.addEventListener("mousedown", (e) => {
    const item = e.target.closest(".autocomplete-item");
    if (item) {
      e.preventDefault();
      const id = item.dataset.id;
      closeDropdown();
      navigateToProduct(id);
    }
  });

  document.addEventListener("click", (e) => {
    if (!e.target.closest("#mobileSearchAutocomplete, #mobileNavSearchInput")) {
      closeDropdown();
    }
  });
}

function wireGlobalSearch() {
  const input = document.getElementById("navSearchInput");
  const dropdown = document.getElementById("searchAutocomplete");
  if (!input || !dropdown) return;

  const isHome = window.location.pathname === "/home.html";

  if (isHome) {
    input.value = new URLSearchParams(window.location.search).get("q") || "";
    input.addEventListener("input", () => {
      const q = input.value.trim();
      const url = q ? `/home.html?q=${encodeURIComponent(q)}` : "/home.html";
      window.history.replaceState(null, "", url);
      window.dispatchEvent(new CustomEvent("search", { detail: q }));
    });
  } else {
    input.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && !dropdown.classList.contains("open")) {
        const q = input.value.trim();
        if (q) {
          window.location.href = `/home.html?q=${encodeURIComponent(q)}`;
        }
      }
    });
  }

  let products = [];
  let highlightedIndex = -1;

  fetchProductsForAutocomplete().then((p) => { products = p; });

  const closeDropdown = () => {
    dropdown.classList.remove("open");
    highlightedIndex = -1;
  };

  const handleInput = debounce(() => {
    const q = input.value.trim().toLowerCase();
    if (!q || !products.length) {
      closeDropdown();
      return;
    }
    const matches = products.filter(
      (p) =>
        (p.name || "").toLowerCase().includes(q) ||
        (p.category || "").toLowerCase().includes(q)
    );
    if (!matches.length) {
      dropdown.innerHTML = '<div class="autocomplete-empty">No products found</div>';
      dropdown.classList.add("open");
      highlightedIndex = -1;
      return;
    }
    renderAutocomplete(matches, input, dropdown);
    highlightedIndex = -1;
    dropdown.classList.add("open");
  }, 200);

  input.addEventListener("input", handleInput);

  input.addEventListener("keydown", (e) => {
    const items = dropdown.querySelectorAll(".autocomplete-item");
    const dropdownOpen = dropdown.classList.contains("open");

    if (e.key === "Escape") {
      closeDropdown();
      input.blur();
      return;
    }

    if (e.key === "Enter") {
      e.preventDefault();

      // If an item is highlighted in the dropdown, navigate to it
      if (dropdownOpen && highlightedIndex >= 0 && items[highlightedIndex]) {
        const id = items[highlightedIndex].dataset.id;
        navigateToProduct(id);
        return;
      }

      // Otherwise: close dropdown, search by query
      closeDropdown();
      input.blur();

      const q = input.value.trim();
      if (!q) return;

      if (isHome) {
        // On home page: update URL and fire search event
        const url = `/home.html?q=${encodeURIComponent(q)}`;
        window.history.replaceState(null, "", url);
        window.dispatchEvent(new CustomEvent("search", { detail: q }));
      } else {
        // On other pages: navigate to home with query
        window.location.href = `/home.html?q=${encodeURIComponent(q)}`;
      }
      return;
    }

    if (!dropdownOpen || !items.length) return;

    if (e.key === "ArrowDown") {
      e.preventDefault();
      highlightedIndex = Math.min(highlightedIndex + 1, items.length - 1);
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      highlightedIndex = Math.max(highlightedIndex - 1, 0);
    } else if (e.key === "Tab") {
      if (highlightedIndex >= 0 && items[highlightedIndex]) {
        e.preventDefault();
        const id = items[highlightedIndex].dataset.id;
        navigateToProduct(id);
      }
      return;
    } else {
      return;
    }

    items.forEach((el, i) => {
      el.classList.toggle("highlighted", i === highlightedIndex);
    });
    if (highlightedIndex >= 0 && items[highlightedIndex]) {
      items[highlightedIndex].scrollIntoView({ block: "nearest" });
    }
  });

  dropdown.addEventListener("mousedown", (e) => {
    const item = e.target.closest(".autocomplete-item");
    if (item) {
      e.preventDefault();
      const id = item.dataset.id;
      navigateToProduct(id);
    }
  });

  document.addEventListener("click", (e) => {
    if (!e.target.closest(".product-autocomplete")) {
      closeDropdown();
    }
  });
}

async function renderSubNav() {
  const existing = document.getElementById("subNav");
  if (existing) existing.remove();
  document.body.classList.remove("has-subnav");
}

export function updateCartBadge(count) {
  const badge = document.getElementById("cartBadge");
  const mobileBadge = document.getElementById("mobileCartBadge");

  let num = parseInt(count);
  if (isNaN(num)) {
    num = parseInt(localStorage.getItem("cache:cart:count")) || 0;
  } else {
    try { localStorage.setItem("cache:cart:count", String(num)); } catch {}
  }

  [badge, mobileBadge].forEach((b) => {
    if (b) {
      if (num > 0) {
        b.textContent = num > 99 ? "99+" : num;
        b.style.display = "inline-flex";
      } else {
        b.style.display = "none";
      }
    }
  });
}