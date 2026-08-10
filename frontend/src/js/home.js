import { requireAuth } from "./auth.js";
import { renderNavbar, updateCartBadge } from "../components/navbar.js";
import { getProductsCached } from "./api.js";
import { setupHoverPrefetch, prefetchRoutes } from "./prefetch.js";
import { isInWishlist, toggleWishlist } from "./wishlist-helper.js";
import { showToast } from "../components/toast.js";
import { renderFooter } from "../components/footer.js";
import api from "./api.js";

const contentEl = document.getElementById("homeContent");
const msg = document.getElementById("msg");

let allProducts = [];

function getSyncInventoryMap() {
  const map = {};
  try {
    const raw = localStorage.getItem("cache:inventory:items");
    if (raw) {
      const items = JSON.parse(raw);
      if (Array.isArray(items)) {
        items.forEach((item) => {
          if (item && item.productId) {
            map[item.productId] = typeof item.quantity === "number" ? item.quantity : parseInt(item.quantity);
          }
        });
      }
    }
  } catch {}
  return map;
}

const _inventoryMap = getSyncInventoryMap();
const _reviewCache = {};
function getSyncCartMap() {
  const map = new Map();
  try {
    const raw = localStorage.getItem("cache:cart:items");
    if (raw) {
      const items = JSON.parse(raw);
      if (Array.isArray(items)) {
        items.forEach((item) => {
          if (item && item.productId) map.set(item.productId, item);
        });
      }
    }
  } catch {}
  return map;
}

let cartMap = getSyncCartMap();
let slideshowInterval = null;

function getSyncAllProducts() {
  try {
    const raw = localStorage.getItem("cache:products:persistent");
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : (parsed.data || []);
  } catch {
    return [];
  }
}

const syncProducts = getSyncAllProducts();
const skCard = `<div class="skeleton-product-card"><div class="skeleton skeleton-img"></div><div class="skeleton-body"><div class="skeleton" style="height:14px;width:80%"></div><div class="skeleton" style="height:12px;width:60%"></div><div class="skeleton" style="height:12px;width:30%"></div></div></div>`;

if (syncProducts.length > 0) {
  allProducts = syncProducts;
  contentEl.style.display = "";
  renderPage(allProducts);
} else {
  contentEl.innerHTML = `
    <div class="skeleton skeleton-hero"></div>
    <div class="skeleton-product-grid">${skCard.repeat(6)}</div>
  `;
  contentEl.style.display = "";
}

async function fetchCartWithRetry(retries = 3) {
  for (let i = 0; i < retries; i++) {
    try {
      const res = await api.get("/cart");
      return res;
    } catch {
      if (i === retries - 1) return { data: [] };
      await new Promise((r) => setTimeout(r, 600 * (i + 1)));
    }
  }
}

function updateCardActionsInPlace() {
  allProducts.forEach((p) => {
    const card = contentEl.querySelector(`.product-card[data-product-id="${p.productId}"]`);
    if (card) {
      const wrap = card.querySelector(".product-card-action-wrap");
      if (wrap) {
        wrap.innerHTML = renderCardAction(p);
      }
    }
  });
  wireAllCards();
  applyOutofStockToCards();
}

async function refreshCartMap() {
  const res = await fetchCartWithRetry();
  const rawData = res.data;
  const items = Array.isArray(rawData) ? rawData : (rawData?.items || rawData?.cartItems || []);
  try { localStorage.setItem("cache:cart:items", JSON.stringify(items)); } catch {}
  cartMap = new Map();
  let totalQty = 0;
  for (const item of items) {
    if (item && item.productId) {
      cartMap.set(item.productId, item);
      totalQty += (parseInt(item.quantity) || 1);
    }
  }
  updateCartBadge(totalQty);
  updateCardActionsInPlace();
}

function wireCardQtyControls(card) {
  const controls = card.querySelector(".card-qty-controls");
  if (!controls) return;
  const input = controls.querySelector(".card-qty-input");
  const pid = card.dataset.productId;

  controls.querySelector(".card-qty-minus")?.addEventListener("click", async () => {
    const val = parseInt(input.value);
    if (val <= 1) {
      cartMap.delete(pid);
      if (typeof syncCartBadge === "function") syncCartBadge();

      const wrap = card.querySelector(".product-card-action-wrap");
      if (wrap) {
        const stock = _inventoryMap[pid];
        if (stock === 0 || stock < 0) {
          wrap.innerHTML = `<button class="btn btn-secondary btn-sm product-card-action" disabled style="opacity:0.65; cursor:not-allowed; background:var(--surface-2); color:var(--text-muted); border-color:var(--border);">Out of Stock</button>`;
        } else {
          wrap.innerHTML = `<button class="btn btn-primary btn-sm product-card-action add-to-cart-btn" data-product-id="${pid}">Add to Cart</button>`;
          wireAllCards();
        }
      }
      showToast("Removed from cart", "info");

      try {
        await api.delete(`/cart/${pid}`);
      } catch (err) {
        console.error("Failed to delete cart item:", err);
      }
      return;
    }

    const newQty = val - 1;
    input.value = newQty;
    try {
      const res = await api.put(`/cart/${pid}?quantity=${newQty}`);
      if (res.data && (res.data.error || res.data.status === "ERROR")) {
        throw new Error(res.data.error || res.data.message || "Failed to update quantity");
      }
      cartMap.set(pid, { ...cartMap.get(pid), quantity: newQty });
      if (typeof syncCartBadge === "function") syncCartBadge();
    } catch (err) {
      input.value = val;
      const errorMsg = err.response?.data?.error || err.response?.data?.message || err.message || "Failed to update quantity.";
      showToast(errorMsg, "error");
    }
  });

  controls.querySelector(".card-qty-plus")?.addEventListener("click", async () => {
    const val = parseInt(input.value);
    input.value = val + 1;
    try {
      const res = await api.put(`/cart/${pid}?quantity=${input.value}`);
      if (res.data && (res.data.error || res.data.status === "ERROR")) {
        throw new Error(res.data.error || res.data.message || "Failed to update quantity");
      }
      cartMap.set(pid, { ...cartMap.get(pid), quantity: parseInt(input.value) });
    } catch (err) {
      input.value = val;
      const errorMsg = err.response?.data?.error || err.response?.data?.message || err.message || "Failed to update quantity.";
      showToast(errorMsg, "error");
      if (errorMsg.toLowerCase().includes("inventory") || errorMsg.toLowerCase().includes("stock")) {
        _inventoryMap[pid] = 0;
        applyOutofStockToCards();
      }
    }
  });
}

function applyOutofStockToCards() {
  const cards = contentEl ? contentEl.querySelectorAll(".product-card") : document.querySelectorAll(".product-card");
  cards.forEach(card => {
    const pId = card.dataset.productId;
    if (!pId) return;
    const wrap = card.querySelector(".product-card-action-wrap");
    if (!wrap) return;

    const qty = _inventoryMap[pId];
    if (qty === 0 || qty < 0) {
      wrap.innerHTML = `<button class="btn btn-secondary btn-sm product-card-action" disabled style="opacity:0.65; cursor:not-allowed; background:var(--surface-2); color:var(--text-muted); border-color:var(--border);">Out of Stock</button>`;
    }
  });
}

async function fetchAndApplyStockLevels() {
  try {
    const res = await api.get("/inventory");
    const items = res.data || [];
    if (Array.isArray(items)) {
      try { localStorage.setItem("cache:inventory:items", JSON.stringify(items)); } catch {}
      items.forEach(item => {
        if (item && item.productId) {
          _inventoryMap[item.productId] = typeof item.quantity === "number" ? item.quantity : (parseInt(item.quantity) || 0);
        }
      });
    }
    applyOutofStockToCards();
  } catch {}
}

function renderCardAction(p) {
  const stockQty = _inventoryMap[p.productId];
  // Out of Stock check ALWAYS TAKES PRIORITY over cartMap!
  if (stockQty === 0 || stockQty < 0 || (p.stockQty !== undefined && p.stockQty <= 0)) {
    return `<button class="btn btn-secondary btn-sm product-card-action" disabled style="opacity:0.65; cursor:not-allowed; background:var(--surface-2); color:var(--text-muted); border-color:var(--border);">Out of Stock</button>`;
  }
  const cartItem = cartMap.get(p.productId);
  if (cartItem) {
    return `
      <div class="card-qty-controls">
        <button class="card-qty-btn card-qty-minus" type="button">&minus;</button>
        <input type="number" min="1" value="${cartItem.quantity}" class="card-qty-input" />
        <button class="card-qty-btn card-qty-plus" type="button">&plus;</button>
      </div>
      <span class="card-in-cart-label">In cart</span>`;
  }
  return `<button class="btn btn-primary btn-sm product-card-action add-to-cart-btn" data-product-id="${p.productId}">Add to Cart</button>`;
}

function renderProductGrid(products) {
  if (products.length === 0) {
    return `<p style="color:var(--text-muted);text-align:center;padding:40px 0;">No products match your search.</p>`;
  }

  const gridHtml = products
    .map(
      (p) => `
      <div class="product-card" data-product-id="${p.productId}">
        <a href="/product.html?id=${p.productId}" class="product-card-img-link">
          <img src="${p.imageUrl || '/placeholder.svg'}" alt="${p.name}" class="product-card-img" loading="lazy" decoding="async" onerror="this.src='/placeholder.svg'" />
        </a>
        <button class="product-card-wishlist ${isInWishlist(p.productId) ? 'active' : ''}" data-product-id="${p.productId}" aria-label="Wishlist">
          <svg viewBox="0 0 24 24" fill="${isInWishlist(p.productId) ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
          </svg>
        </button>
        <div class="product-card-body">
          ${p.category ? `<span class="product-card-category">${p.category}</span>` : ""}
          <h3 class="product-card-title">${p.name}</h3>
          <p class="product-card-desc">${p.description}</p>
          <div class="product-card-rating">
            <div class="skeleton" style="height:14px; width:80px; border-radius:4px; margin-bottom:4px;"></div>
          </div>
          <div class="product-card-footer">
            <span class="product-card-price">₹${p.price}</span>
            <div class="product-card-action-wrap">${renderCardAction(p)}</div>
          </div>
        </div>
      </div>`
    )
    .join("");

  return gridHtml;
}

async function fetchAndApplyLiveRatings() {
  if (!allProducts || allProducts.length === 0) return;
  const idsToFetch = allProducts.map(p => p.productId).filter(id => id && !_reviewCache[id]);
  
  if (idsToFetch.length > 0) {
    try {
      const res = await api.post("/reviews/summaries", { productIds: idsToFetch });
      if (res && res.data) {
        Object.assign(_reviewCache, res.data);
      }
    } catch {
      // Fallback to individual calls if batch fails
      await Promise.all(idsToFetch.slice(0, 10).map(async (id) => {
        try {
          const r = await api.get(`/reviews/product/${id}/summary`);
          if (r && r.data) _reviewCache[id] = r.data;
        } catch {}
      }));
    }
  }

  allProducts.forEach(p => {
    const cardEl = contentEl.querySelector(`.product-card[data-product-id="${p.productId}"]`);
    if (!cardEl) return;
    const summary = _reviewCache[p.productId];
    
    let ratingEl = cardEl.querySelector(".product-card-rating");
    if (!ratingEl) {
      const bodyEl = cardEl.querySelector(".product-card-body");
      if (bodyEl) {
        const descEl = bodyEl.querySelector(".product-card-desc");
        ratingEl = document.createElement("div");
        ratingEl.className = "product-card-rating";
        if (descEl) descEl.after(ratingEl);
        else bodyEl.appendChild(ratingEl);
      }
    }
    
    if (ratingEl) {
      const total = summary?.totalReviews || 0;
      const avg = summary?.averageRating || 0;
      let stars = "";
      for (let i = 1; i <= 5; i++) {
        stars += `<span style="color:${i <= Math.floor(avg) ? "#f59e0b" : "var(--border, #e2e8f0)"}; font-size:13px;">★</span>`;
      }
      const countLabel = total > 0 ? `${avg.toFixed(1)} (${total})` : `<span style="color:var(--text-muted); font-weight:500;">New</span>`;
      ratingEl.innerHTML = `
        <div class="product-card-rating-stars" style="display:inline-flex; align-items:center;">${stars}</div>
        <span class="product-card-rating-count" style="font-size:12px; font-weight:600; color:var(--text-secondary); margin-left:4px;">${countLabel}</span>
      `;
    }
  });
}

function wireAllCards() {
  contentEl.querySelectorAll(".product-card-wishlist").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.preventDefault();
      const id = btn.dataset.productId;
      const isNowInWishlist = toggleWishlist(id);
      btn.classList.toggle("active", isNowInWishlist);
      const svg = btn.querySelector("svg");
      if (isNowInWishlist) {
        svg.setAttribute("fill", "#ef4444");
        svg.setAttribute("stroke", "#ef4444");
        showToast("Added to Wishlist! ❤️", "success");
      } else {
        svg.setAttribute("fill", "none");
        svg.setAttribute("stroke", "currentColor");
        showToast("Removed from Wishlist", "info");
      }
    });
  });
  setupHoverPrefetch();
  contentEl.querySelectorAll(".add-to-cart-btn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      e.preventDefault();
      const productId = btn.dataset.productId;

      btn.disabled = true;
      btn.innerHTML = '<span class="btn-spinner"></span> Adding...';
      btn.classList.add("btn-loading");

      try {
        const res = await api.post("/cart", { productId, quantity: 1 });
        if (res.data && (res.data.error || res.data.status === "ERROR")) {
          throw new Error(res.data.error || res.data.message || "Failed to add to cart");
        }

        cartMap.set(productId, { productId, quantity: 1 });
        if (typeof syncCartBadge === "function") syncCartBadge();

        btn.classList.remove("btn-loading");
        btn.classList.add("btn-added");
        btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> Added';

        setTimeout(() => {
          const wrap = btn.closest(".product-card-action-wrap");
          if (wrap) {
            wrap.innerHTML = `
              <div class="card-qty-controls">
                <button class="card-qty-btn card-qty-minus" type="button">&minus;</button>
                <input type="number" min="1" value="1" class="card-qty-input" />
                <button class="card-qty-btn card-qty-plus" type="button">&plus;</button>
              </div>
              <span class="card-in-cart-label">In cart</span>`;
            wireCardQtyControls(wrap.closest(".product-card"));
          }
        }, 1000);
      } catch (err) {
        btn.disabled = false;
        btn.classList.remove("btn-loading");
        btn.innerHTML = "Add to Cart";
        const errorMsg = err.response?.data?.error || err.response?.data?.message || err.message || "Failed to add to cart.";
        
        showToast(errorMsg, "error");

        if (errorMsg.toLowerCase().includes("inventory") || errorMsg.toLowerCase().includes("stock")) {
          _inventoryMap[productId] = 0;
          const wrap = btn.closest(".product-card-action-wrap");
          if (wrap) {
            wrap.innerHTML = `<button class="btn btn-secondary btn-sm product-card-action" disabled style="opacity:0.65; cursor:not-allowed; background:var(--surface-2); color:var(--text-muted); border-color:var(--border);">Out of Stock</button>`;
          }
        }
      }
    });
  });

  contentEl.querySelectorAll(".product-card").forEach((card) => {
    card.addEventListener("click", (e) => {
      if (e.target.closest(".product-card-wishlist, .product-card-action-wrap, .add-to-cart-btn, .card-qty-controls, button, input")) {
        return;
      }
      const productId = card.dataset.productId;
      if (productId) {
        window.location.href = `/product.html?id=${encodeURIComponent(productId)}`;
      }
    });

    if (card.querySelector(".card-qty-controls")) {
      wireCardQtyControls(card);
    }
  });

  fetchAndApplyLiveRatings();
  fetchAndApplyStockLevels();
}

function renderPage(products) {
  contentEl.classList.add("content-loaded");
  const gridHtml = renderProductGrid(products);
  
  // Extract unique categories
  const categories = Array.from(new Set(allProducts.map(p => p.category).filter(Boolean)));
  const chipsHtml = `
    <button class="chip active" data-filter="all">All Items</button>
    <button class="chip" data-filter="wishlist" style="color:#ef4444; font-weight:600;">❤️ Wishlist</button>
    ${categories.map(c => `<button class="chip" data-filter="cat:${c.toLowerCase()}">${c}</button>`).join("")}
  `;

  contentEl.innerHTML = `
    <div class="hero">
      <div class="hero-orbs">
        <div class="hero-orb"></div>
        <div class="hero-orb"></div>
        <div class="hero-orb"></div>
      </div>
      <div class="hero-inner">
        <div class="hero-text">
          <h1 class="hero-title">Discover amazing products</h1>
          <p class="hero-subtitle">Browse our curated collection of trending items. Fresh picks, updated in real time.</p>
          <div class="hero-actions">
            <a href="#products" class="btn btn-primary btn-lg">Shop Now ↓</a>
          </div>
        </div>
        <div class="hero-slideshow" id="heroSlideshow"></div>
      </div>
    </div>

    <!-- Shopify Trust Banner -->
    <div class="shopify-trust-banner" style="display:grid; grid-template-columns:repeat(auto-fit, minmax(220px, 1fr)); gap:16px; margin:32px 0 40px; padding:20px; background:var(--surface); border:1px solid var(--border); border-radius:var(--radius-lg);">
      <div style="display:flex; align-items:center; gap:12px;">
        <div style="width:40px; height:40px; border-radius:10px; background:var(--accent-soft); color:var(--accent); display:flex; align-items:center; justify-content:center; font-size:20px;">🚚</div>
        <div>
          <div style="font-weight:700; font-size:13px; color:var(--text);">Free Express Shipping</div>
          <div style="font-size:11px; color:var(--text-muted);">On all orders above ₹999</div>
        </div>
      </div>
      <div style="display:flex; align-items:center; gap:12px;">
        <div style="width:40px; height:40px; border-radius:10px; background:var(--success-soft); color:var(--success); display:flex; align-items:center; justify-content:center; font-size:20px;">⚡</div>
        <div>
          <div style="font-weight:700; font-size:13px; color:var(--text);">Instant Checkout</div>
          <div style="font-size:11px; color:var(--text-muted);">Razorpay, PhonePe, UPI & Cards</div>
        </div>
      </div>
      <div style="display:flex; align-items:center; gap:12px;">
        <div style="width:40px; height:40px; border-radius:10px; background:var(--purple-soft); color:var(--purple); display:flex; align-items:center; justify-content:center; font-size:20px;">🔒</div>
        <div>
          <div style="font-weight:700; font-size:13px; color:var(--text);">Buyer Protection</div>
          <div style="font-size:11px; color:var(--text-muted);">Verified AWS Encryption</div>
        </div>
      </div>
      <div style="display:flex; align-items:center; gap:12px;">
        <div style="width:40px; height:40px; border-radius:10px; background:var(--warning-soft); color:var(--warning); display:flex; align-items:center; justify-content:center; font-size:20px;">🔄</div>
        <div>
          <div style="font-weight:700; font-size:13px; color:var(--text);">7-Day Easy Returns</div>
          <div style="font-size:11px; color:var(--text-muted);">Hassle-free guarantee</div>
        </div>
      </div>
    </div>

    <div class="section-header" id="products" style="display:flex; flex-direction:column; align-items:stretch; gap:12px; margin-bottom:16px; border-bottom:none; padding:0;">
      <div>
        <h2 class="section-title">Featured Products</h2>
        <p class="section-subtitle">Handpicked just for you</p>
      </div>
      <div class="category-chips-strip">
        <div class="category-chips" id="wishlistCategoryChips">
          ${chipsHtml}
        </div>
      </div>
    </div>

    <div id="productList" class="product-grid">${gridHtml}</div>
  `;

  wireAllCards();
  wireWishlistFilter();
  initSlideshow(products);
}

function wireWishlistFilter() {
  const chipsContainer = document.getElementById("wishlistCategoryChips");
  if (!chipsContainer) return;

  chipsContainer.querySelectorAll(".chip").forEach((btn) => {
    btn.addEventListener("click", () => {
      chipsContainer.querySelectorAll(".chip").forEach((c) => c.classList.remove("active"));
      btn.classList.add("active");
      const filter = btn.dataset.filter;
      const listEl = document.getElementById("productList");

      if (filter === "wishlist") {
        const wishlistProducts = allProducts.filter((p) => isInWishlist(p.productId));
        if (!wishlistProducts.length) {
          listEl.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1; padding: 40px 20px;">
              <div class="empty-state-icon" style="font-size: 36px; background: rgba(239,68,68,0.1); color: #ef4444;">❤️</div>
              <h3 class="empty-state-title">Your Wishlist is empty</h3>
              <p class="empty-state-desc">Tap the heart icon on any product to save it here for later!</p>
            </div>`;
          return;
        }
        listEl.innerHTML = renderProductGrid(wishlistProducts);
        wireAllCards();
      } else if (filter.startsWith("cat:")) {
        const catName = filter.replace("cat:", "");
        const filtered = allProducts.filter(p => (p.category || "").toLowerCase() === catName);
        listEl.innerHTML = renderProductGrid(filtered);
        wireAllCards();
      } else {
        listEl.innerHTML = renderProductGrid(allProducts);
        wireAllCards();
      }
    });
  });
}

function filterProducts(query) {
  const q = query.toLowerCase().trim();
  if (!q) return allProducts;
  return allProducts.filter(
    (p) =>
      (p.name || "").toLowerCase().includes(q) ||
      (p.category || "").toLowerCase().includes(q) ||
      (p.description || "").toLowerCase().includes(q)
  );
}

function initSlideshow(products) {
  const container = document.getElementById("heroSlideshow");
  if (!container) return;

  const slideshowProducts = products.slice(0, 5);
  if (slideshowProducts.length === 0) return;

  container.innerHTML = slideshowProducts
    .map(
      (p, i) => `
      <a href="/product.html?id=${p.productId}" class="hero-slide ${i === 0 ? "active" : ""}" data-index="${i}">
        <img src="${p.imageUrl || '/placeholder.svg'}" alt="${p.name}" onerror="this.src='/placeholder.svg'" />
        <div class="hero-slide-info">
          <span class="hero-slide-name">${p.name}</span>
          <span class="hero-slide-price">₹${p.price}</span>
        </div>
      </a>`
    )
    .join("") +
    `<div class="hero-dots">
      ${slideshowProducts.map((_, i) => `<button class="hero-dot ${i === 0 ? "active" : ""}" data-index="${i}"></button>`).join("")}
    </div>`;

  const slides = container.querySelectorAll(".hero-slide");
  const dots = container.querySelectorAll(".hero-dot");
  if (slides.length <= 1) return;
  let current = 0;

  function goTo(index) {
    slides[current].classList.remove("active");
    dots[current].classList.remove("active");
    current = index;
    slides[current].classList.add("active");
    dots[current].classList.add("active");
  }

  function next() {
    goTo((current + 1) % slides.length);
  }

  slideshowInterval = setInterval(next, 4000);

  dots.forEach((dot) => {
    dot.addEventListener("click", (e) => {
      e.preventDefault();
      clearInterval(slideshowInterval);
      goTo(Number(dot.dataset.index));
      slideshowInterval = setInterval(next, 4000);
    });
  });

  // Touch Swipe Support on Mobile
  let touchStartX = 0;
  let touchEndX = 0;

  container.addEventListener("touchstart", (e) => {
    if (e.touches && e.touches.length > 0) {
      touchStartX = e.touches[0].clientX;
    }
    clearInterval(slideshowInterval);
  }, { passive: true });

  container.addEventListener("touchend", (e) => {
    if (e.changedTouches && e.changedTouches.length > 0) {
      touchEndX = e.changedTouches[0].clientX;
      const diffX = touchStartX - touchEndX;
      if (Math.abs(diffX) > 35) {
        if (diffX > 0) {
          goTo((current + 1) % slides.length);
        } else {
          goTo((current - 1 + slides.length) % slides.length);
        }
      }
    }
    slideshowInterval = setInterval(next, 4000);
  }, { passive: true });

  container.addEventListener("mouseenter", () => clearInterval(slideshowInterval));
  container.addEventListener("mouseleave", () => {
    slideshowInterval = setInterval(next, 4000);
  });
}

function applySearchFromUrl() {
  const params = new URLSearchParams(window.location.search);
  const q = params.get("q") || "";
  const category = params.get("category") || "";
  const navInput = document.getElementById("navSearchInput");
  if (navInput) {
    navInput.value = q;
  }
  let filtered = allProducts;
  if (q) filtered = filterProducts(q);
  if (category) filtered = filtered.filter((p) => (p.category || "").toLowerCase() === category.toLowerCase());
  if ((q || category) && allProducts.length) {
    renderPage(filtered);
  }
}

const [, , productsRes] = await Promise.all([
  requireAuth(),
  renderNavbar("navbar"),
  getProductsCached().catch((err) => err),
  fetchAndApplyStockLevels().catch(() => {})
]);

if (productsRes instanceof Error) {
  contentEl.style.display = "none";
  msg.textContent = "Failed to load products — check console.";
} else {
  allProducts = productsRes.data || [];
  prefetchRoutes(['/cart.html', '/product.html', '/profile.html', '/orders.html']);
  renderPage(allProducts);
  applySearchFromUrl();

  // Load cart in background and update card actions in-place
  refreshCartMap().catch(() => {});
}

let searchDebounceTimer = null;
window.addEventListener("search", (e) => {
  clearTimeout(searchDebounceTimer);
  searchDebounceTimer = setTimeout(() => {
    const q = e.detail;
    const results = filterProducts(q);
    if (q && results.length === 0) {
      // Show "no products" state without re-rendering the whole page
      const listEl = document.getElementById("productList");
      if (listEl) {
        listEl.innerHTML = `
          <div class="empty-state" style="grid-column: 1 / -1; padding: 60px 20px;">
            <div class="empty-state-icon" style="font-size: 40px; background: var(--surface-2); color: var(--text-muted);">🔍</div>
            <h3 class="empty-state-title">No products found</h3>
            <p class="empty-state-desc">No results for "<strong>${q}</strong>". Try a different keyword.</p>
          </div>`;
      }
    } else {
      renderPage(results);
    }
  }, 250);
});

// Back to Top Button
renderFooter();

