import { requireAuth } from "./auth.js";
import { renderNavbar, updateCartBadge } from "../components/navbar.js";
import api, { getProductsCached } from "./api.js";
import { showToast } from "../components/toast.js";
import { renderFooter } from "../components/footer.js";

const contentEl = document.getElementById("cartContent");
const msg = document.getElementById("msg");

let productImageMap = null;

const skItem = `
  <div class="skeleton-cart-item">
    <div class="skeleton skeleton-cart-item-img"></div>
    <div class="skeleton-cart-item-info">
      <div class="skeleton"></div>
      <div class="skeleton"></div>
    </div>
    <div class="skeleton-cart-item-actions">
      <div class="skeleton skeleton-qty"></div>
      <div class="skeleton skeleton-subtotal"></div>
      <div class="skeleton skeleton-remove"></div>
    </div>
  </div>`;
const skCart = `
  <div class="cart-page-header">
    <div class="skeleton skeleton-back-link"></div>
    <div class="skeleton skeleton-title-row">
      <div class="skeleton skeleton-heading"></div>
      <div class="skeleton skeleton-badge"></div>
    </div>
  </div>
  <div class="skeleton-cart-layout">
    <div class="skeleton-cart-items">${skItem.repeat(3)}</div>
    <div class="skeleton-cart-summary-card">
      <div class="skeleton skeleton-title"></div>
      <div class="skeleton skeleton-row"></div>
      <div class="skeleton skeleton-row"></div>
      <div class="skeleton skeleton-row-total"></div>
      <div class="skeleton skeleton-btn"></div>
    </div>
  </div>
`;

contentEl.innerHTML = skCart;
contentEl.style.display = "";

async function buildProductImageMap() {
  if (productImageMap) return productImageMap;
  const res = await getProductsCached();
  const products = (res.data || []).filter(Boolean);
  const map = new Map();
  for (const p of products) {
    if (p.productId) {
      const imgs = (p.imageUrls && p.imageUrls.length > 0) ? p.imageUrls : (p.imageUrl ? [p.imageUrl] : []);
      if (imgs.length > 0) map.set(p.productId, imgs[0]);
    }
  }
  productImageMap = map;
  return map;
}

function resolveImageUrl(item) {
  if (item.imageUrls && item.imageUrls.length > 0) return item.imageUrls[0];
  if (item.imageUrl) return item.imageUrl;
  if (productImageMap && item.productId) return productImageMap.get(item.productId) || "/placeholder.svg";
  return "/placeholder.svg";
}

function renderCart(items) {
  const totalQty = (items || []).reduce((sum, i) => sum + (parseInt(i.quantity) || 1), 0);
  try {
    localStorage.setItem("cache:cart:items", JSON.stringify(items || []));
    localStorage.setItem("cache:cart:count", String(totalQty));
  } catch {}
  updateCartBadge(totalQty);
  contentEl.classList.add("content-loaded");

  if (!items || items.length === 0) {
    contentEl.innerHTML = `
      <div class="cart-page-header" data-reveal>
        <a href="/home.html" class="cart-back-link">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5"/><polyline points="12 19 5 12 12 5"/></svg>
          Continue Shopping
        </a>
        <h2 class="section-title">Shopping Cart</h2>
      </div>
      <div class="empty-state">
        <div class="empty-state-icon">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
        </div>
        <h3 class="empty-state-title">Your cart is empty</h3>
        <p class="empty-state-desc">Looks like you haven't added anything yet. Start exploring our collection!</p>
        <a href="/home.html" class="btn btn-primary" style="margin-top:8px;">Browse Products</a>
      </div>`;
    return;
  }

  let grandTotal = 0;
  let totalItems = 0;

  const itemsHtml = items
    .map((item) => {
      grandTotal += item.totalPrice;
      totalItems += item.quantity;
      return `
        <div class="cart-item" data-product-id="${item.productId}" data-unit-price="${item.price}">
          <a href="/product.html?id=${item.productId}" class="cart-item-img-link">
            <img src="${resolveImageUrl(item)}" alt="${item.productName}" class="cart-item-img" onerror="this.src='/placeholder.svg'" />
          </a>
          <div class="cart-item-info">
            <a href="/product.html?id=${item.productId}" class="cart-item-name">${item.productName}</a>
            <div class="cart-item-unit-price">₹${item.price.toLocaleString()} each</div>
          </div>
          <div class="cart-item-actions">
            <div class="cart-qty-controls">
              <button class="cart-qty-btn qtyMinus" type="button">&minus;</button>
              <input type="number" min="1" value="${item.quantity}" class="cart-qty-input qtyInput" />
              <button class="cart-qty-btn qtyPlus" type="button">&plus;</button>
            </div>
            <div class="cart-item-subtotal">₹${item.totalPrice.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</div>
            <button class="cart-item-remove removeBtn">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              Remove
            </button>
          </div>
        </div>`;
    })
    .join("");

  const summaryHtml = `
    <div class="cart-summary-row">
      <span>Subtotal (${totalItems} item${totalItems > 1 ? "s" : ""})</span>
      <span>₹${grandTotal.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</span>
    </div>
    <div class="cart-summary-row">
      <span>Shipping</span>
      <span class="cart-summary-free">Free</span>
    </div>
    ${grandTotal > 1000 ? `
    <div class="cart-summary-row" style="color: var(--success); font-weight: 500;">
      <span>Savings (10% Off)</span>
      <span>-₹${Math.round(grandTotal * 0.10).toLocaleString("en-IN")}</span>
    </div>` : ''}
    <div class="cart-summary-divider"></div>
    <div class="cart-summary-row total">
      <span>Total</span>
      <span>₹${grandTotal.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</span>
    </div>
    <div style="font-size: 13px; color: var(--text-muted); text-align: center; margin-top: 12px;">
      Estimated delivery: 3-5 days
    </div>`;

  contentEl.innerHTML = `
    <div class="cart-page-header" data-reveal>
      <a href="/home.html" class="cart-back-link">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5"/><polyline points="12 19 5 12 12 5"/></svg>
        Continue Shopping
      </a>
      <div class="cart-page-title-row">
        <h2 class="section-title">Shopping Cart</h2>
        <span class="cart-item-count-badge">${totalItems} item${totalItems > 1 ? "s" : ""}</span>
      </div>
    </div>
    <div class="cart-layout">
      <div class="cart-items-card" id="cartItems">${itemsHtml}</div>
      <div id="cartSummary" class="cart-summary">
        <h3 class="cart-summary-title">Order Summary</h3>
        <div id="cartSummaryRows">${summaryHtml}</div>
        <div class="mt-3">
          <a href="/checkout.html" id="checkoutBtn" class="btn btn-primary btn-block btn-lg" style="text-decoration:none; display:flex; align-items:center; justify-content:center; gap:8px;">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
            Proceed to Checkout
          </a>
        </div>
        <div class="cart-summary-secure">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
          Secure checkout powered by ShopVibe
        </div>
      </div>
    </div>`;

  updateCartBadge(totalItems);

  function updateSummary() {
    let totalItems = 0;
    let grandTotal = 0;
    contentEl.querySelectorAll(".cart-item").forEach((card) => {
      const unitPrice = parseFloat(card.dataset.unitPrice) || 0;
      const qty = parseInt(card.querySelector(".qtyInput").value) || 0;
      const lineTotal = unitPrice * qty;
      card.querySelector(".cart-item-subtotal").textContent = `₹${lineTotal.toLocaleString("en-IN", { minimumFractionDigits: 2 })}`;
      totalItems += qty;
      grandTotal += lineTotal;
    });
    const rows = document.getElementById("cartSummaryRows");
    if (rows) {
      rows.innerHTML = `
        <div class="cart-summary-row">
          <span>Subtotal (${totalItems} item${totalItems > 1 ? "s" : ""})</span>
          <span>₹${grandTotal.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</span>
        </div>
        <div class="cart-summary-row">
          <span>Shipping</span>
          <span class="cart-summary-free">Free</span>
        </div>
        <div class="cart-summary-row" style="color: #10b981; font-weight: 500;">
          <span>Savings</span>
          <span>You're saving ₹500</span>
        </div>
        <div class="cart-summary-divider"></div>
        <div class="cart-summary-row total">
          <span>Total</span>
          <span>₹${grandTotal.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</span>
        </div>
        <div style="font-size: 13px; color: var(--text-muted); text-align: center; margin-top: 12px;">
          Estimated delivery: 3-5 days
        </div>`;
    }
  }

  async function updateItemQuantity(productId, newQty) {
    try {
      const res = await api.put(`/cart/${productId}?quantity=${newQty}`).catch(async () => {
        return await api.post("/cart", { productId, quantity: newQty });
      });
      if (res.data && (res.data.error || res.data.status === "ERROR")) {
        throw new Error(res.data.error || res.data.message || "Failed to update cart");
      }
      await loadCart();
    } catch (err) {
      const text = err.response?.data?.error || err.response?.data?.message || err.message || "Failed to update cart.";
      msg.textContent = text;
      msg.className = "error mt-2";
      showToast(text, "error");
      await loadCart();
    }
  }

  contentEl.querySelectorAll(".qtyMinus").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const card = e.target.closest(".cart-item");
      const input = card.querySelector(".qtyInput");
      const productId = card.dataset.productId;
      const val = parseInt(input.value) || 1;

      if (val > 1) {
        const newQty = val - 1;
        input.value = newQty;
        updateSummary();
        await updateItemQuantity(productId, newQty);
      } else {
        try {
          card.classList.add("swipe-delete");
          await new Promise((r) => setTimeout(r, 300));
          await api.delete(`/cart/${productId}`);
          await loadCart();
        } catch (err) {
          const text = err.response?.data?.error || err.response?.data?.message || err.message || "Remove failed.";
          msg.textContent = text;
          msg.className = "error mt-2";
          showToast(text, "error");
        }
      }
    });
  });

  contentEl.querySelectorAll(".qtyPlus").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const card = e.target.closest(".cart-item");
      const input = card.querySelector(".qtyInput");
      const productId = card.dataset.productId;
      const newQty = (parseInt(input.value) || 0) + 1;

      input.value = newQty;
      updateSummary();
      await updateItemQuantity(productId, newQty);
    });
  });

  contentEl.querySelectorAll(".qtyInput").forEach((input) => {
    input.addEventListener("change", async (e) => {
      const card = e.target.closest(".cart-item");
      const productId = card.dataset.productId;
      const qty = Math.max(1, parseInt(e.target.value) || 1);
      updateSummary();
      await updateItemQuantity(productId, qty);
    });
  });

  contentEl.querySelectorAll(".removeBtn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const card = e.target.closest(".cart-item");
      const productId = card.dataset.productId;
      try {
        card.classList.add("swipe-delete");
        await new Promise((r) => setTimeout(r, 300));
        await api.delete(`/cart/${productId}`);
        await loadCart();
      } catch (err) {
        const text = err.response?.data?.error || err.response?.data?.message || err.message || "Remove failed.";
        msg.textContent = text;
        msg.className = "error mt-2";
        showToast(text, "error");
      }
    });
  });
}

function extractCartItems(resData) {
  if (Array.isArray(resData)) return resData;
  if (resData && Array.isArray(resData.items)) return resData.items;
  if (resData && Array.isArray(resData.cartItems)) return resData.cartItems;
  return [];
}

async function loadCart() {
  try {
    const res = await api.get("/cart");
    const items = extractCartItems(res.data);
    await buildProductImageMap();
    renderCart(items);
  } catch (err) {
    updateCartBadge(0);
    if (err.response?.status === 404) {
      contentEl.innerHTML = `
        <div class="cart-page-header" data-reveal>
          <a href="/home.html" class="cart-back-link">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5"/><polyline points="12 19 5 12 12 5"/></svg>
            Continue Shopping
          </a>
          <div class="cart-page-title-row">
            <h2 class="section-title">Shopping Cart</h2>
            <span class="cart-item-count-badge">0 items</span>
          </div>
        </div>
        <div class="empty-state">
          <div class="empty-state-icon">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
          </div>
          <h3 class="empty-state-title">Your cart is empty</h3>
          <p class="empty-state-desc">Looks like you haven't added anything yet. Start exploring our collection!</p>
          <a href="/home.html" class="btn btn-primary" style="margin-top:8px;">Browse Products</a>
        </div>`;
      msg.textContent = "";
    } else {
      contentEl.style.display = "none";
      msg.textContent = "Failed to load cart — check console.";
    }
  }
}

await Promise.all([requireAuth(), renderNavbar("navbar"), loadCart()]);
renderFooter();
