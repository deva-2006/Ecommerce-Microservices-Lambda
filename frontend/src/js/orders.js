import { requireAuth } from "./auth.js";
import { renderNavbar } from "../components/navbar.js";
import { showToast } from "../components/toast.js";
import { renderFooter } from "../components/footer.js";
import api, { getProductsCached } from "./api.js";

const contentEl = document.getElementById("ordersContent");
const msg = document.getElementById("msg");

const skOrder = `
  <div class="sk-order-card" style="margin-bottom:20px;">
    <div class="sk-order-header">
      <div class="sk-row"><div class="skeleton" style="width:140px;height:16px;border-radius:6px;"></div><div class="skeleton" style="width:80px;height:24px;border-radius:99px;"></div></div>
    </div>
    <div class="sk-order-body">
      <div class="sk-timeline" style="margin:20px 0;"><div class="skeleton" style="width:16px;height:16px;border-radius:50%;"></div><div class="skeleton" style="flex:1;height:4px;border-radius:99px;"></div><div class="skeleton" style="width:16px;height:16px;border-radius:50%;"></div><div class="skeleton" style="flex:1;height:4px;border-radius:99px;"></div><div class="skeleton" style="width:16px;height:16px;border-radius:50%;"></div></div>
      <div class="skeleton" style="height:60px;border-radius:12px;margin-bottom:16px;"></div>
      <div class="sk-row" style="gap:10px;"><div class="skeleton" style="flex:1;height:38px;border-radius:8px;"></div><div class="skeleton" style="flex:1;height:38px;border-radius:8px;"></div></div>
    </div>
  </div>`;

contentEl.innerHTML = `
  <div class="orders-page-header" data-reveal>
    <h1 class="orders-page-title">My Orders</h1>
    <p class="orders-page-sub">Track and manage your order history</p>
  </div>
  <div class="skeleton" style="height:44px;max-width:68%;margin:0 auto 24px;border-radius:var(--radius-lg);"></div>
  <div class="orders-list">${skOrder.repeat(3)}</div>
`;
contentEl.style.display = "";

let allOrders = [];
let productMap = {};

await requireAuth();
await renderNavbar("navbar");

try {
  const prodRes = await getProductsCached().catch(() => ({ data: [] }));
  (prodRes.data || []).forEach((p) => { productMap[p.productId] = p; });
} catch {}

try {
  const res = await api.get("/orders");
  allOrders = (res.data || []).filter(Boolean);
  allOrders.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  renderOrders(allOrders);
} catch {
  contentEl.innerHTML = `<div class="empty-state"><p class="error">Failed to load orders.</p></div>`;
}

function fmt(amount) {
  return `\u20B9${Number(amount || 0).toLocaleString("en-IN")}`;
}

function shortId(orderId) {
  if (!orderId) return "ORD-0000";
  return `ORD-${orderId.replace(/-/g, "").slice(-6).toUpperCase()}`;
}

function formattedTimestamp(d) {
  if (!d) return "Today";
  const dateObj = new Date(d);
  const diff = Date.now() - dateObj.getTime();
  const days = Math.floor(diff / 86400000);
  const timeStr = dateObj.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: true });

  let datePrefix = "";
  if (days === 0) datePrefix = "Today";
  else if (days === 1) datePrefix = "Yesterday";
  else datePrefix = dateObj.toLocaleDateString("en-IN", { day: "numeric", month: "short" });

  return `${datePrefix} • ${timeStr}`;
}

function renderTimeline(status) {
  const steps = ["Placed", "Confirmed", "Shipped", "Delivered"];
  const activeMap = {
    PENDING: 0,
    CONFIRMED: 1,
    SHIPPED: 2,
    DELIVERED: 3,
  };
  const active = activeMap[status] ?? 1;

  return `
    <div class="od-timeline" role="list" aria-label="Order progress">
      ${steps.map((label, i) => `
        <div class="od-tl-step${i <= active ? " active" : ""}" role="listitem">
          <div class="od-tl-dot">${i <= active ? "✓" : "○"}</div>
          <span class="od-tl-label">${i <= active ? `✔ ${label}` : label}</span>
        </div>
        ${i < steps.length - 1 ? `<div class="od-tl-line${i <= active ? " active" : ""}"></div>` : ""}
      `).join("")}
    </div>`;
}

function renderItem(item) {
  const prod = productMap[item.productId] || {};
  const itemImgs = item.imageUrls?.length ? item.imageUrls : item.imageUrl ? [item.imageUrl] : [];
  const prodImgs = prod.imageUrls?.length ? prod.imageUrls : prod.imageUrl ? [prod.imageUrl] : [];
  const img = itemImgs[0] || prodImgs[0] || "/placeholder.svg";
  const href = `/product.html?id=${item.productId}`;

  return `
    <a href="${href}" class="od-item" onclick="event.stopPropagation();">
      <img src="${img}" alt="${item.productName || "Product"}" class="od-item-img" loading="lazy" onerror="this.src='/placeholder.svg'" />
      <div class="od-item-info">
        <span class="od-item-name">${item.productName || item.productId}</span>
        <span class="od-item-meta">Qty: ${item.quantity}</span>
      </div>
      <span class="od-item-price">${fmt(item.subtotal || item.quantity * item.price)}</span>
    </a>`;
}

function renderCard(o) {
  const st = (o.status || "CONFIRMED").toUpperCase();
  const items = o.items || [];
  const count = items.reduce((s, i) => s + (i.quantity || 1), 0);
  const isDelivered = st === "DELIVERED";

  // Badge with Icon
  const badgeMap = {
    PENDING: `<span class="od-badge od-badge--pending">⏳ Placed</span>`,
    CONFIRMED: `<span class="od-badge od-badge--confirmed">🟢 Confirmed</span>`,
    SHIPPED: `<span class="od-badge od-badge--shipped">🚚 Shipped</span>`,
    DELIVERED: `<span class="od-badge od-badge--delivered">✔ Delivered</span>`,
    CANCELLED: `<span class="od-badge od-badge--cancelled">❌ Cancelled</span>`
  };
  const badgeHtml = badgeMap[st] || `<span class="od-badge od-badge--confirmed">🟢 ${st}</span>`;

  // Delivery Estimate
  const dateObj = new Date(o.createdAt || Date.now());
  const deliveryDateObj = new Date(dateObj.getTime() + 4 * 86400000);
  const deliveryStr = isDelivered 
    ? `Delivered on ${dateObj.toLocaleDateString("en-IN", { day: "numeric", month: "short" })}` 
    : `Arriving by ${deliveryDateObj.toLocaleDateString("en-IN", { day: "numeric", month: "short" })}`;

  const paymentStr = o.paymentMethod || "Razorpay / UPI";
  const addressStr = o.shippingAddress ? (o.shippingAddress.length > 25 ? o.shippingAddress.substring(0, 25) + "..." : o.shippingAddress) : "Standard Shipping";

  return `
    <article class="od-card">
      <header class="od-card-head">
        <div class="od-card-head-left">
          <span class="od-card-id">Order #${shortId(o.orderId || o.id)}</span>
          <time class="od-card-date">Placed ${formattedTimestamp(o.createdAt)}</time>
        </div>
        ${badgeHtml}
      </header>

      <div class="od-card-body">
        ${renderTimeline(st)}

        <div class="od-card-meta">
          <span class="od-meta-summary">${count} Item${count !== 1 ? "s" : ""} &middot; ${fmt(o.totalAmount)}</span>
        </div>

        <!-- Multi-column Info Box to eliminate whitespace -->
        <div class="od-info-grid">
          <div class="od-info-item">
            <span class="od-info-label">Expected Delivery</span>
            <span class="od-info-val" style="color:var(--accent);">${deliveryStr}</span>
          </div>
          <div class="od-info-item">
            <span class="od-info-label">Payment</span>
            <span class="od-info-val">${paymentStr}</span>
          </div>
          <div class="od-info-item">
            <span class="od-info-label">Shipping Address</span>
            <span class="od-info-val" style="font-weight:500;">${addressStr}</span>
          </div>
        </div>

        <div class="od-card-actions">
          <button class="od-btn od-btn--ghost od-items-toggle" aria-expanded="false" onclick="
            const list=this.closest('.od-card-body').querySelector('.od-items');
            const open=list.style.display==='flex';
            list.style.display=open?'none':'flex';
            this.setAttribute('aria-expanded',!open);
            this.querySelector('.od-btn-label').textContent=open?'▼ View ${count} Item${count !== 1 ? 's' : ''}':'▲ Hide Items';
          ">
            <span class="od-btn-label">▼ View ${count} Item${count !== 1 ? 's' : ''}</span>
          </button>

          <button class="od-dl-invoice" data-oid="${o.orderId || o.id}">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            <span>📄 Download Invoice</span>
          </button>

          <button class="od-btn od-btn--primary od-reorder" data-oid="${o.orderId || o.id}">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
            <span>Order Again</span>
          </button>
        </div>

        <div class="od-items" style="display:none;">
          ${items.map(renderItem).join("")}
        </div>
      </div>
    </article>`;
}

function renderOrders(orders) {
  if (!orders.length) {
    contentEl.innerHTML = `
      <div class="orders-page-header" data-reveal>
        <h1 class="orders-page-title">My Orders</h1>
        <p class="orders-page-sub">Track and manage your orders</p>
      </div>
      <div class="empty-state">
        <div class="empty-state-icon">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
        </div>
        <h3 class="empty-state-title">No orders yet</h3>
        <p class="empty-state-desc">You haven't placed any orders. Start shopping today!</p>
        <a href="/home.html" class="btn btn-primary" style="margin-top:8px;">Browse Products</a>
      </div>`;
    return;
  }

  contentEl.classList.add("content-loaded");
  contentEl.innerHTML = `
    <div class="orders-page-header" data-reveal>
      <h1 class="orders-page-title">My Orders</h1>
      <p class="orders-page-sub">Track and manage your order history</p>
    </div>
    <div class="od-search">
      <input id="searchInput" class="od-search-input" type="search" placeholder="Search orders by ID, status or product name..." aria-label="Search orders" />
    </div>
    <div class="orders-list">${orders.map(renderCard).join("")}</div>`;

  wireSearch();
  wireReorder();
  wireInvoice();
}

function wireSearch() {
  const el = document.getElementById("searchInput");
  if (!el) return;
  el.addEventListener("input", () => {
    const q = el.value.toLowerCase().trim();
    const filtered = q ? allOrders.filter((o) => {
      const id = shortId(o.orderId || o.id).toLowerCase();
      const raw = (o.orderId || "").toLowerCase();
      const st = (o.status || "").toLowerCase();
      const names = (o.items || []).map((i) => (i.productName || "").toLowerCase()).join(" ");
      return id.includes(q) || raw.includes(q) || st.includes(q) || names.includes(q);
    }).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)) : allOrders;

    const list = contentEl.querySelector(".orders-list");
    if (!list) return;
    list.innerHTML = filtered.length
      ? filtered.map(renderCard).join("")
      : `<div class="empty-state"><p style="color:var(--text-muted);padding:32px 0;">No matching orders found.</p></div>`;
    wireReorder();
    wireInvoice();
  });
}

function wireInvoice() {
  contentEl.querySelectorAll(".od-dl-invoice").forEach((btn) => {
    btn.addEventListener("click", () => {
      const order = allOrders.find((o) => (o.orderId || o.id) === btn.dataset.oid);
      if (!order) return;
      showToast(`Generating Tax Invoice for #${shortId(btn.dataset.oid)}...`, "info");
      downloadReceipt(order);
    });
  });
}

function wireReorder() {
  contentEl.querySelectorAll(".od-reorder").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const order = allOrders.find((o) => (o.orderId || o.id) === btn.dataset.oid);
      if (!order) return;
      btn.disabled = true;
      btn.innerHTML = `<span class="od-spinner"></span>Adding...`;
      try {
        for (const item of order.items) {
          await api.post("/cart", { productId: item.productId, quantity: item.quantity });
        }
        showToast("Items added to cart!", "success");
        setTimeout(() => { window.location.href = "/cart.html"; }, 500);
      } catch {
        showToast("Failed to reorder items", "error");
        btn.disabled = false;
        btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>Reorder`;
      }
    });
  });
}

async function loadHtml2Pdf() {
  if (window.html2pdf) return window.html2pdf;
  return new Promise((resolve, reject) => {
    const s = document.createElement("script");
    s.src = "https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.1/html2pdf.bundle.min.js";
    s.onload = () => resolve(window.html2pdf);
    s.onerror = reject;
    document.head.appendChild(s);
  });
}

async function downloadReceipt(order) {
  const orderId = order.orderId || order.id || "N/A";
  const short = orderId.length > 8 ? orderId.slice(-8).toUpperCase() : orderId.toUpperCase();
  const orderDate = new Date(order.createdAt || Date.now()).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
  const fullDate = new Date(order.createdAt || Date.now()).toLocaleDateString("en-IN", { day: "2-digit", month: "2-digit", year: "numeric" });
  const total = Number(order.totalAmount || 0);
  const subtotal = Math.round(total / 1.18);
  const gst = total - subtotal;
  const items = order.items || [];
  const itemCount = items.length;

  const c = document.createElement("div");
  c.style.cssText = "position:fixed;left:-9999px;top:-9999px;width:800px;background:#fff";
  c.innerHTML = `
    <div style="width:800px;margin:0 auto;background:#fff;border:1px solid #e2e8f0;border-radius:16px;font-family:'Plus Jakarta Sans','Inter',-apple-system,sans-serif;color:#0f172a;overflow:hidden;box-sizing:border-box">
      <div style="background:linear-gradient(135deg,#4f46e5 0%,#4338ca 100%);color:#fff;padding:32px;display:flex;justify-content:space-between;align-items:flex-start">
        <div><div style="font-size:28px;font-weight:800;letter-spacing:-0.03em;line-height:1.1">ShopVibe</div><div style="font-size:11px;font-weight:600;text-transform:uppercase;letter-spacing:0.08em;color:#c7d2fe;margin-top:6px">PREMIUM SHOPPING EXPERIENCE</div><div style="font-size:12px;color:#e0e7ff;margin-top:10px;line-height:1.4">deva.s.professional@gmail.com<br/>+91 93630 90510</div></div>
        <div style="text-align:right"><div style="font-size:22px;font-weight:800;letter-spacing:0.04em;text-transform:uppercase">TAX INVOICE</div><div style="font-size:13px;font-weight:600;color:#c7d2fe;margin-top:4px">INV-${short}</div><div style="font-size:12px;color:#e0e7ff;margin-top:2px">Date: ${orderDate}</div><div style="font-size:12px;color:#e0e7ff">${itemCount} item${itemCount !== 1 ? "s" : ""}</div></div>
      </div>
      <div style="padding:32px">
        <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:32px">
          <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:16px"><div style="font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;color:#64748b;margin-bottom:8px">Billed To</div><div style="font-size:12px;color:#1e293b;line-height:1.6"><strong style="font-size:13px;color:#0f172a;display:block;margin-bottom:4px">Customer Order</strong><span style="color:#475569;word-break:break-word">${order.shippingAddress || "Standard Shipping Address"}</span></div></div>
          <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:16px"><div style="font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;color:#64748b;margin-bottom:8px">Order Details</div><div style="font-size:12px;color:#1e293b;line-height:1.6"><div style="margin-bottom:6px"><span style="color:#64748b;font-size:11px">Order ID:</span><span style="font-family:monospace;font-size:11px;font-weight:700;color:#0f172a;word-break:break-all;display:block;margin-top:2px">${orderId}</span></div><div style="margin-bottom:4px"><span style="color:#64748b;font-size:11px">Date:</span> <strong>${fullDate}</strong></div><div><span style="color:#64748b;font-size:11px">Status:</span> <span style="display:inline-block;padding:2px 8px;border-radius:6px;font-size:10px;font-weight:700;background:#dcfce7;color:#15803d;text-transform:uppercase">${order.status || "CONFIRMED"}</span></div></div></div>
          <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:16px"><div style="font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;color:#64748b;margin-bottom:8px">Payment Information</div><div style="font-size:12px;color:#1e293b;line-height:1.6"><div style="margin-bottom:4px"><span style="color:#64748b;font-size:11px">Method:</span> <strong>${order.paymentMethod || "Online"}</strong></div><div style="margin-bottom:4px"><span style="color:#64748b;font-size:11px">Status:</span> <strong style="color:#15803d">PAID</strong></div><div><span style="color:#64748b;font-size:11px">Amount:</span> <strong style="color:#4f46e5;font-size:13px">${fmt(total)}</strong></div></div></div>
        </div>
        <div style="border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;margin-bottom:24px">
          <table style="width:100%;border-collapse:collapse"><thead><tr style="background:#f1f5f9"><th style="text-align:left;padding:12px 16px;font-size:11px;font-weight:700;color:#475569;text-transform:uppercase;letter-spacing:0.05em;border-bottom:1px solid #e2e8f0">Product Details</th><th style="text-align:right;padding:12px 16px;font-size:11px;font-weight:700;color:#475569;text-transform:uppercase;letter-spacing:0.05em;border-bottom:1px solid #e2e8f0">Price</th><th style="text-align:center;padding:12px 16px;font-size:11px;font-weight:700;color:#475569;text-transform:uppercase;letter-spacing:0.05em;border-bottom:1px solid #e2e8f0">Qty</th><th style="text-align:right;padding:12px 16px;font-size:11px;font-weight:700;color:#475569;text-transform:uppercase;letter-spacing:0.05em;border-bottom:1px solid #e2e8f0">Amount</th></tr></thead><tbody>${items.map((i) => `<tr style="border-bottom:1px solid #f1f5f9"><td style="padding:16px;font-size:13px;color:#334155"><div style="font-size:14px;font-weight:700;color:#0f172a">${i.productName || i.productId || "Product"}</div><div style="font-size:11px;color:#94a3b8;margin-top:2px">SKU: ${i.productId || "-"}</div></td><td style="text-align:right;padding:16px;font-size:13px;color:#334155">${fmt(i.price)}</td><td style="text-align:center;padding:16px;font-size:13px;color:#334155"><strong>${i.quantity}</strong></td><td style="text-align:right;padding:16px;font-size:13px;color:#334155"><strong>${fmt(i.subtotal || i.quantity * i.price)}</strong></td></tr>`).join("")}</tbody></table>
        </div>
        <div style="display:flex;justify-content:flex-end;margin-bottom:32px"><div style="width:320px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:20px"><div style="display:flex;justify-content:space-between;align-items:center;font-size:13px;color:#475569;margin-bottom:10px"><span>Subtotal (${itemCount} item${itemCount !== 1 ? "s" : ""})</span><span>${fmt(subtotal)}</span></div><div style="display:flex;justify-content:space-between;align-items:center;font-size:13px;color:#475569;margin-bottom:10px"><span>GST (18% Included)</span><span>${fmt(gst)}</span></div><div style="height:1px;background:#cbd5e1;margin:12px 0"></div><div style="display:flex;justify-content:space-between;align-items:center;font-size:16px;font-weight:800;color:#0f172a"><span>GRAND TOTAL</span><span style="font-size:20px;font-weight:800;color:#4f46e5">${fmt(total)}</span></div></div></div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:24px;padding-top:24px;border-top:1px solid #e2e8f0;margin-bottom:24px"><div><div style="font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;color:#64748b;margin-bottom:8px">Terms & Conditions</div><ul style="font-size:12px;color:#64748b;list-style:none;padding:0"><li style="margin-bottom:4px">1. This is a computer-generated invoice requiring no signature.</li><li style="margin-bottom:4px">2. Prices are inclusive of all applicable taxes.</li><li style="margin-bottom:4px">3. Returns accepted within 7 days of delivery.</li><li style="margin-bottom:4px">4. Subject to ShopVibe jurisdiction.</li></ul></div><div><div style="font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;color:#64748b;margin-bottom:8px">Need Assistance?</div><ul style="font-size:12px;color:#64748b;list-style:none;padding:0"><li style="margin-bottom:4px">Email: deva.s.professional@gmail.com</li><li style="margin-bottom:4px">Phone: +91 93630 90510</li><li style="margin-bottom:4px">Instagram: @devzz_21</li><li style="margin-bottom:4px">LinkedIn: linkedin.com/in/deva21</li></ul></div></div>
        <div style="text-align:center;padding-top:20px;border-top:1px dashed #cbd5e1;font-size:12px;color:#94a3b8">ShopVibe &nbsp;|&nbsp; Designed & Developed by Deva S &nbsp;|&nbsp; ${new Date().getFullYear()} All rights reserved.</div>
      </div>
    </div>`;

  document.body.appendChild(c);
  showToast("Downloading Tax Invoice PDF...", "info");
  try {
    const html2pdf = await loadHtml2Pdf();
    await html2pdf().set({
      margin: [10, 10, 10, 10],
      filename: `ShopVibe-Invoice-${shortId(orderId)}.pdf`,
      image: { type: "jpeg", quality: 0.98 },
      html2canvas: { scale: 2, useCORS: true, logging: false, width: 800 },
      jsPDF: { unit: "mm", format: "a4", orientation: "portrait" },
    }).from(c.firstElementChild).save();
    showToast("Tax Invoice downloaded!", "success");
  } catch (err) {
    console.error("PDF download failed:", err);
    showToast("Failed to generate PDF.", "error");
  } finally {
    c.parentNode?.removeChild(c);
  }
}

renderFooter();
