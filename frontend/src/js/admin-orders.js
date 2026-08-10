import "./admin-common.js";
import api, { getProductsCached } from "./api.js";
import { showToast } from "../components/toast.js";
import { renderFooter } from "../components/footer.js";

const listEl = document.getElementById("ordersList");
const msg = document.getElementById("msg");
const searchInput = document.getElementById("searchInput");

const STATUSES = ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED", "REFUNDED"];
let allOrders = [];
let productCatalogMap = {};

function renderOrders(orders) {
  if (orders.length === 0) {
    listEl.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        </div>
        <h3 class="empty-state-title">No orders found</h3>
        <p class="empty-state-desc">Orders from customers will appear here.</p>
      </div>`;
    return;
  }

  listEl.innerHTML = orders
    .map(
      (o) => {
        const payment = o._payment;
        const items = o.items || [];
        const addr = o.shippingAddress || o.address || null;
        const customerEmail = o.email || o.userEmail || o.customerEmail || (typeof addr === 'object' ? addr?.email : null) || o._payment?.email || localStorage.getItem("userEmail") || "deva.s.professional@gmail.com";
        o._resolvedEmail = customerEmail;

        return `
        <div class="admin-item-card" data-order-id="${o.orderId}" style="grid-template-columns: 1fr;">
          <div style="display:flex;align-items:flex-start;justify-content:space-between;flex-wrap:wrap;gap:12px;">
            <div style="flex:1;min-width:200px;">
              <div style="font-weight:700;font-size:15px;">Order <span style="font-family:monospace;word-break:break-all;">${o.orderId || ""}</span></div>
              <div style="font-size:13px;color:var(--accent);font-weight:600;margin-top:2px;">
                Email: ${customerEmail}
              </div>
              <div style="font-size:12px;color:var(--text-muted);margin-top:2px;">
                User: <span style="font-family:monospace;word-break:break-all;">${o.userId || ""}</span>
              </div>
              <div style="font-size:12px;color:var(--text-muted);margin-top:2px;">
                Placed: ${new Date(o.createdAt).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" })}
              </div>
            </div>
            <div style="display:flex;align-items:center;gap:12px;">
              <div style="text-align:right;">
                <div style="font-size:12px;color:var(--text-muted);">Total</div>
                <div style="font-weight:700;font-size:16px;">₹${o.totalAmount}</div>
              </div>
              <span class="badge badge-${o.status.toLowerCase()}">${o.status}</span>
            </div>
          </div>

          ${items.length > 0 ? `
          <div style="margin-top:12px;padding-top:12px;border-top:1px solid var(--border-light);">
            <div style="font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:0.05em;color:var(--text-muted);margin-bottom:8px;">Items Ordered</div>
            <div style="display:flex;flex-direction:column;gap:6px;">
              ${items.map((item) => {
                const prod = productCatalogMap[item.productId] || {};
                const prodImages = (prod.imageUrls && prod.imageUrls.length > 0) ? prod.imageUrls : (prod.imageUrl ? [prod.imageUrl] : []);
                const itemImages = (item.imageUrls && item.imageUrls.length > 0) ? item.imageUrls : (item.image ? [item.image] : []);
                const imgUrl = itemImages[0] || item.imageUrl || prodImages[0] || prod.imageUrl || '/placeholder.svg';
                return `
                <div style="display:flex;align-items:center;justify-content:space-between;padding:8px 12px;background:var(--surface-2);border-radius:var(--radius-sm);font-size:13px;">
                  <div style="display:flex;align-items:center;gap:10px;">
                    <img src="${imgUrl}" alt="${item.productName || item.name || 'Product'}" style="width:36px;height:36px;object-fit:cover;border-radius:6px;background:var(--border-light);" onerror="this.src='/placeholder.svg'" />
                    <div>
                      <span style="font-weight:600;display:inline-block;">${item.productName || item.name || prod.name || "Unknown Product"}</span>
                      <span style="color:var(--text-muted);margin-left:6px;">× ${item.quantity}</span>
                    </div>
                  </div>
                  <span style="font-weight:700;color:var(--accent);">₹${item.price || prod.price || 0}</span>
                </div>`;
              }).join("")}
            </div>
          </div>` : ""}

          ${payment ? `
          <div style="margin-top:12px;padding-top:12px;border-top:1px solid var(--border-light);">
            <div style="font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:0.05em;color:var(--text-muted);margin-bottom:8px;">Payment</div>
            <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap;">
              <span style="font-family:monospace;font-size:12px;">${payment.paymentId || ""}</span>
              <span class="badge badge-${payment.status === "SUCCESS" ? "delivered" : payment.status === "FAILED" ? "cancelled" : "pending"}">${payment.status || "UNKNOWN"}</span>
              <span style="color:var(--text-muted);font-size:13px;">${payment.paymentMethod || ""} — ₹${payment.amount || o.totalAmount}</span>
              ${payment.transactionId ? `<span style="font-family:monospace;font-size:11px;color:var(--text-muted);">TXN: ${payment.transactionId}</span>` : ""}
            </div>
          </div>` : `
          <div style="margin-top:12px;padding-top:12px;border-top:1px solid var(--border-light);">
            <div style="font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:0.05em;color:var(--text-muted);margin-bottom:8px;">Payment</div>
            <span style="font-size:13px;color:var(--text-muted);">No payment record found</span>
          </div>`}

          ${addr ? `
          <div style="margin-top:12px;padding-top:12px;border-top:1px solid var(--border-light);">
            <div style="font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:0.05em;color:var(--text-muted);margin-bottom:8px;">Shipping Address</div>
            <div style="font-size:13px;line-height:1.6;color:var(--text-secondary);">
              ${typeof addr === "string" ? addr : `
                ${addr.name ? `<div style="font-weight:600;">${addr.name}</div>` : ""}
                ${addr.line1 || addr.street ? `<div>${addr.line1 || addr.street}</div>` : ""}
                ${addr.line2 ? `<div>${addr.line2}</div>` : ""}
                ${[addr.city, addr.state, addr.pincode || addr.zip || addr.postalCode].filter(Boolean).join(", ") ? `<div>${[addr.city, addr.state, addr.pincode || addr.zip || addr.postalCode].filter(Boolean).join(", ")}</div>` : ""}
                ${addr.country ? `<div>${addr.country}</div>` : ""}
                ${addr.phone ? `<div style="color:var(--text-muted);">Phone: ${addr.phone}</div>` : ""}
              `}
            </div>
          </div>` : ""}

          <div style="display:flex;align-items:center;gap:8px;margin-top:12px;padding-top:12px;border-top:1px solid var(--border-light);flex-wrap:wrap;">
            <select class="form-select statusSelect" style="width:auto;padding:6px 12px;font-size:13px;">
              ${STATUSES.map(
                (s) => `<option value="${s}" ${s === o.status ? "selected" : ""}>${s}</option>`
              ).join("")}
            </select>
            <button class="btn btn-primary btn-sm updateStatusBtn">Update Status</button>
            <button class="btn btn-outline btn-sm viewPaymentBtn">Refresh Payment</button>
            <button class="btn btn-ghost btn-sm viewDetailModalBtn" style="color:var(--accent); font-weight:700;">Full Details &rarr;</button>
            <div class="paymentInfo" style="width:100%;margin-top:8px;"></div>
          </div>
        </div>`;
      }
    )
    .join("");

  listEl.querySelectorAll(".updateStatusBtn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const card = e.target.closest(".admin-item-card");
      const orderId = card.dataset.orderId;
      const status = card.querySelector(".statusSelect").value;
      try {
        await api.put(`/orders/${orderId}/status?status=${status}`);
        showToast("Order status updated.", "success");
        await loadOrders();
      } catch (err) {
        showToast(err.response?.data?.message || "Update failed.", "error");
      }
    });
  });

  listEl.querySelectorAll(".viewPaymentBtn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const card = e.target.closest(".admin-item-card");
      const orderId = card.dataset.orderId;
      const infoEl = card.querySelector(".paymentInfo");
      try {
        const res = await api.get(`/payments/order/${orderId}`);
        const payments = res.data || [];
        if (payments.length === 0) {
          infoEl.innerHTML = `<span style="font-size:13px;color:var(--text-muted);">No payment found.</span>`;
          return;
        }
        infoEl.innerHTML = payments
          .map(
            (p) => `
            <div style="padding:10px 12px;background:var(--surface-2);border-radius:var(--radius-sm);font-size:13px;display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
              <span style="font-family:monospace;">${p.paymentId}</span>
              <span class="badge badge-${p.status === "SUCCESS" ? "delivered" : "pending"}">${p.status}</span>
              <span style="color:var(--text-muted);">${p.paymentMethod} — ₹${p.amount}</span>
              ${p.transactionId ? `<span style="font-family:monospace;font-size:11px;color:var(--text-muted);">TXN: ${p.transactionId}</span>` : ""}
            </div>`
          )
          .join("");
      } catch (err) {
        infoEl.innerHTML = `<span style="font-size:13px;color:var(--text-muted);">Failed to fetch payment info.</span>`;
      }
    });
  });

  listEl.querySelectorAll(".viewDetailModalBtn").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      const card = e.target.closest(".admin-item-card");
      const orderId = card.dataset.orderId;
      const targetOrder = allOrders.find((o) => o.orderId === orderId);
      if (targetOrder) {
        showAdminOrderDetailModal(targetOrder, () => loadOrders());
      }
    });
  });
}

function showAdminOrderDetailModal(o, onStatusUpdated) {
  const existing = document.getElementById("adminOrderDetailModal");
  if (existing) existing.remove();

  const items = o.items || [];
  const addr = o.shippingAddress || o.address || null;

  const itemsHtml = items.length > 0 ? items.map((item) => {
    const p = productCatalogMap[item.productId] || {};
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
        <span class="badge badge-${o.status.toLowerCase()}" id="modalStatusBadge">${o.status}</span>
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
          <div style="font-size:16px; font-weight:800; color:var(--accent); margin-top:2px;">₹${(o.totalAmount || 0).toLocaleString("en-IN")}</div>
        </div>
      </div>

      <div style="margin-bottom:20px; padding:14px; border:1px solid var(--border); border-radius:var(--radius); background:color-mix(in srgb, var(--accent) 5%, transparent);">
        <div style="font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--text-muted); margin-bottom:8px;">Manage Order Status</div>
        <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
          <select id="adminModalStatusSelect" class="form-select" style="width:auto; flex:1; min-width:140px; padding:8px 12px; font-size:13px; font-weight:600;">
            ${STATUSES.map((s) => `<option value="${s}" ${s === o.status ? "selected" : ""}>${s}</option>`).join("")}
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
        modal.querySelector("#modalStatusBadge").className = `badge badge-${newStatus.toLowerCase()}`;
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

function filterOrders(query) {
  const q = query.toLowerCase().trim();
  if (!q) return allOrders;

  return allOrders
    .filter((o) => {
      const orderId = (o.orderId || "").toLowerCase();
      const userId = (o.userId || "").toLowerCase();
      const resolvedEmail = (o._resolvedEmail || o.email || o.userEmail || o.customerEmail || o.shippingAddress?.email || o.address?.email || "").toLowerCase();
      const status = (o.status || "").toLowerCase();
      
      if (orderId.includes(q) || userId.includes(q) || resolvedEmail.includes(q) || status.includes(q)) {
        return true;
      }

      try {
        return JSON.stringify(o).toLowerCase().includes(q);
      } catch {
        return false;
      }
    })
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
}

searchInput?.addEventListener("input", () => {
  renderOrders(filterOrders(searchInput.value));
});

function renderOrdersSkeleton() {
  const card = `
    <div class="skeleton-order-admin-card">
      <div class="skeleton"></div>
      <div class="skeleton"></div>
      <div class="skeleton"></div>
      <div class="skeleton"></div>
      <div class="skeleton"></div>
      <div class="skeleton"></div>
    </div>`;
  listEl.innerHTML = card.repeat(3);
}

async function loadOrders() {
  renderOrdersSkeleton();

  try {
    const [res, prodRes] = await Promise.all([
      api.get("/orders/all").catch(() => api.get("/orders")),
      getProductsCached().catch(() => ({ data: [] }))
    ]);
    (prodRes.data || []).forEach((p) => { if (p && p.productId) productCatalogMap[p.productId] = p; });
    const rawData = res.data;
    const orders = Array.isArray(rawData) ? rawData : (rawData?.orders || []);

    const ordersWithPayments = await Promise.all(
      orders.map(async (o) => {
        try {
          const payRes = await api.get(`/payments/order/${o.orderId}`);
          return { ...o, _payment: (payRes.data || [])[0] || null };
        } catch {
          return { ...o, _payment: null };
        }
      })
    );

    allOrders = ordersWithPayments;
    allOrders.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    if (allOrders.length === 0) {
      listEl.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          </div>
          <h3 class="empty-state-title">No orders found</h3>
          <p class="empty-state-desc">Orders from customers will appear here.</p>
        </div>`;
      return;
    }

    renderOrders(allOrders);
  } catch (err) {
    if (err.response?.status === 404) {
      listEl.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/></svg>
          </div>
          <h3 class="empty-state-title">No orders found</h3>
          <p class="empty-state-desc">Orders from customers will appear here.</p>
        </div>`;
    } else {
      showToast("Failed to load orders.", "error");
    }
  }
}

loadOrders();

document.getElementById("exportCsvBtn")?.addEventListener("click", () => {
  if (!allOrders.length) {
    showToast("No orders available to export.", "error");
    return;
  }
  const headers = ["Order ID", "User ID", "Total Amount", "Status", "Created At"];
  const rows = allOrders.map(o => [
    `"${o.orderId || ''}"`,
    `"${o.userId || ''}"`,
    o.totalAmount || 0,
    `"${o.status || ''}"`,
    `"${new Date(o.createdAt).toISOString()}"`
  ]);

  const csvContent = "data:text/csv;charset=utf-8," + [headers.join(","), ...rows.map(e => e.join(","))].join("\n");
  const encodedUri = encodeURI(csvContent);
  const link = document.createElement("a");
  link.setAttribute("href", encodedUri);
  link.setAttribute("download", `shopvibe_orders_${new Date().toISOString().slice(0,10)}.csv`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  showToast("Orders exported to CSV!", "success");
});

renderFooter();
