import { requireAuth, getCachedUserAttributes, doSignOut, doChangePassword } from "./auth.js";
import { renderNavbar } from "../components/navbar.js";
import { showToast } from "../components/toast.js";
import { renderFooter } from "../components/footer.js";
import api from "./api.js";

const contentEl = document.getElementById("profileContent");
const msg = document.getElementById("msg");

// Skeleton Loader for V2 Profile
const skProfile = `
  <div class="profile-v2-container">
    <div class="skeleton" style="height: 140px; border-radius: var(--radius-xl); margin-bottom: 28px;"></div>
    <div class="profile-v2-grid">
      <div class="skeleton" style="height: 280px; border-radius: var(--radius-xl);"></div>
      <div>
        <div class="skeleton" style="height: 260px; border-radius: var(--radius-xl); margin-bottom: 24px;"></div>
        <div class="skeleton" style="height: 120px; border-radius: var(--radius-xl);"></div>
      </div>
    </div>
  </div>
`;

contentEl.innerHTML = skProfile;
contentEl.style.display = "block";

async function init() {
  const authPromise = requireAuth();
  const navbarPromise = renderNavbar("navbar");
  const ordersPromise = api.get("/orders").catch(() => ({ data: [] }));

  await authPromise;
  await navbarPromise;

  const [attrsRes, ordersRes] = await Promise.all([
    getCachedUserAttributes().catch(() => ({ failed: true })),
    ordersPromise
  ]);

  if (attrsRes.failed) {
    contentEl.style.display = "none";
    msg.textContent = "Failed to load profile details.";
    return;
  }

  contentEl.style.display = "block";
  const email = attrsRes.email || "N/A";
  const savedName = localStorage.getItem("user_profile_name") || attrsRes.name || (email !== "N/A" ? email.split("@")[0] : "Customer");
  const savedPhone = localStorage.getItem("user_profile_phone") || attrsRes.phone_number || "";
  const initial = savedName.split(" ").map(n => n[0]).join("").substring(0, 2).toUpperCase() || "US";

  const orders = ordersRes.data || [];
  const totalOrders = orders.length;
  const totalSpent = orders.reduce((sum, o) => sum + (o.totalAmount || 0), 0);
  const wishlist = JSON.parse(localStorage.getItem("wishlist") || "[]");

  contentEl.classList.add("content-loaded");
  contentEl.innerHTML = `
    <div class="profile-v2-container">
      <!-- Hero Top Banner -->
      <div class="profile-hero-banner">
        <div class="profile-hero-user">
          <div class="profile-hero-avatar-wrapper">
            <div class="profile-hero-avatar" id="heroAvatar">${initial}</div>
            <div class="online-indicator-dot" title="Active Session"></div>
          </div>
          <div class="profile-hero-info">
            <h1 id="heroName">${savedName}</h1>
            <div class="profile-hero-meta">
              <span class="badge-vip">
                <svg width="14" height="14" fill="currentColor" viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/></svg>
                Verified Account
              </span>
              <span style="font-size:13px; color:var(--text-muted);">${email}</span>
            </div>
          </div>
        </div>

        <div class="profile-hero-stats">
          <div class="hero-stat-box">
            <div class="hero-stat-value">${totalOrders}</div>
            <div class="hero-stat-label">Orders</div>
          </div>
          <div class="hero-stat-box">
            <div class="hero-stat-value">₹${Math.round(totalSpent).toLocaleString("en-IN")}</div>
            <div class="hero-stat-label">Spent</div>
          </div>
        </div>
      </div>

      <!-- 2-Column Grid -->
      <div class="profile-v2-grid">
        <!-- Sidebar Navigation -->
        <aside class="profile-v2-nav">
          <nav class="profile-v2-nav-card">
            <button class="profile-v2-nav-item active" data-tab="overview">
              <div class="nav-item-left">
                <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7" rx="1.5"></rect><rect x="14" y="3" width="7" height="7" rx="1.5"></rect><rect x="14" y="14" width="7" height="7" rx="1.5"></rect><rect x="3" y="14" width="7" height="7" rx="1.5"></rect></svg>
                <span>Dashboard</span>
              </div>
            </button>

            <button class="profile-v2-nav-item" data-tab="orders">
              <div class="nav-item-left">
                <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path></svg>
                <span>My Orders</span>
              </div>
              <span class="nav-pill-badge">${totalOrders}</span>
            </button>

            <button class="profile-v2-nav-item" data-tab="wishlist">
              <div class="nav-item-left">
                <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
                <span>Wishlist</span>
              </div>
              <span class="nav-pill-badge">${wishlist.length}</span>
            </button>

            <button class="profile-v2-nav-item" data-tab="settings">
              <div class="nav-item-left">
                <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
                <span>Settings</span>
              </div>
            </button>

            <button class="profile-v2-nav-item logout-v2" id="logoutBtn">
              <div class="nav-item-left">
                <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                <span>Log out</span>
              </div>
            </button>
          </nav>
        </aside>

        <!-- Main Workspace Panes -->
        <main class="profile-v2-main">
          <!-- TAB 1: Overview -->
          <div class="tab-pane active" id="tab-overview">
            <div class="profile-v2-card">
              <div class="v2-card-header">
                <div class="v2-card-title-group">
                  <div class="v2-icon-box v2-icon-cyan">
                    <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>
                  </div>
                  <div>
                    <h3 class="v2-card-title">Recent Activity</h3>
                    <p class="v2-card-subtitle">Your latest orders and order status updates</p>
                  </div>
                </div>
                <button class="btn btn-ghost" id="viewAllOrdersLink" style="color: #06b6d4; font-weight: 700; font-size: 14px;">View All Orders ➔</button>
              </div>

              <div id="overviewOrdersContainer">
                ${renderOverviewOrders(orders)}
              </div>
            </div>

            <!-- Quick Action Interactive Tiles Grid -->
            <div class="profile-v2-quick-grid">
              <div class="quick-tile-v2" id="quickWishlistTile">
                <div class="tile-info">
                  <div class="v2-icon-box" style="background: rgba(236,72,153,0.12); color:#ec4899;">
                    <svg width="22" height="22" fill="currentColor" viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
                  </div>
                  <div>
                    <h4 style="font-size:15px; font-weight:700; margin:0 0 2px 0;">My Saved Wishlist</h4>
                    <p style="font-size:13px; color:var(--text-muted); margin:0;">${wishlist.length} item(s) saved for later</p>
                  </div>
                </div>
                <div class="tile-arrow">➔</div>
              </div>

              <div class="quick-tile-v2" id="quickSettingsTile">
                <div class="tile-info">
                  <div class="v2-icon-box v2-icon-indigo">
                    <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
                  </div>
                  <div>
                    <h4 style="font-size:15px; font-weight:700; margin:0 0 2px 0;">Security & Details</h4>
                    <p style="font-size:13px; color:var(--text-muted); margin:0;">Update name, phone & password</p>
                  </div>
                </div>
                <div class="tile-arrow">➔</div>
              </div>
            </div>
          </div>

          <!-- TAB 2: My Orders -->
          <div class="tab-pane" id="tab-orders" style="display:none;">
            <div class="profile-v2-card">
              <div class="v2-card-header">
                <div class="v2-card-title-group">
                  <div class="v2-icon-box v2-icon-cyan">
                    <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path></svg>
                  </div>
                  <div>
                    <h3 class="v2-card-title">Order History</h3>
                    <p class="v2-card-subtitle">All your past orders and current processing status</p>
                  </div>
                </div>
              </div>
              <div>
                ${renderFullOrders(orders)}
              </div>
            </div>
          </div>

          <!-- TAB 3: Wishlist -->
          <div class="tab-pane" id="tab-wishlist" style="display:none;">
            <div class="profile-v2-card">
              <div class="v2-card-header">
                <div class="v2-card-title-group">
                  <div class="v2-icon-box" style="background: rgba(236,72,153,0.12); color:#ec4899;">
                    <svg width="22" height="22" fill="currentColor" viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
                  </div>
                  <div>
                    <h3 class="v2-card-title">Saved Items</h3>
                    <p class="v2-card-subtitle">Products saved to your wishlist</p>
                  </div>
                </div>
              </div>
              <div>
                ${renderWishlistItems(wishlist)}
              </div>
            </div>
          </div>

          <!-- TAB 4: Settings -->
          <div class="tab-pane" id="tab-settings" style="display:none;">
            <!-- Profile Information Form Card -->
            <div class="profile-v2-card">
              <div class="v2-card-header">
                <div class="v2-card-title-group">
                  <div class="v2-icon-box v2-icon-indigo">
                    <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                  </div>
                  <div>
                    <h3 class="v2-card-title">Personal Details</h3>
                    <p class="v2-card-subtitle">Update your display name and contact information</p>
                  </div>
                </div>
              </div>

              <form id="profileInfoForm" style="display:flex; flex-direction:column; gap:22px;">
                <div class="form-grid-2">
                  <div class="form-group">
                    <label class="form-label" for="profileFullName">Full Name</label>
                    <div class="input-wrapper-v2">
                      <input type="text" id="profileFullName" class="form-input-v2" value="${savedName}" required />
                      <svg class="input-icon-prefix" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="form-label" for="profileEmail">Email Address</label>
                    <div class="input-wrapper-v2">
                      <input type="email" id="profileEmail" class="form-input-v2" value="${email}" readonly />
                      <svg class="input-icon-prefix" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg>
                    </div>
                    <span class="field-caption">
                      <svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                      Email address is locked to your Cognito user account
                    </span>
                  </div>
                </div>

                <div class="form-group">
                  <label class="form-label" for="profilePhone">Phone Number</label>
                  <div class="input-wrapper-v2">
                    <input type="tel" id="profilePhone" class="form-input-v2" placeholder="+91 9876543210" value="${savedPhone}" />
                    <svg class="input-icon-prefix" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg>
                  </div>
                </div>

                <div style="display:flex; justify-content:flex-end; margin-top:6px;">
                  <button type="submit" class="btn-gradient-v2">
                    <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path><polyline points="17 21 17 13 7 13 7 21"></polyline><polyline points="7 3 7 8 15 8"></polyline></svg>
                    <span>Save Account Changes</span>
                  </button>
                </div>
              </form>
            </div>

            <!-- Password & Security Card -->
            <div class="profile-v2-card">
              <div class="v2-card-header">
                <div class="v2-card-title-group">
                  <div class="v2-icon-box v2-icon-danger">
                    <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                  </div>
                  <div>
                    <h3 class="v2-card-title">Account Password</h3>
                    <p class="v2-card-subtitle">Ensure your account uses a strong, random password</p>
                  </div>
                </div>
              </div>

              <form id="changePasswordForm" style="display:flex; flex-direction:column; gap:18px;">
                <div class="form-group">
                  <label class="form-label" for="currentPassword">Current Password</label>
                  <div class="input-wrapper-v2">
                    <input type="password" id="currentPassword" class="form-input-v2" placeholder="••••••••" required />
                    <svg class="input-icon-prefix" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                  </div>
                </div>

                <div class="form-grid-2">
                  <div class="form-group">
                    <label class="form-label" for="newPassword">New Password</label>
                    <div class="input-wrapper-v2">
                      <input type="password" id="newPassword" class="form-input-v2" placeholder="••••••••" required minlength="8" />
                      <svg class="input-icon-prefix" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.778-7.778zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"></path></svg>
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="form-label" for="confirmNewPassword">Confirm Password</label>
                    <div class="input-wrapper-v2">
                      <input type="password" id="confirmNewPassword" class="form-input-v2" placeholder="••••••••" required minlength="8" />
                      <svg class="input-icon-prefix" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                    </div>
                  </div>
                </div>

                <div style="display:flex; justify-content:flex-end; margin-top:6px;">
                  <button type="submit" class="btn-gradient-v2" id="updatePasswordBtn">
                    <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                    <span>Update Security Password</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        </main>
      </div>
    </div>
  `;

  bindEvents();
}

function renderOverviewOrders(orders) {
  if (!orders || orders.length === 0) {
    return `
      <div class="recent-orders-empty">
        <div class="recent-orders-empty-icon">
          <svg width="28" height="28" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path></svg>
        </div>
        <h4 style="font-size: 16px; font-weight: 700; margin:0 0 4px 0;">No Order Activity Yet</h4>
        <p style="font-size: 13px; color: var(--text-muted); margin:0 0 20px 0;">Explore our collection and place your first order.</p>
        <a href="/home.html" class="btn-gradient-v2" style="text-decoration:none;">Explore Store</a>
      </div>
    `;
  }

  const recent = orders.slice(0, 3);
  return `
    <div style="display:flex; flex-direction:column; gap:14px;">
      ${recent.map(o => `
        <div style="background: var(--surface-hover); border: 1px solid var(--border); border-radius: var(--radius); padding: 18px 20px; display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; transition:border-color 0.2s;">
          <div>
            <div style="font-weight: 800; font-size: 15px; letter-spacing:-0.01em;">Order #${o.orderId.substring(0, 8)}</div>
            <div style="font-size: 13px; color: var(--text-muted); margin-top:2px;">${o.createdAt ? new Date(o.createdAt).toLocaleDateString("en-IN", { month: "short", day: "numeric", year: "numeric" }) : 'Recent'} • ${o.items?.length || 0} item(s)</div>
          </div>
          <div style="display:flex; align-items:center; gap:16px;">
            <span class="badge ${o.status === 'COMPLETED' || o.status === 'PAID' ? 'badge-confirmed' : 'badge-pending'}">${o.status}</span>
            <span style="font-weight:800; font-size:16px; color: var(--text);">₹${Math.round(o.totalAmount || 0).toLocaleString("en-IN")}</span>
          </div>
        </div>
      `).join('')}
    </div>
  `;
}

function renderFullOrders(orders) {
  if (!orders || orders.length === 0) {
    return `
      <div style="text-align:center; padding: 40px 20px; color:var(--text-muted);">
        <p>No orders found in your account history.</p>
      </div>
    `;
  }
  return `
    <div style="display:flex; flex-direction:column; gap:16px;">
      ${orders.map(o => `
        <div style="background: var(--surface-hover); border: 1px solid var(--border); border-radius: var(--radius); padding: 20px;">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:14px; flex-wrap:wrap; gap:8px;">
            <div>
              <span style="font-weight:800; font-size:15px; letter-spacing:-0.01em;">Order #${o.orderId}</span>
              <div style="font-size:12px; color:var(--text-muted); margin-top:2px;">Placed on ${o.createdAt ? new Date(o.createdAt).toLocaleString("en-IN") : 'N/A'}</div>
            </div>
            <span class="badge ${o.status === 'COMPLETED' || o.status === 'PAID' ? 'badge-confirmed' : 'badge-pending'}">${o.status}</span>
          </div>
          <div style="border-top:1px solid var(--border); padding-top:14px; display:flex; justify-content:space-between; align-items:center;">
            <div style="font-size:13px; color:var(--text-secondary); font-weight:600;">${o.items?.length || 0} product(s)</div>
            <div style="font-size:17px; font-weight:800; color:var(--text);">₹${Math.round(o.totalAmount || 0).toLocaleString("en-IN")}</div>
          </div>
        </div>
      `).join('')}
    </div>
  `;
}

function renderWishlistItems(wishlist) {
  if (!wishlist || wishlist.length === 0) {
    return `
      <div style="text-align:center; padding: 40px 20px; color:var(--text-muted);">
        <p>Your wishlist is currently empty.</p>
        <a href="/home.html" class="btn-gradient-v2" style="margin-top:12px; text-decoration:none;">Discover Products</a>
      </div>
    `;
  }
  return `
    <div style="display:grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap:18px;">
      ${wishlist.map(item => `
        <div style="background:var(--surface-hover); border:1px solid var(--border); border-radius:var(--radius); padding:18px; text-align:center;">
          <div style="font-weight:700; font-size:15px; margin-bottom:6px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${item.name || 'Saved Product'}</div>
          <div style="color:#06b6d4; font-size:16px; font-weight:800; margin-bottom:14px;">₹${item.price || 0}</div>
          <a href="/product.html?id=${item.id}" class="btn-gradient-v2" style="font-size:12px; padding:8px 16px; text-decoration:none; width:100%; justify-content:center;">View Details</a>
        </div>
      `).join('')}
    </div>
  `;
}

function bindEvents() {
  const navBtns = document.querySelectorAll(".profile-v2-nav-item[data-tab]");
  const tabPanes = {
    overview: document.getElementById("tab-overview"),
    orders: document.getElementById("tab-orders"),
    wishlist: document.getElementById("tab-wishlist"),
    settings: document.getElementById("tab-settings")
  };

  function switchTab(tabName) {
    navBtns.forEach(btn => {
      if (btn.getAttribute("data-tab") === tabName) btn.classList.add("active");
      else btn.classList.remove("active");
    });
    Object.keys(tabPanes).forEach(key => {
      if (key === tabName) {
        tabPanes[key].style.display = "block";
      } else {
        tabPanes[key].style.display = "none";
      }
    });
  }

  navBtns.forEach(btn => {
    btn.addEventListener("click", () => {
      const tab = btn.getAttribute("data-tab");
      switchTab(tab);
    });
  });

  // Quick Action Tiles Navigation
  document.getElementById("quickWishlistTile")?.addEventListener("click", () => switchTab("wishlist"));
  document.getElementById("quickSettingsTile")?.addEventListener("click", () => switchTab("settings"));
  document.getElementById("viewAllOrdersLink")?.addEventListener("click", () => switchTab("orders"));

  // Logout Button
  document.getElementById("logoutBtn")?.addEventListener("click", async () => {
    await doSignOut();
    showToast("Logged out successfully", "info");
    setTimeout(() => { window.location.href = "/login.html"; }, 500);
  });

  // Profile Information Form Submit
  const profileForm = document.getElementById("profileInfoForm");
  if (profileForm) {
    profileForm.addEventListener("submit", (e) => {
      e.preventDefault();
      const newName = document.getElementById("profileFullName").value.trim();
      const newPhone = document.getElementById("profilePhone").value.trim();

      if (newName) {
        localStorage.setItem("user_profile_name", newName);
        document.getElementById("heroName").textContent = newName;
        const initial = newName.split(" ").map(n => n[0]).join("").substring(0, 2).toUpperCase();
        document.getElementById("heroAvatar").textContent = initial;
      }
      if (newPhone) {
        localStorage.setItem("user_profile_phone", newPhone);
      }

      showToast("Personal details updated successfully!", "success");
    });
  }

  // Change Password Form Submit
  const passwordForm = document.getElementById("changePasswordForm");
  if (passwordForm) {
    passwordForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const currPass = document.getElementById("currentPassword").value;
      const newPass = document.getElementById("newPassword").value;
      const confirmPass = document.getElementById("confirmNewPassword").value;

      if (newPass !== confirmPass) {
        showToast("New passwords do not match!", "error");
        return;
      }

      if (newPass.length < 8) {
        showToast("Password must be at least 8 characters long.", "error");
        return;
      }

      const updateBtn = document.getElementById("updatePasswordBtn");
      updateBtn.disabled = true;
      updateBtn.innerHTML = `<span>Updating...</span>`;

      const res = await doChangePassword(currPass, newPass);

      updateBtn.disabled = false;
      updateBtn.innerHTML = `
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
        <span>Update Security Password</span>
      `;

      if (res.success) {
        showToast("Password changed successfully!", "success");
        passwordForm.reset();
      } else {
        showToast(res.error || "Failed to change password.", "error");
      }
    });
  }
}

init();
renderFooter();
