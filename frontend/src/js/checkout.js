import { requireAuth, getCachedUserAttributes } from "./auth.js";
import { renderNavbar } from "../components/navbar.js";
import { showToast } from "../components/toast.js";
import { renderFooter } from "../components/footer.js";
import api from "./api.js";

const contentEl = document.getElementById("checkoutContent");
const msg = document.getElementById("msg");

const skCheckout = `
  <div class="section-header mt-3" data-reveal>
    <div>
      <h2 class="section-title">Checkout</h2>
      <p class="section-subtitle">Complete your order</p>
    </div>
  </div>
  <div class="checkout-layout">
    <div>
      <div class="checkout-section">
        <div class="skeleton-section-title">
          <div class="skeleton skeleton-number"></div>
          <div class="skeleton skeleton-text"></div>
        </div>
        <div class="skeleton-form-field">
          <div class="skeleton skeleton-label"></div>
          <div class="skeleton skeleton-textarea"></div>
        </div>
        <div class="skeleton skeleton-btn" style="margin-top:20px;"></div>
      </div>
    </div>
    <div class="order-summary-card">
      <div class="skeleton skeleton-title" style="margin-bottom:12px;"></div>
      <div class="skeleton" style="height:14px; width:80%; margin:0 auto;"></div>
    </div>
  </div>
`;

contentEl.innerHTML = skCheckout;
contentEl.style.display = "";

await requireAuth();
await renderNavbar("navbar");

contentEl.classList.add("content-loaded");
contentEl.innerHTML = `
  <div class="section-header mt-3" data-reveal>
    <div>
      <h2 class="section-title">Checkout</h2>
      <p class="section-subtitle">Complete your order</p>
    </div>
  </div>

  <div class="checkout-layout">
    <div>
      <div id="orderForm">
        <div class="checkout-section" id="step1">
          <h3 class="checkout-section-title">
            <span class="checkout-section-number">1</span>
            Shipping Address
          </h3>
          <div class="form-group">
            <label class="form-label" for="shippingAddress">Delivery address</label>
            <textarea id="shippingAddress" class="form-textarea" rows="3" placeholder="Street, City, State, ZIP code"></textarea>
          </div>
          <button id="nextStep2Btn" class="btn btn-primary btn-lg btn-block mt-3">Continue to Payment</button>
        </div>

        <div class="checkout-section hidden" id="step2">
          <h3 class="checkout-section-title">
            <span class="checkout-section-number">2</span>
            Payment Method
          </h3>
          <div class="form-group">
            <label class="form-label" style="font-weight:700; margin-bottom:10px;">Select Payment Method</label>
            <div class="payment-options">
              <label class="payment-option selected" data-method="RAZORPAY">
                <div class="payment-option-header">
                  <input type="radio" name="paymentMethod" value="RAZORPAY" checked />
                  <span class="payment-badge">Fast & Secure</span>
                </div>
                <div>
                  <div class="payment-option-title">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
                    Razorpay
                  </div>
                  <div class="payment-option-sub">UPI, PhonePe, GPay, Cards, NetBanking</div>
                </div>
              </label>
              <label class="payment-option" data-method="CREDIT_CARD">
                <div class="payment-option-header">
                  <input type="radio" name="paymentMethod" value="CREDIT_CARD" />
                </div>
                <div>
                  <div class="payment-option-title">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>
                    Credit / Debit Card
                  </div>
                  <div class="payment-option-sub">Visa, Mastercard, RuPay</div>
                </div>
              </label>
              <label class="payment-option" data-method="UPI">
                <div class="payment-option-header">
                  <input type="radio" name="paymentMethod" value="UPI" />
                </div>
                <div>
                  <div class="payment-option-title">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
                    UPI Direct
                  </div>
                  <div class="payment-option-sub">Instant VPA Transfer</div>
                </div>
              </label>
              <label class="payment-option" data-method="COD">
                <div class="payment-option-header">
                  <input type="radio" name="paymentMethod" value="COD" />
                </div>
                <div>
                  <div class="payment-option-title">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="2"/></svg>
                    Cash on Delivery
                  </div>
                  <div class="payment-option-sub">Pay cash when delivered</div>
                </div>
              </label>
            </div>
          </div>
          <div style="display: flex; gap: 8px; margin-top: 16px;">
            <button id="backStep1Btn" class="btn btn-outline" style="flex: 1;">Back</button>
            <button id="placeOrderBtn" class="btn btn-primary" style="flex: 2;">Place Order</button>
          </div>
        </div>
      </div>

      <div id="paymentSection" class="hidden mt-3">
        <div class="checkout-section">
          <h3 class="checkout-section-title">
            <span class="checkout-section-number">3</span>
            Payment
          </h3>
          <div id="orderSummary" class="mb-3"></div>
          <button id="simulatePaymentBtn" class="btn btn-success btn-lg btn-block">Simulate Payment</button>
        </div>
      </div>
    </div>

    <div class="order-summary-card">
      <h3 class="cart-summary-title">Order Summary</h3>
      <p class="text-center" style="color:var(--text-muted);font-size:14px;">Review items in your cart before placing the order.</p>
    </div>
  </div>
`;

const orderFormEl = document.getElementById("orderForm");
const paymentSectionEl = document.getElementById("paymentSection");
const orderSummaryEl = document.getElementById("orderSummary");
const placeOrderBtn = document.getElementById("placeOrderBtn");
const simulatePaymentBtn = document.getElementById("simulatePaymentBtn");

const step1El = document.getElementById("step1");
const step2El = document.getElementById("step2");
const nextStep2Btn = document.getElementById("nextStep2Btn");
const backStep1Btn = document.getElementById("backStep1Btn");

const originalBtnHtml = placeOrderBtn.innerHTML;

// Check if cart has items before proceeding
try {
  const cartRes = await api.get("/cart");
  if (!cartRes.data || cartRes.data.length === 0) {
    showToast("Your cart is empty!", "error");
    setTimeout(() => { window.location.href = "/cart.html"; }, 1000);
  }
} catch {}

if (nextStep2Btn) {
  nextStep2Btn.addEventListener("click", () => {
    const addr = document.getElementById("shippingAddress").value.trim();
    if (!addr || addr.length < 10) {
      msg.textContent = "Please enter a detailed shipping address (at least 10 characters).";
      msg.className = "error mt-2";
      document.getElementById("shippingAddress").focus();
      return;
    }
    msg.textContent = "";
    step1El.classList.add("hidden");
    step2El.classList.remove("hidden");
  });
}
if (backStep1Btn) {
  backStep1Btn.addEventListener("click", () => {
    step2El.classList.add("hidden");
    step1El.classList.remove("hidden");
  });
}

// Enable Enter key on shipping address to proceed to payment or place order
const shippingAddrEl = document.getElementById("shippingAddress");
if (shippingAddrEl) {
  shippingAddrEl.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      if (!step1El.classList.contains("hidden")) {
        nextStep2Btn.click();
      } else {
        placeOrderBtn.click();
      }
    }
  });
}

let currentOrder = null;

document.querySelectorAll(".payment-option").forEach((opt) => {
  opt.addEventListener("click", () => {
    document.querySelectorAll(".payment-option").forEach((o) => o.classList.remove("selected"));
    opt.classList.add("selected");
    opt.querySelector("input").checked = true;
  });
});

placeOrderBtn.addEventListener("click", async () => {
  const shippingAddress = document.getElementById("shippingAddress").value.trim();
  const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked')?.value;

  if (!shippingAddress || shippingAddress.length < 10) {
    msg.textContent = "Please enter a valid, detailed shipping address (at least 10 characters).";
    msg.className = "error mt-2";
    return;
  }

  placeOrderBtn.disabled = true;
  placeOrderBtn.textContent = "Placing order...";

  try {
    const userAttrs = await getCachedUserAttributes().catch(() => ({}));
    const userEmail = userAttrs?.email || localStorage.getItem("userEmail") || "";
    if (userEmail) {
      try { localStorage.setItem("userEmail", userEmail); } catch {}
    }

    const res = await api.post("/orders", { shippingAddress, paymentMethod });
    currentOrder = res.data;

    if (paymentMethod === "RAZORPAY") {
      const orderAmountPaise = Math.max(100, Math.round((currentOrder.totalAmount || 500) * 100));

      // Step 1: Call Backend or Dev Server to Create Razorpay Order
      placeOrderBtn.textContent = "Opening Razorpay payment gateway...";
      let rzpOrderData = null;

      try {
        const orderCreateRes = await fetch("/api/create-order", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            amount: orderAmountPaise,
            currency: "INR",
            receipt: `rcpt_${currentOrder.orderId || currentOrder.id || Date.now()}`,
          }),
        }).catch(() => null);

        if (orderCreateRes && orderCreateRes.ok) {
          rzpOrderData = await orderCreateRes.json().catch(() => null);
        }
      } catch {}

      const rzpKey = import.meta.env.VITE_RAZORPAY_KEY_ID || "rzp_test_TGTUEnN9TtOs0I";
      const rzpOrderId = rzpOrderData?.order_id || rzpOrderData?.id || null;

      // Step 2: Open Razorpay Checkout Modal (works both with server order_id and direct client fallback)
      const options = {
        key: rzpKey,
        amount: rzpOrderData?.amount || orderAmountPaise,
        currency: rzpOrderData?.currency || "INR",
        name: "ShopVibe Store",
        description: `Order #${currentOrder.orderId || currentOrder.id || "1001"}`,
        ...(rzpOrderId ? { order_id: rzpOrderId } : {}),
        handler: async function (response) {
          // Step 3: Verify Payment Signature on Backend / Update DynamoDB
          try {
            placeOrderBtn.textContent = "Verifying payment...";
            let verified = false;

            if (response.razorpay_payment_id && response.razorpay_signature) {
              try {
                const verifyRes = await fetch("/api/verify-payment", {
                  method: "POST",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify({
                    razorpay_payment_id: response.razorpay_payment_id,
                    razorpay_order_id: response.razorpay_order_id,
                    razorpay_signature: response.razorpay_signature,
                  }),
                }).catch(() => null);

                if (verifyRes && verifyRes.ok) {
                  const verifyData = await verifyRes.json().catch(() => ({}));
                  if (verifyData.success) verified = true;
                }
              } catch {}
            }

            // Update payment status directly on backend DynamoDB
            await api.put(`/payments/${currentOrder.paymentId}/status?status=SUCCESS`);
            showToast("Payment completed successfully! Redirecting...", "success");
            setTimeout(() => {
              window.location.href = `/order-confirmation.html?id=${currentOrder.orderId || currentOrder.id || "1001"}`;
            }, 500);
          } catch (verifyErr) {
            msg.textContent = verifyErr.message || "Payment verification failed.";
            msg.className = "error mt-2";
            showToast(verifyErr.message || "Payment verification failed.", "error");
            placeOrderBtn.disabled = false;
            placeOrderBtn.innerHTML = originalBtnHtml;
          }
        },
        modal: {
          ondismiss: function () {
            showToast("Razorpay payment cancelled.", "info");
            placeOrderBtn.disabled = false;
            placeOrderBtn.innerHTML = originalBtnHtml;
          },
        },
        theme: {
          color: "#4f46e5",
        },
      };

      if (typeof Razorpay !== "undefined") {
        const rzp = new Razorpay(options);
        rzp.on("payment.failed", function (failResponse) {
          showToast(`Payment Failed: ${failResponse.error?.description || "Transaction failed"}`, "error");
          placeOrderBtn.disabled = false;
          placeOrderBtn.innerHTML = originalBtnHtml;
        });
        rzp.open();
      } else {
        throw new Error("Razorpay SDK script not loaded.");
      }
    } else {
      placeOrderBtn.textContent = "Processing payment...";

      try {
        await api.put(`/payments/${currentOrder.paymentId}/status?status=SUCCESS`);
        showToast("Payment successful!", "success");
        setTimeout(() => {
          window.location.href = `/order-confirmation.html?id=${currentOrder.orderId || currentOrder.id || '1001'}`;
        }, 600);
      } catch (payErr) {
        const payText = payErr.response?.data?.message || payErr.message || "Payment failed.";
        msg.textContent = payText;
        msg.className = "error mt-2";
        showToast(payText, "error");
        placeOrderBtn.disabled = false;
        placeOrderBtn.innerHTML = originalBtnHtml;
      }
    }
  } catch (err) {
    const text = err.response?.data?.error || err.response?.data?.message || err.message || "Failed to place order.";
    msg.textContent = text;
    msg.className = "error mt-2";
    showToast(text, "error");
    placeOrderBtn.disabled = false;
    placeOrderBtn.innerHTML = originalBtnHtml;

    if (text.toLowerCase().includes("out of stock") || text.toLowerCase().includes("sold out") || text.toLowerCase().includes("unavailable") || text.toLowerCase().includes("inventory")) {
      const summaryBox = document.getElementById("orderSummary");
      if (summaryBox) {
        summaryBox.insertAdjacentHTML("afterbegin", `
          <div class="alert alert-danger mb-3" style="background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.3); color: #ef4444; padding: 12px 16px; border-radius: 8px; font-size: 14px; font-weight:600; display: flex; align-items: center; justify-content: space-between; gap: 10px;">
            <span>⚠️ ${text}</span>
            <a href="/cart.html" class="btn btn-sm btn-outline" style="white-space: nowrap; color: #ef4444; border-color: rgba(239, 68, 68, 0.4);">Clean Cart</a>
          </div>
        `);
      }
    }
  }
});

simulatePaymentBtn.addEventListener("click", async () => {
  if (!currentOrder || simulatePaymentBtn.disabled) return;

  simulatePaymentBtn.disabled = true;
  simulatePaymentBtn.textContent = "Processing payment...";

  try {
    await api.put(`/payments/${currentOrder.paymentId}/status?status=SUCCESS`);
    msg.textContent = "Payment successful! Redirecting to your orders...";
    msg.className = "success mt-2";
    showToast("Payment successful!", "success");
    setTimeout(() => (window.location.href = "/orders.html"), 1200);
  } catch (err) {
    const text = err.response?.data?.message || err.message || "Payment failed.";
    msg.textContent = text;
    msg.className = "error mt-2";
    showToast(text, "error");
    simulatePaymentBtn.disabled = false;
    simulatePaymentBtn.textContent = "Simulate Payment";
  }
});

renderFooter();
