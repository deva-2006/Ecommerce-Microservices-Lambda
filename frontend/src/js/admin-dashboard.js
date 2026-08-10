import "./admin-common.js";
import api from "./api.js";
import { showToast } from "../components/toast.js";
import { renderFooter } from "../components/footer.js";

const STATUS_ORDER = ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED", "REFUNDED"];
const STATUS_COLOR = {
  PENDING: "#f08c00",
  CONFIRMED: "#228be6",
  SHIPPED: "#7048e8",
  DELIVERED: "#2f9e44",
  CANCELLED: "#e03131",
  REFUNDED: "#868e96",
};
const REVENUE_STATUSES = new Set(["CONFIRMED", "SHIPPED", "DELIVERED"]);
const LOW_STOCK_THRESHOLD = 10;
const CRITICAL_STOCK_THRESHOLD = 3;

const statSkeleton = document.getElementById("statSkeleton");
const statGrid = document.getElementById("statGrid");
const recentOrdersWrap = document.getElementById("recentOrdersWrap");
const statusChart = document.getElementById("statusChart");
const lowStockList = document.getElementById("lowStockList");
const dashGrid = document.querySelector(".dash-grid");

function fmtCurrency(n) {
  return `₹${Math.round(n).toLocaleString("en-IN")}`;
}

function badgeClass(status) {
  return `badge badge-${status.toLowerCase()}`;
}

function animateCount(el, target, { prefix = "", duration = 900 } = {}) {
  const start = 0;
  const startTime = performance.now();
  el.setAttribute("data-counting", "true");

  function tick(now) {
    const progress = Math.min(1, (now - startTime) / duration);
    const eased = 1 - Math.pow(1 - progress, 3);
    const value = Math.round(start + (target - start) * eased);
    el.textContent = prefix ? `${prefix}${value.toLocaleString("en-IN")}` : value.toLocaleString("en-IN");
    if (progress < 1) requestAnimationFrame(tick);
    else el.removeAttribute("data-counting");
  }
  requestAnimationFrame(tick);
}

function statCard({ id, label, target, color, iconPath, sub, isCurrency, trend }) {
  return `
    <div class="stat-card" style="--stat-color: ${color}">
      <div style="display:flex; justify-content:space-between; align-items:flex-start;">
        <div class="stat-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${iconPath}</svg></div>
        ${trend ? `<span class="badge badge-success" style="font-size:11px; font-weight:700;">${trend}</span>` : `<span class="badge badge-pending" style="font-size:11px; opacity:0.8;">Live</span>`}
      </div>
      <p class="stat-label">${label}</p>
      <p class="stat-value" id="${id}" data-target="${target}" data-currency="${isCurrency ? 1 : 0}">0</p>
      ${sub ? `<p class="stat-sub">${sub}</p>` : ""}
    </div>`;
}

const ICONS = {
  revenue: '<path d="M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>',
  pending: '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
  confirmed: '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="M22 4 12 14.01l-3-3"/>',
  products: '<path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/><path d="m3.3 7 8.7 5 8.7-5M12 22V12"/>',
};

async function loadDashboard(isRefresh = false) {
  try {
    const [ordersRes, productsRes, inventoryRes] = await Promise.all([
      api.get("/orders/all").catch(() => api.get("/orders")).catch(() => ({ data: [] })),
      api.get("/products").catch(() => ({ data: [] })),
      api.get("/inventory").catch(() => ({ data: [] })),
      api.get("/reviews/health").catch(() => api.get("/reviews")).catch(() => ({ data: [] })),
    ]);

    const orders = ordersRes.data || [];
    const products = productsRes.data || [];
    const inventory = inventoryRes.data || [];

    if (isRefresh) {
      updateDashboardContent(orders, products, inventory);
    } else {
      renderStats(orders, products);
      renderStatusChart(orders);
      renderRecentOrders(orders, products);
      renderLowStock(inventory, products);
      renderTopProducts(orders, products);
      dashGrid?.classList.add("animate-in");
    }
  } catch (err) {
    showToast("Failed to load dashboard data.", "error");
  }
}

function updateDashboardContent(orders, products, inventory) {
  renderStats(orders, products);
  renderStatusChart(orders);
  renderRecentOrders(orders, products);
  renderLowStock(inventory, products);
  renderTopProducts(orders, products);
}

function renderStats(orders, products) {
  const revenue = orders
    .filter((o) => REVENUE_STATUSES.has(o.status))
    .reduce((sum, o) => sum + (o.totalAmount || 0), 0);

  const pending = orders.filter((o) => o.status === "PENDING").length;
  const confirmed = orders.filter((o) => o.status === "CONFIRMED").length;
  const paidCount = orders.filter((o) => REVENUE_STATUSES.has(o.status)).length;

  statGrid.innerHTML = [
    statCard({
      id: "statRevenue",
      label: "Total Revenue",
      target: Math.round(revenue),
      color: STATUS_COLOR.DELIVERED,
      iconPath: ICONS.revenue,
      sub: `from ${paidCount} paid order${paidCount === 1 ? "" : "s"}`,
      isCurrency: true,
      trend: "↑ +14.2%"
    }),
    statCard({
      id: "statPending",
      label: "Orders Pending",
      target: pending,
      color: STATUS_COLOR.PENDING,
      iconPath: ICONS.pending,
      sub: pending > 0 ? "needs review" : "all clear",
    }),
    statCard({
      id: "statConfirmed",
      label: "Orders Confirmed",
      target: confirmed,
      color: STATUS_COLOR.CONFIRMED,
      iconPath: ICONS.confirmed,
      trend: "Active"
    }),
    statCard({
      id: "statProducts",
      label: "Total Products",
      target: products.length,
      color: "#7048e8",
      iconPath: ICONS.products,
    }),
  ].join("");

  statSkeleton.style.display = "none";
  statGrid.style.display = "grid";

  statGrid.querySelectorAll(".stat-value").forEach((el, i) => {
    const target = Number(el.dataset.target);
    const isCurrency = el.dataset.currency === "1";
    setTimeout(() => {
      animateCount(el, target, { prefix: isCurrency ? "₹" : "", duration: 900 });
    }, i * 70 + 150);
  });
}

function renderStatusChart(orders) {
  const counts = {};
  STATUS_ORDER.forEach((s) => (counts[s] = 0));
  orders.forEach((o) => {
    if (counts[o.status] !== undefined) counts[o.status]++;
  });
  const max = Math.max(1, ...Object.values(counts));

  statusChart.innerHTML = STATUS_ORDER.map((s) => `
    <div style="margin-bottom: 14px;">
      <div style="display: flex; justify-content: space-between; align-items: center; font-size: 13px; margin-bottom: 6px;">
        <span style="font-weight: 700; color: var(--text); text-transform: uppercase; font-size: 12px; letter-spacing: 0.04em;">${s}</span>
        <span style="font-weight: 600; color: var(--text-secondary); font-size: 12px; background: var(--surface-2); padding: 2px 8px; border-radius: var(--radius-full); border: 1px solid var(--border-light);">${counts[s]} ${counts[s] === 1 ? 'order' : 'orders'}</span>
      </div>
      <div class="bar-track" style="height: 8px; background: var(--surface-2); border-radius: 999px; overflow: hidden; border: 1px solid var(--border-light);">
        <div class="bar-fill" style="--target-width: ${(counts[s] / max) * 100}%; background: ${STATUS_COLOR[s]}; height: 100%; border-radius: 999px; transition: width 0.8s cubic-bezier(0.16, 1, 0.3, 1);"></div>
      </div>
    </div>`).join("");

  requestAnimationFrame(() => {
    statusChart.querySelectorAll(".bar-fill").forEach((el, i) => {
      setTimeout(() => el.classList.add("filled"), i * 60);
    });
  });
}

function renderRecentOrders(orders, products = []) {
  if (orders.length === 0) {
    recentOrdersWrap.innerHTML = `
      <div class="empty-state" style="padding:32px;">
        <div class="empty-state-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        </div>
        <h3 class="empty-state-title">No orders yet</h3>
        <p class="empty-state-desc">Orders will appear here once customers start buying.</p>
      </div>`;
    return;
  }

  const recent = [...orders]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 8);

  recentOrdersWrap.innerHTML = `
    <div class="data-table-wrapper">
    <table class="data-table">
      <thead>
        <tr>
          <th style="padding:14px 14px;">Order ID</th>
          <th style="padding:14px 14px;">Total</th>
          <th style="padding:14px 14px;">Status</th>
          <th style="padding:14px 14px;">Placed</th>
          <th style="padding:14px 14px; text-align:right;">Action</th>
        </tr>
      </thead>
      <tbody>
        ${recent.map((o) => `
          <tr class="recent-order-row" data-order-id="${o.orderId}" style="cursor:pointer; transition:background var(--transition);">
            <td style="font-family:monospace;font-size:13px;word-break:break-all;font-weight:600;padding:16px 14px;">${o.orderId || ""}</td>
            <td style="font-weight:700; color:var(--accent);padding:16px 14px;">${fmtCurrency(o.totalAmount || 0)}</td>
            <td style="padding:16px 14px;"><span class="${badgeClass(o.status)}">${o.status}</span></td>
            <td style="color:var(--text-muted);font-size:13px;padding:16px 14px;">${new Date(o.createdAt).toLocaleDateString("en-IN", { day: "numeric", month: "short" })}</td>
            <td style="padding:16px 14px; text-align:right;">
              <button class="btn btn-outline btn-sm view-order-detail-btn" style="padding:4px 10px; font-size:12px;">View Details</button>
            </td>
          </tr>`).join("")}
      </tbody>
    </table>
    </div>`;

  recentOrdersWrap.querySelectorAll(".recent-order-row").forEach((row) => {
    row.addEventListener("click", () => {
      const id = row.dataset.orderId;
      const targetOrder = orders.find((o) => o.orderId === id);
      if (targetOrder) {
        showAdminOrderDetailModal(targetOrder, () => loadDashboard(true), products);
      }
    });
  });
}

function showAdminOrderDetailModal(o, onStatusUpdated, productsList = []) {
  const existing = document.getElementById("adminOrderDetailModal");
  if (existing) existing.remove();

  const productMap = {};
  if (Array.isArray(productsList)) {
    productsList.forEach((p) => { if (p && p.productId) productMap[p.productId] = p; });
  }

  const items = o.items || [];
  const addr = o.shippingAddress || o.address || null;

  const itemsHtml = items.length > 0 ? items.map((item) => {
    const p = productMap[item.productId] || {};
    const prodImages = (p.imageUrls && p.imageUrls.length > 0) ? p.imageUrls : (p.imageUrl ? [p.imageUrl] : []);
    const itemImages = (item.imageUrls && item.imageUrls.length > 0) ? item.imageUrls : (item.image ? [item.image] : []);
    const imgUrl = itemImages[0] || item.imageUrl || prodImages[0] || p.imageUrl || '/placeholder.svg';
    return `
    <div style="display:flex; align-items:center; justify-content:space-between; gap:12px; padding:10px 12px; background:var(--surface-2); border-radius:var(--radius-sm); margin-bottom:6px;">
      <div style="display:flex; align-items:center; gap:10px;">
        <img src="${imgUrl}" alt="${item.productName || item.name || p.name || 'Product'}" style="width:44px; height:44px; object-fit:cover; border-radius:6px; background:var(--border-light);" onerror="this.src='/placeholder.svg'" />
        <div>
          <div style="font-weight:600; font-size:13px; color:var(--text);">${item.productName || item.name || p.name || "Product"}</div>
          <div style="font-size:11px; color:var(--text-muted);">₹${item.price || p.price || 0} × ${item.quantity || 1}</div>
        </div>
      </div>
      <div style="font-weight:700; font-size:13px; color:var(--accent);">₹${((item.price || p.price || 0) * (item.quantity || 1)).toLocaleString("en-IN")}</div>
    </div>
  `}).join("") : `<div style="font-size:13px; color:var(--text-muted); padding:8px 0;">No items listed in order.</div>`;

  const modal = document.createElement("div");
  modal.id = "adminOrderDetailModal";
  modal.className = "modal-overlay";
  modal.style.cssText = "position:fixed; inset:0; background:rgba(0,0,0,0.65); backdrop-filter:blur(6px); display:flex; align-items:center; justify-content:center; z-index:9999; padding:16px;";

  modal.innerHTML = `
    <div class="modal-box" style="background:var(--surface); border:1px solid var(--border); border-radius:var(--radius-lg); width:100%; max-width:640px; max-height:90vh; overflow-y:auto; padding:24px; box-shadow:var(--shadow-2xl); position:relative;">
      <button id="closeAdminOrderModal" style="position:absolute; top:16px; right:16px; background:var(--surface-2); border:none; width:32px; height:32px; border-radius:50%; display:flex; align-items:center; justify-content:center; color:var(--text-secondary); cursor:pointer; font-size:18px;">&times;</button>
      
      <div style="display:flex; align-items:center; gap:10px; margin-bottom:16px; padding-right:40px;">
        <h3 style="font-size:18px; font-weight:800; margin:0;">Order Details</h3>
        <span class="${badgeClass(o.status)}" id="modalStatusBadge">${o.status}</span>
      </div>

      <div style="background:var(--surface-2); border-radius:var(--radius); padding:16px; margin-bottom:20px; font-size:13px; display:grid; grid-template-columns:1fr 1fr; gap:12px;">
        <div>
          <div style="color:var(--text-muted); font-size:11px; text-transform:uppercase; font-weight:700;">Order ID</div>
          <div style="font-family:monospace; font-weight:700; word-break:break-all; margin-top:2px;">${o.orderId}</div>
        </div>
        <div>
          <div style="color:var(--text-muted); font-size:11px; text-transform:uppercase; font-weight:700;">Customer ID</div>
          <div style="font-family:monospace; word-break:break-all; margin-top:2px;">${o.userId || "Guest"}</div>
        </div>
        <div>
          <div style="color:var(--text-muted); font-size:11px; text-transform:uppercase; font-weight:700;">Placed Date</div>
          <div style="margin-top:2px; font-weight:500;">${new Date(o.createdAt).toLocaleString("en-IN")}</div>
        </div>
        <div>
          <div style="color:var(--text-muted); font-size:11px; text-transform:uppercase; font-weight:700;">Total Amount</div>
          <div style="font-size:16px; font-weight:800; color:var(--accent); margin-top:2px;">${fmtCurrency(o.totalAmount || 0)}</div>
        </div>
      </div>

      <div style="margin-bottom:20px; padding:14px; border:1px solid var(--border); border-radius:var(--radius); background:color-mix(in srgb, var(--accent) 5%, transparent);">
        <div style="font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--text-muted); margin-bottom:8px;">Manage Order Status</div>
        <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
          <select id="adminModalStatusSelect" class="form-select" style="width:auto; flex:1; min-width:140px; padding:8px 12px; font-size:13px; font-weight:600;">
            ${STATUS_ORDER.map((s) => `<option value="${s}" ${s === o.status ? "selected" : ""}>${s}</option>`).join("")}
          </select>
          <button id="adminModalUpdateStatusBtn" class="btn btn-primary btn-sm" style="padding:8px 16px; font-weight:700;">Update Status</button>
        </div>
      </div>

      <div style="margin-bottom:20px;">
        <div style="font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--text-muted); margin-bottom:10px;">Items Ordered (${items.length})</div>
        ${itemsHtml}
      </div>

      ${addr ? `
      <div style="margin-bottom:20px; padding:14px; background:var(--surface-2); border-radius:var(--radius); font-size:13px;">
        <div style="font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--text-muted); margin-bottom:6px;">Shipping Address</div>
        <div style="color:var(--text-secondary); line-height:1.5;">
          ${typeof addr === "string" ? addr : `
            ${addr.name ? `<div style="font-weight:700; color:var(--text);">${addr.name}</div>` : ""}
            ${addr.line1 || addr.street ? `<div>${addr.line1 || addr.street}</div>` : ""}
            ${addr.line2 ? `<div>${addr.line2}</div>` : ""}
            ${[addr.city, addr.state, addr.pincode || addr.zip || addr.postalCode].filter(Boolean).join(", ") ? `<div>${[addr.city, addr.state, addr.pincode || addr.zip || addr.postalCode].filter(Boolean).join(", ")}</div>` : ""}
            ${addr.phone ? `<div style="color:var(--text-muted); margin-top:2px;">Phone: ${addr.phone}</div>` : ""}
          `}
        </div>
      </div>` : ""}

      <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:20px; padding-top:16px; border-top:1px solid var(--border-light);">
        <button id="closeAdminOrderModalBtn" class="btn btn-ghost">Close</button>
      </div>
    </div>`;

  document.body.appendChild(modal);

  const closeModal = () => modal.remove();
  modal.querySelector("#closeAdminOrderModal").addEventListener("click", closeModal);
  modal.querySelector("#closeAdminOrderModalBtn").addEventListener("click", closeModal);
  modal.addEventListener("click", (e) => {
    if (e.target === modal) closeModal();
  });

  const updateBtn = modal.querySelector("#adminModalUpdateStatusBtn");
  if (updateBtn) {
    updateBtn.addEventListener("click", async () => {
      const newStatus = modal.querySelector("#adminModalStatusSelect").value;
      updateBtn.disabled = true;
      updateBtn.textContent = "Updating...";
      try {
        await api.put(`/orders/${o.orderId}/status?status=${newStatus}`);
        showToast(`Order status updated to ${newStatus}`, "success");
        o.status = newStatus;
        modal.querySelector("#modalStatusBadge").className = badgeClass(newStatus);
        modal.querySelector("#modalStatusBadge").textContent = newStatus;
        if (onStatusUpdated) onStatusUpdated(o);
      } catch (err) {
        showToast(err.response?.data?.message || "Failed to update status", "error");
      } finally {
        updateBtn.disabled = false;
        updateBtn.textContent = "Update Status";
      }
    });
  }
}

function renderLowStock(inventory, products) {
  const invMap = {};
  const hasInvMap = {};
  inventory.forEach((i) => {
    invMap[i.productId] = i.quantity;
    hasInvMap[i.productId] = true;
  });

  // Include ONLY active products from the product catalog (ignoring orphaned / deleted inventory records)
  const combined = products.map((p) => {
    const qty = invMap[p.productId] ?? p.stock ?? p.quantity ?? 0;
    return {
      productId: p.productId,
      name: p.name || "Unnamed Product",
      quantity: qty,
      hasInventory: hasInvMap[p.productId] || false,
    };
  });

  const low = combined
    .filter((i) => i.quantity <= LOW_STOCK_THRESHOLD)
    .sort((a, b) => a.quantity - b.quantity)
    .slice(0, 6);

  if (low.length === 0) {
    lowStockList.innerHTML = `
      <div class="empty-state" style="padding:24px;">
        <div class="empty-state-icon" style="background:var(--success-soft);">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--success)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="M22 4 12 14.01l-3-3"/></svg>
        </div>
        <h3 class="empty-state-title">All stock healthy</h3>
        <p class="empty-state-desc">No inventory alerts at this time.</p>
      </div>`;
    return;
  }

  lowStockList.innerHTML = low.map((i) => `
    <div class="low-stock-item" style="display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid var(--border-light);">
      <div>
        <div style="font-weight:600; font-size:13px; color:var(--text);">${i.name}</div>
        <div style="font-size:11px; color:var(--text-muted); font-family:monospace;">ID: ${i.productId.slice(0, 8)}</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px;">
        <span class="stock-pill ${i.quantity <= CRITICAL_STOCK_THRESHOLD ? "critical" : ""}" style="font-weight:700;">${i.quantity} left</span>
        <button class="btn btn-outline btn-sm quick-restock-btn" data-pid="${i.productId}" data-curr="${i.quantity}" data-has-inv="${i.hasInventory}" data-add="10" style="padding:2px 8px; font-size:11px; height:26px;">+10</button>
      </div>
    </div>`).join("");

  lowStockList.querySelectorAll(".quick-restock-btn").forEach(btn => {
    btn.addEventListener("click", async (e) => {
      const pid = e.currentTarget.dataset.pid;
      const curr = Number(e.currentTarget.dataset.curr || 0);
      const add = Number(e.currentTarget.dataset.add || 10);
      const hasInv = e.currentTarget.dataset.hasInv === "true";
      e.currentTarget.disabled = true;
      e.currentTarget.textContent = "...";
      try {
        if (hasInv) {
          await api.put(`/inventory/${pid}/add-stock?quantity=${add}`).catch(() => 
            api.put(`/inventory/${pid}`, { quantity: curr + add })
          );
        } else {
          await api.post("/inventory", { productId: pid, quantity: curr + add }).catch(() =>
            api.put(`/inventory/${pid}`, { quantity: curr + add })
          );
        }
        showToast(`Restocked +${add} items!`, "success");
        loadDashboard(true);
      } catch (err) {
        showToast("Restock failed.", "error");
        e.currentTarget.disabled = false;
        e.currentTarget.textContent = "+10";
      }
    });
  });
}

function renderTopProducts(orders, products) {
  const container = document.getElementById("topProductsWrap");
  if (!container) return;

  const sales = {};
  orders.forEach(o => {
    if (REVENUE_STATUSES.has(o.status) && o.items) {
      o.items.forEach(item => {
        sales[item.productId] = (sales[item.productId] || 0) + (item.quantity || 1);
      });
    }
  });

  const top = Object.entries(sales)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);

  const nameById = Object.fromEntries(products.map((p) => [p.productId, p.name]));
  const maxSales = Math.max(1, top[0]?.[1] || 1);
  const RANK_BADGES = ["🥇", "🥈", "🥉", "4️⃣", "5️⃣"];

  if (top.length === 0) {
    container.innerHTML = `
      <div class="empty-state" style="padding:16px;">
        <p class="empty-state-desc">No sales data yet.</p>
      </div>`;
    return;
  }

  container.innerHTML = top.map(([pid, qty], idx) => `
    <div style="margin-bottom:12px;">
      <div style="display:flex; justify-content:space-between; align-items:center; font-size:13px; margin-bottom:4px;">
        <span><strong>${RANK_BADGES[idx] || `#${idx + 1}`}</strong> ${nameById[pid] || pid.slice(0, 8)}</span>
        <span style="font-weight:700; color:var(--accent);">${qty} sold</span>
      </div>
      <div style="height:6px; background:var(--border-light); border-radius:999px; overflow:hidden;">
        <div style="width:${(qty / maxSales) * 100}%; background:linear-gradient(90deg, var(--accent), var(--purple)); height:100%; border-radius:999px; transition:width 0.8s ease;"></div>
      </div>
    </div>`).join("");
}

document.getElementById("refreshBtn")?.addEventListener("click", async (e) => {
  const btn = e.currentTarget;
  const main = document.querySelector(".admin-content");
  const origHTML = btn.innerHTML;

  btn.disabled = true;
  btn.innerHTML = '<span class="dash-loading-spinner"></span> Refreshing...';
  main?.classList.add("dash-loading");

  await loadDashboard(true);

  requestAnimationFrame(() => {
    main?.classList.remove("dash-loading");
    btn.disabled = false;
    btn.innerHTML = origHTML;
  });
});

loadDashboard();

document.getElementById("dateFilterSelect")?.addEventListener("change", () => {
  showToast("Filtering data...", "info");
  loadDashboard(true);
});

renderFooter();
