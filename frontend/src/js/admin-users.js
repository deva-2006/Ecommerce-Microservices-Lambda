import "./admin-common.js";
import api, { getProductsCached } from "./api.js";
import { showToast } from "../components/toast.js";
import { renderFooter } from "../components/footer.js";

const listEl = document.getElementById("usersList");
const msg = document.getElementById("msg");
const searchInput = document.getElementById("searchInput");

let allUsers = [];
let productCatalogMap = {};

function formatCurrency(num) {
  return `₹${Math.round(num || 0).toLocaleString("en-IN")}`;
}

function renderUsers(users) {
  if (!users || users.length === 0) {
    listEl.innerHTML = `
      <div class="empty-state" style="grid-column: 1 / -1; padding: 40px;">
        <div class="empty-state-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        </div>
        <h3 class="empty-state-title">No users found</h3>
        <p class="empty-state-desc">Customer profiles will appear here once orders are placed.</p>
      </div>`;
    return;
  }

  listEl.innerHTML = users.map((u) => {
    const initial = (u.name || u.email || "U").charAt(0).toUpperCase();
    return `
      <div class="admin-item-card user-card" data-user-id="${u.userId}" style="display:flex; flex-direction:column; justify-content:space-between; gap:16px;">
        <div>
          <div style="display:flex; align-items:center; gap:12px; margin-bottom:12px;">
            <div style="width:44px; height:44px; border-radius:50%; background:linear-gradient(135deg, var(--accent) 0%, #7c3aed 100%); color:#fff; font-weight:800; font-size:18px; display:flex; align-items:center; justify-content:center; box-shadow:0 2px 10px rgba(79, 70, 229, 0.3);">
              ${initial}
            </div>
            <div style="flex:1; min-width:0;">
              <div style="font-weight:700; font-size:15px; color:var(--text); text-overflow:ellipsis; overflow:hidden; white-space:nowrap;">${u.name || "Customer"}</div>
              <div style="font-size:13px; color:var(--accent); font-weight:600; text-overflow:ellipsis; overflow:hidden; white-space:nowrap;">${u.email}</div>
            </div>
          </div>

          <div style="font-size:12px; color:var(--text-muted); font-family:monospace; margin-bottom:12px; background:var(--surface-2); padding:6px 10px; border-radius:var(--radius-sm); word-break:break-all;">
            Sub: ${u.userId}
          </div>

          <div style="display:grid; grid-template-columns:1fr 1fr; gap:10px; background:var(--surface-2); padding:12px; border-radius:var(--radius); margin-bottom:8px;">
            <div>
              <div style="font-size:11px; color:var(--text-muted); text-transform:uppercase; font-weight:700;">Total Orders</div>
              <div style="font-size:16px; font-weight:800; color:var(--text); margin-top:2px;">${u.orderCount}</div>
            </div>
            <div>
              <div style="font-size:11px; color:var(--text-muted); text-transform:uppercase; font-weight:700;">Total Spent</div>
              <div style="font-size:16px; font-weight:800; color:var(--accent); margin-top:2px;">${formatCurrency(u.totalSpent)}</div>
            </div>
          </div>

          ${u.phone ? `<div style="font-size:12px; color:var(--text-secondary); margin-top:6px;">Phone: ${u.phone}</div>` : ""}
          <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">Last Order: ${new Date(u.lastActive).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}</div>
        </div>

        <button class="btn btn-outline btn-block view-user-details-btn" style="margin-top:8px;">View User Profile & Orders &rarr;</button>
      </div>`;
  }).join("");

  listEl.querySelectorAll(".view-user-details-btn").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      const card = e.target.closest(".user-card");
      const uid = card.dataset.userId;
      const user = allUsers.find((u) => u.userId === uid);
      if (user) showUserDetailModal(user);
    });
  });
}

function showUserDetailModal(u) {
  const existing = document.getElementById("adminUserDetailModal");
  if (existing) existing.remove();

  const ordersHtml = u.orders.map((o) => `
    <div style="background:var(--surface-2); border:1px solid var(--border-light); border-radius:var(--radius); padding:12px; margin-bottom:10px; font-size:13px;">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
        <span style="font-family:monospace; font-weight:700; font-size:12px;">Order ${o.orderId}</span>
        <span class="badge badge-${(o.status || 'pending').toLowerCase()}">${o.status}</span>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; color:var(--text-muted); font-size:12px; margin-bottom:6px;">
        <span>Date: ${new Date(o.createdAt).toLocaleString("en-IN")}</span>
        <span style="font-weight:700; color:var(--accent); font-size:14px;">${formatCurrency(o.totalAmount)}</span>
      </div>
      ${(o.items || []).length > 0 ? `
        <div style="font-size:11px; color:var(--text-secondary); margin-top:4px;">
          Items: ${(o.items || []).map(i => `${i.productName || i.name || 'Product'} (×${i.quantity})`).join(", ")}
        </div>` : ""}
    </div>
  `).join("");

  const modal = document.createElement("div");
  modal.id = "adminUserDetailModal";
  modal.className = "modal-overlay";
  modal.style.cssText = "position:fixed; inset:0; background:rgba(0,0,0,0.65); backdrop-filter:blur(6px); display:flex; align-items:center; justify-content:center; z-index:9999; padding:16px;";

  modal.innerHTML = `
    <div class="modal-box" style="background:var(--surface); border:1px solid var(--border); border-radius:var(--radius-lg); width:100%; max-width:600px; max-height:90vh; overflow-y:auto; padding:24px; box-shadow:var(--shadow-2xl); position:relative;">
      <button id="closeAdminUserModal" style="position:absolute; top:16px; right:16px; background:var(--surface-2); border:none; width:32px; height:32px; border-radius:50%; display:flex; align-items:center; justify-content:center; color:var(--text-secondary); cursor:pointer; font-size:18px;">&times;</button>
      
      <div style="display:flex; align-items:center; gap:14px; margin-bottom:20px;">
        <div style="width:52px; height:52px; border-radius:50%; background:linear-gradient(135deg, var(--accent) 0%, #7c3aed 100%); color:#fff; font-weight:800; font-size:22px; display:flex; align-items:center; justify-content:center;">
          ${(u.name || u.email || "U").charAt(0).toUpperCase()}
        </div>
        <div>
          <h3 style="font-size:18px; font-weight:800; margin:0;">${u.name || "Customer Profile"}</h3>
          <div style="font-size:14px; color:var(--accent); font-weight:600; margin-top:2px;">${u.email}</div>
        </div>
      </div>

      <div style="background:var(--surface-2); border-radius:var(--radius); padding:16px; margin-bottom:20px; font-size:13px; display:grid; grid-template-columns:1fr 1fr; gap:12px;">
        <div>
          <div style="color:var(--text-muted); font-size:11px; text-transform:uppercase; font-weight:700;">User ID Sub</div>
          <div style="font-family:monospace; font-size:11px; margin-top:2px; word-break:break-all;">${u.userId}</div>
        </div>
        <div>
          <div style="color:var(--text-muted); font-size:11px; text-transform:uppercase; font-weight:700;">Account Status</div>
          <div style="margin-top:2px;"><span class="badge badge-delivered">ACTIVE</span></div>
        </div>
        <div>
          <div style="color:var(--text-muted); font-size:11px; text-transform:uppercase; font-weight:700;">Total Lifetime Orders</div>
          <div style="font-size:16px; font-weight:800; margin-top:2px;">${u.orderCount}</div>
        </div>
        <div>
          <div style="color:var(--text-muted); font-size:11px; text-transform:uppercase; font-weight:700;">Total Lifetime Spend</div>
          <div style="font-size:16px; font-weight:800; color:var(--accent); margin-top:2px;">${formatCurrency(u.totalSpent)}</div>
        </div>
      </div>

      <div style="margin-bottom:20px;">
        <h4 style="font-size:14px; font-weight:700; margin-bottom:10px;">Order History (${u.orders.length})</h4>
        ${ordersHtml}
      </div>

      <div style="display:flex; justify-content:flex-end;">
        <button id="closeAdminUserModalBtn" class="btn btn-ghost">Close</button>
      </div>
    </div>`;

  document.body.appendChild(modal);

  const closeModal = () => modal.remove();
  modal.querySelector("#closeAdminUserModal").addEventListener("click", closeModal);
  modal.querySelector("#closeAdminUserModalBtn").addEventListener("click", closeModal);
  modal.addEventListener("click", (e) => {
    if (e.target === modal) closeModal();
  });
}

function filterUsers(query) {
  const q = query.toLowerCase().trim();
  if (!q) return allUsers;
  return allUsers.filter(
    (u) =>
      u.email.toLowerCase().includes(q) ||
      u.name.toLowerCase().includes(q) ||
      u.userId.toLowerCase().includes(q) ||
      (u.phone || "").toLowerCase().includes(q)
  );
}

searchInput?.addEventListener("input", () => {
  renderUsers(filterUsers(searchInput.value));
});

async function loadUsers() {
  listEl.innerHTML = `
    <div class="skeleton skeleton-row"></div>
    <div class="skeleton skeleton-row"></div>
    <div class="skeleton skeleton-row"></div>`;

  try {
    const [ordersRes, prodRes] = await Promise.all([
      api.get("/orders/all").catch(() => api.get("/orders")).catch(() => ({ data: [] })),
      getProductsCached().catch(() => ({ data: [] })),
    ]);

    (prodRes.data || []).forEach((p) => { if (p && p.productId) productCatalogMap[p.productId] = p; });

    const rawOrders = ordersRes.data;
    const orders = Array.isArray(rawOrders) ? rawOrders : (rawOrders?.orders || []);

    const userMap = new Map();
    const defaultEmail = localStorage.getItem("userEmail") || "deva.s.professional@gmail.com";

    orders.forEach((o) => {
      const uid = o.userId || "guest";
      const addr = o.shippingAddress || o.address || {};
      const email = o.email || o.userEmail || o.customerEmail || (typeof addr === "object" ? addr.email : null) || defaultEmail;
      const name = (typeof addr === "object" ? addr.name : null) || email.split("@")[0] || "Customer";
      const phone = (typeof addr === "object" ? addr.phone : null) || "";

      if (!userMap.has(uid)) {
        userMap.set(uid, {
          userId: uid,
          email,
          name,
          phone,
          orderCount: 0,
          totalSpent: 0,
          lastActive: o.createdAt || new Date().toISOString(),
          orders: [],
        });
      }

      const user = userMap.get(uid);
      user.orderCount += 1;
      user.totalSpent += parseFloat(o.totalAmount) || 0;
      user.orders.push(o);
      if (new Date(o.createdAt) > new Date(user.lastActive)) {
        user.lastActive = o.createdAt;
      }
    });

    allUsers = Array.from(userMap.values());
    allUsers.sort((a, b) => b.totalSpent - a.totalSpent);

    renderUsers(allUsers);
  } catch (err) {
    showToast("Failed to load users.", "error");
  }
}

loadUsers();
renderFooter();
