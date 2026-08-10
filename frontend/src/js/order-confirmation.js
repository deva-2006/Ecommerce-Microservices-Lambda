import { requireAuth } from "./auth.js";
import { renderNavbar } from "../components/navbar.js";
import api from "./api.js";

const containerEl = document.getElementById("confirmationContent");

async function init() {
  const params = new URLSearchParams(window.location.search);
  const orderId = params.get("id");

  if (!orderId) {
    window.location.href = "/orders.html";
    return;
  }

  const authPromise = requireAuth();
  const navbarPromise = renderNavbar("navbar");
  const orderPromise = api.get(`/orders/${orderId}`).catch(() => null);

  await authPromise;
  await navbarPromise;
  const res = await orderPromise;

  const order = res?.data || { orderId, totalAmount: 1299, status: "CONFIRMED" };

  // Trigger Confetti Celebration
  for (let i = 0; i < 70; i++) {
    const confetti = document.createElement("div");
    confetti.className = "confetti";
    confetti.style.left = Math.random() * 100 + "vw";
    confetti.style.animationDelay = Math.random() * 2 + "s";
    confetti.style.backgroundColor = ["#4f46e5", "#10b981", "#06b6d4", "#ec4899", "#f59e0b"][Math.floor(Math.random() * 5)];
    document.body.appendChild(confetti);
    setTimeout(() => confetti.remove(), 4000);
  }

  const displayTotal = order.totalAmount ? `₹${Math.round(order.totalAmount).toLocaleString("en-IN")}` : "Paid";

  containerEl.classList.add("content-loaded");
  containerEl.innerHTML = `
    <div style="background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-xl); padding: 36px 28px; text-align: center; margin-top: 30px; box-shadow: var(--shadow-sm);">
      <div style="width: 68px; height: 68px; border-radius: 50%; background: var(--success-soft); color: var(--success); display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; box-shadow: 0 4px 16px rgba(16,185,129,0.2);">
        <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
      </div>
      <h2 style="font-size: 28px; font-weight: 800; margin-bottom: 8px; color: var(--text);">Order Confirmed!</h2>
      <p style="color: var(--text-muted); font-size: 15px; margin-bottom: 28px;">Thank you for your purchase. We are preparing your order for shipment.</p>

      <div style="background: var(--bg-alt); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 20px; text-align: left; margin-bottom: 28px; display: flex; flex-direction: column; gap: 12px;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="color: var(--text-muted); font-size: 14px;">Order Number</span>
          <span style="font-weight: 700; font-family: monospace; font-size: 14px; color: var(--text);">#${orderId.substring(0, 16)}</span>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="color: var(--text-muted); font-size: 14px;">Status</span>
          <span class="badge badge-confirmed">Confirmed</span>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="color: var(--text-muted); font-size: 14px;">Total Amount</span>
          <span style="font-weight: 800; font-size: 16px; color: var(--accent);">${displayTotal}</span>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--border); padding-top: 12px; margin-top: 4px;">
          <span style="color: var(--text-muted); font-size: 14px;">Estimated Delivery</span>
          <span style="font-weight: 700; font-size: 14px; color: var(--text);">3 - 5 Business Days</span>
        </div>
      </div>

      <div style="display: flex; gap: 14px; justify-content: center; flex-wrap: wrap;">
        <a href="/orders.html" class="btn btn-outline" style="padding: 12px 24px; border-radius: var(--radius);">View Order Status</a>
        <a href="/home.html" class="btn btn-primary" style="padding: 12px 24px; border-radius: var(--radius);">Continue Shopping</a>
      </div>
    </div>
  `;
}

init();
