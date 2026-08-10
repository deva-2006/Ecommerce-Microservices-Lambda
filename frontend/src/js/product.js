import { requireAuth } from "./auth.js";
import { renderNavbar, updateCartBadge } from "../components/navbar.js";
import api, { getProductsCached } from "./api.js";
import { isInWishlist, toggleWishlist } from "./wishlist-helper.js";
import { showToast } from "../components/toast.js";
import { renderFooter } from "../components/footer.js";

const contentEl = document.getElementById("productContent");
const msg = document.getElementById("msg");

async function refreshCartBadge() {
  try {
    const res = await api.get("/cart");
    const items = res?.data || [];
    const total = Array.isArray(items) ? items.reduce((s, i) => s + (parseInt(i.quantity) || 1), 0) : 0;
    updateCartBadge(total);
  } catch {}
}

function resolveProductImages(p) {
  if (p.imageUrls && p.imageUrls.length > 0) return p.imageUrls;
  if (p.imageUrl) return [p.imageUrl];
  return ["/placeholder.svg"];
}

const params = new URLSearchParams(window.location.search);
const productId = params.get("id");

const skProduct = `
  <div class="skeleton-detail-layout">
    <div class="skeleton skeleton-detail-img"></div>
    <div class="skeleton-detail-info">
      <div class="skeleton"></div>
      <div class="skeleton"></div>
      <div class="skeleton"></div>
      <div class="skeleton"></div>
    </div>
  </div>
`;

function renderRelated(currentProduct, allProducts) {
  const others = allProducts.filter((p) => p.productId !== productId);
  if (others.length === 0) return;

  const sameCategory = others.filter((p) => p.category && p.category === currentProduct.category);
  const rest = others.filter((p) => !p.category || p.category !== currentProduct.category);

  for (let i = rest.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [rest[i], rest[j]] = [rest[j], rest[i]];
  }

  const related = [...sameCategory, ...rest].slice(0, 8);
  if (related.length === 0) return;

  const relatedGrid = related
    .map(
      (p) => {
        const imgs = resolveProductImages(p);
        return `
      <div class="product-card">
        <a href="/product.html?id=${p.productId}" class="product-card-img-link">
          <img src="${imgs[0]}" alt="${p.name}" class="product-card-img" onerror="this.src='/placeholder.svg'" />
        </a>
        <div class="product-card-body">
          ${p.category ? `<span class="product-card-category">${p.category}</span>` : ""}
          <h3 class="product-card-title">${p.name}</h3>
          <p class="product-card-desc">${p.description}</p>
          <div class="product-card-footer">
            <span class="product-card-price">₹${p.price}</span>
            <a href="/product.html?id=${p.productId}" class="btn btn-primary btn-sm product-card-action">View Details</a>
          </div>
        </div>
      </div>`;
      }
    )
    .join("");

  contentEl.insertAdjacentHTML("beforeend", `
    <section class="mt-4 mb-4">
      <div class="section-header">
        <h3 class="section-title">You may also like</h3>
      </div>
      <div class="product-grid">${relatedGrid}</div>
    </section>
  `);
}

if (!productId) {
  contentEl.style.display = "none";
  msg.textContent = "No product ID provided.";
} else {
  renderNavbar("navbar");
  requireAuth().catch(() => {});

  function getSyncProduct(id) {
    try {
      const raw = localStorage.getItem("cache:products:persistent");
      if (!raw) return null;
      const parsed = JSON.parse(raw);
      const items = Array.isArray(parsed) ? parsed : (parsed.data || []);
      return items.find((p) => p.productId === id) || null;
    } catch {
      return null;
    }
  }

  const syncP = getSyncProduct(productId);
  if (syncP) {
    contentEl.style.display = "";
    renderProductDetails(syncP, null, []);
  } else {
    contentEl.innerHTML = skProduct;
    contentEl.style.display = "";
  }

  async function initProductPage() {
    let p = syncP;
    if (!p) {
      const pRes = await api.get(`/products/${productId}`).catch(() => null);
      if (pRes && pRes.data) p = pRes.data;
    }

    if (!p) {
      contentEl.style.display = "none";
      msg.innerHTML = `
        <div style="text-align:center; padding:60px 20px;">
          <div style="font-size:48px; margin-bottom:12px;">🛍️</div>
          <h2 style="font-size:22px; font-weight:700; color:var(--text-primary); margin-bottom:8px;">Product Not Found</h2>
          <p style="font-size:14px; color:var(--text-secondary); margin-bottom:20px;">This product is no longer available or may have been deleted.</p>
          <a href="/home.html" class="btn btn-primary">&larr; Back to Products</a>
        </div>`;
      return;
    }

    if (!syncP) {
      renderProductDetails(p, null, []);
    }

    Promise.all([
      api.get(`/inventory/${productId}`).catch(() => null),
      api.get("/cart").catch(() => ({ data: [] })),
      getProductsCached().catch(() => ({ data: [] }))
    ]).then(([invRes, cartRes, prodsRes]) => {
      let stockQty = 0;
      if (invRes && invRes.data && typeof invRes.data.quantity === "number") {
        stockQty = invRes.data.quantity;
      }
      const cartItems = (cartRes && cartRes.data) || [];
      
      updateProductControls(p, stockQty, cartItems);
      renderRelated(p, (prodsRes && prodsRes.data) || []);
      loadProductReviews(productId);
    });
  }

  function resolveHighlights(p) {
    if (Array.isArray(p.highlights) && p.highlights.length > 0) {
      return p.highlights.map((h) => h.trim()).filter(Boolean);
    }
    if (typeof p.highlights === "string" && p.highlights.trim()) {
      return p.highlights.split(/,|\n/).map((h) => h.trim()).filter(Boolean);
    }
    if (p.description) {
      const lines = p.description
        .split(/\r?\n|•|\*|✓|-/)
        .map((s) => s.trim())
        .filter((s) => s.length >= 4 && s.length <= 50);
      if (lines.length >= 2) return lines.slice(0, 6);
    }
    const cat = (p.category || "").toLowerCase();
    if (cat.includes("electro") || cat.includes("tech") || cat.includes("phone") || cat.includes("laptop")) {
      return [
        "1 Year Brand Warranty",
        "100% Genuine & Sealed Pack",
        "Express Insured Delivery",
        "7-Day Easy Replacement"
      ];
    } else if (cat.includes("fashion") || cat.includes("cloth") || cat.includes("shoe")) {
      return [
        "Premium Comfort Fabric",
        "True-to-Size Standard Fit",
        "7-Day Hassle-Free Returns",
        "Fast & Free Shipping"
      ];
    }
    return [
      "100% Quality Inspected",
      "Official Brand Warranty",
      "Express Doorstep Delivery",
      "7-Day Easy Return Guarantee"
    ];
  }

  function renderActionControls(inStock, inCart, cartQty, productId) {
    if (!inStock) {
      return `<button id="addToCartBtn" class="btn btn-secondary" disabled style="height:48px; width:100%; border-radius:9999px; opacity:0.65; cursor:not-allowed; font-weight:700; font-size:15px; background:var(--surface-2); color:var(--text-muted); border:1px solid var(--border);">Out of Stock</button>`;
    }

    const topRowHtml = inCart
      ? `<div style="display:flex; align-items:center; gap:10px; width:100%;">
          <div class="cart-qty-controls product-qty-controls" style="height:48px; margin:0; flex:1; display:flex; align-items:center; justify-content:space-between; padding:0 16px; border-radius:9999px; border:1px solid var(--border); background:var(--surface);">
            <button class="cart-qty-btn qtyMinus" type="button" style="width:36px; height:36px; font-size:18px; font-weight:800; cursor:pointer; border-radius:50%; border:none; background:var(--surface-2); color:var(--text);" aria-label="Decrease Quantity">&minus;</button>
            <span class="qtyDisplay" style="font-weight:800; font-size:15px; color:var(--text);">${cartQty} in Cart</span>
            <button class="cart-qty-btn qtyPlus" type="button" style="width:36px; height:36px; font-size:18px; font-weight:800; cursor:pointer; border-radius:50%; border:none; background:var(--surface-2); color:var(--text);" aria-label="Increase Quantity">&plus;</button>
          </div>
          <button id="removeFromCartBtn" class="btn btn-outline" type="button" style="height:48px; padding:0 20px; border-radius:9999px; color:#ef4444; border-color:rgba(239,68,68,0.3); font-weight:600; display:flex; align-items:center; gap:6px;">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            Remove
          </button>
        </div>`
      : `<div style="display:flex; align-items:center; gap:10px; width:100%;">
          <button id="addToCartBtn" class="btn btn-primary" type="button" style="flex:1; height:48px; border-radius:9999px; font-weight:700; font-size:15px; background:linear-gradient(135deg, #6366f1 0%, #4f46e5 100%); color:#fff; border:none; box-shadow:0 4px 16px rgba(99,102,241,0.35); cursor:pointer; display:flex; align-items:center; justify-content:center; gap:8px;">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
            Add to Cart
          </button>
        </div>`;

    return `
      <div style="display:flex; flex-direction:column; gap:12px; width:100%;">
        ${topRowHtml}
        <div style="display:flex; align-items:center; gap:10px; width:100%; flex-wrap:wrap;">
          <button id="buyNowBtn" class="btn btn-accent" type="button" style="flex:1; min-width:120px; height:48px; border-radius:9999px; font-weight:700; font-size:15px; background:linear-gradient(135deg, #10b981 0%, #059669 100%); color:#fff; border:none; box-shadow:0 4px 16px rgba(16,185,129,0.35); cursor:pointer; display:flex; align-items:center; justify-content:center; gap:8px;">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
            Buy Now
          </button>
          <button id="productWishlistBtn" class="btn btn-outline ${isInWishlist(productId) ? "active" : ""}" type="button" style="height:48px; padding:0 14px; border-radius:9999px; font-weight:600; display:flex; align-items:center; gap:6px; flex-shrink:0; ${isInWishlist(productId) ? "color:#ef4444; border-color:rgba(239,68,68,0.4);" : ""}" aria-label="Save to Wishlist">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="${isInWishlist(productId) ? "#ef4444" : "none"}" stroke="${isInWishlist(productId) ? "#ef4444" : "currentColor"}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
            </svg>
            ${isInWishlist(productId) ? "Saved" : "Wishlist"}
          </button>
          <button id="shareBtn" class="btn btn-outline" type="button" style="height:48px; padding:0 14px; border-radius:9999px; font-weight:600; display:flex; align-items:center; gap:6px; flex-shrink:0;" aria-label="Share product link">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><polyline points="16 6 12 2 8 6"/><line x1="12" y1="2" x2="12" y2="15"/></svg>
            Share
          </button>
        </div>
      </div>`;
  }

  function renderProductDetails(p, stockQty, cartItems) {
    contentEl.classList.add("content-loaded");
    const inStock = stockQty !== null && stockQty !== undefined ? stockQty > 0 : true;
    const cartItem = cartItems.find((ci) => ci.productId === productId);
    const inCart = !!cartItem;
    const cartQty = cartItem ? cartItem.quantity : 0;
    const images = resolveProductImages(p);
    const hasMultiple = images.length > 1;
    const highlights = resolveHighlights(p);

    const galleryHtml = hasMultiple
      ? `<div class="product-gallery">
          <div class="product-gallery-main">
            <img src="${images[0]}" alt="${p.name}" class="product-detail-img" id="galleryMainImg" onerror="this.src='/placeholder.svg'" />
            <button type="button" class="gallery-nav-btn gallery-prev" id="galleryPrevBtn" aria-label="Previous Image">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
            </button>
            <button type="button" class="gallery-nav-btn gallery-next" id="galleryNextBtn" aria-label="Next Image">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
            </button>
            <div class="gallery-counter" id="galleryCounter">1 / ${images.length}</div>
          </div>
          <div class="product-gallery-thumbs">
            ${images.map((url, i) => `
              <button class="product-gallery-thumb ${i === 0 ? "active" : ""}" data-gallery-index="${i}" data-gallery-url="${url}">
                <img src="${url}" alt="${p.name} ${i + 1}" onerror="this.src='/placeholder.svg'" />
              </button>
            `).join("")}
          </div>
        </div>`
      : `<img src="${images[0]}" alt="${p.name}" class="product-detail-img" onerror="this.src='/placeholder.svg'" />`;

    contentEl.innerHTML = `
      <nav style="margin-bottom:20px; display:flex; align-items:center; gap:8px; font-size:13px; color:var(--text-muted);">
        <a href="/home.html" style="display:inline-flex; align-items:center; gap:5px; color:var(--text-muted); text-decoration:none; font-weight:500; padding:6px 14px; border-radius:9999px; background:var(--surface-2); border:1px solid var(--border); transition:color 0.2s, border-color 0.2s;">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
          Products
        </a>
        <span style="color:var(--border);">›</span>
        <span style="color:var(--text); font-weight:600; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; max-width:200px;">${p.name}</span>
      </nav>
      <div class="product-detail">
        <div>
          ${galleryHtml}
        </div>
        <div class="product-detail-info">
          ${p.category ? `<span class="product-detail-category">${p.category}</span>` : ""}
          <h1 class="product-detail-title">${p.name}</h1>
          <p class="product-detail-desc">${p.description}</p>
          <p class="product-detail-price">₹${p.price}</p>
          <div id="productStockBadge" class="product-detail-stock ${stockQty === 0 ? "out-of-stock" : (inStock ? "in-stock" : "out-of-stock")}" style="margin-bottom:16px;">
            <span class="stock-dot"></span>
            <span id="stockText">${stockQty === 0 ? "Out of Stock" : (stockQty !== null ? (inStock ? `${stockQty} in stock` : "Out of stock") : "Checking stock...")}</span>
          </div>

          <!-- 1. PRODUCT HIGHLIGHTS -->
          <div class="product-highlights-card" style="margin-bottom:16px; padding:16px; background:var(--surface-2); border:1px solid var(--border-light); border-radius:var(--radius-lg); box-shadow:var(--shadow-sm);">
            <div style="font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:0.06em; color:var(--accent); margin-bottom:12px; display:flex; align-items:center; gap:6px;">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3Z"/></svg>
              Product Highlights
            </div>
            <div style="display:grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap:10px;">
              ${highlights.map((h) => `
                <div style="display:flex; align-items:center; gap:8px; font-size:13px; font-weight:600; color:var(--text-primary);">
                  <span style="display:inline-flex; align-items:center; justify-content:center; width:20px; height:20px; border-radius:50%; background:var(--accent-soft); color:var(--accent); font-weight:800; font-size:11px; flex-shrink:0;">✓</span>
                  <span>${h}</span>
                </div>
              `).join("")}
            </div>
          </div>

          <!-- 2. DELIVERY & RETURNS TRUST CARD -->
          <div class="product-trust-card" style="margin-bottom:16px; display:grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap:8px; padding:16px; background:var(--surface); border:1px solid var(--border-light); border-radius:var(--radius-lg); box-shadow:var(--shadow-sm);">
            <div style="display:flex; align-items:center; gap:10px; padding:4px;">
              <div style="width:36px; height:36px; border-radius:50%; background:var(--accent-soft); color:var(--accent); display:flex; align-items:center; justify-content:center; flex-shrink:0;">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
              </div>
              <div>
                <div style="font-size:12px; font-weight:700; color:var(--text-primary);">Free Delivery</div>
                <div style="font-size:11px; color:var(--text-muted);">By Tomorrow</div>
              </div>
            </div>
            <div style="display:flex; align-items:center; gap:10px; padding:4px; border-left:1px solid var(--border-light); border-right:1px solid var(--border-light); padding-left:12px;">
              <div style="width:36px; height:36px; border-radius:50%; background:var(--accent-soft); color:var(--accent); display:flex; align-items:center; justify-content:center; flex-shrink:0;">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/></svg>
              </div>
              <div>
                <div style="font-size:12px; font-weight:700; color:var(--text-primary);">7-Day Returns</div>
                <div style="font-size:11px; color:var(--text-muted);">Easy Replacement</div>
              </div>
            </div>
            <div style="display:flex; align-items:center; gap:10px; padding:4px; padding-left:12px;">
              <div style="width:36px; height:36px; border-radius:50%; background:var(--accent-soft); color:var(--accent); display:flex; align-items:center; justify-content:center; flex-shrink:0;">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
              </div>
              <div>
                <div style="font-size:12px; font-weight:700; color:var(--text-primary);">1 Year Warranty</div>
                <div style="font-size:11px; color:var(--text-muted);">Brand Coverage</div>
              </div>
            </div>
          </div>

          <!-- 3. AVAILABLE OFFERS -->
          <div class="product-offers-card" style="margin-bottom:20px; padding:14px 16px; background:color-mix(in srgb, var(--accent) 6%, var(--surface-2)); border:1px dashed var(--accent); border-radius:var(--radius-lg);">
            <div style="font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--accent); margin-bottom:8px; display:flex; align-items:center; gap:6px;">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
              Available Offers & Bank Discounts
            </div>
            <ul style="margin:0; padding-left:18px; font-size:12px; color:var(--text-secondary); display:flex; flex-direction:column; gap:5px;">
              <li><strong style="color:var(--text-primary);">Bank Offer:</strong> 10% Instant Discount on HDFC & ICICI Bank Credit Cards</li>
              <li><strong style="color:var(--text-primary);">No Cost EMI:</strong> Options available starting from ₹7,491/month</li>
              <li><strong style="color:var(--text-primary);">Exchange Bonus:</strong> Extra savings on product exchange</li>
            </ul>
          </div>

          <!-- 4. ACTION BAR -->
          <div id="productActionsWrap" class="product-detail-actions">
            ${renderActionControls(inStock, inCart, cartQty, productId)}
          </div>
          <p id="addToCartMsg" class="mt-2"></p>
        </div>
      </div>

      <!-- 5. VERIFIED BUYER REVIEWS SECTION -->
      <section id="reviewsSection" style="margin-top:40px; padding-top:28px; border-top:1px solid var(--border-light);">
        <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:20px;">
          <h3 style="font-size:20px; font-weight:800; color:var(--text-primary); margin:0;">
            Customer Reviews & Ratings
          </h3>
        </div>
        <div id="reviewsContainer">
          <div style="padding:20px; text-align:center; color:var(--text-muted);">Loading ratings & reviews...</div>
        </div>
      </section>`;

    if (hasMultiple) {
      let currentIndex = 0;
      const mainImg = document.getElementById("galleryMainImg");
      const counterEl = document.getElementById("galleryCounter");
      const thumbs = contentEl.querySelectorAll(".product-gallery-thumb");

      function setActiveGalleryImage(index) {
        currentIndex = (index + images.length) % images.length;
        if (mainImg) mainImg.src = images[currentIndex] || "/placeholder.svg";
        if (counterEl) counterEl.textContent = `${currentIndex + 1} / ${images.length}`;
        thumbs.forEach((t, i) => {
          if (i === currentIndex) t.classList.add("active");
          else t.classList.remove("active");
        });
      }

      thumbs.forEach((thumb) => {
        thumb.addEventListener("click", () => {
          const idx = parseInt(thumb.dataset.galleryIndex) || 0;
          setActiveGalleryImage(idx);
        });
      });

      document.getElementById("galleryPrevBtn")?.addEventListener("click", (e) => {
        e.preventDefault();
        setActiveGalleryImage(currentIndex - 1);
      });

      document.getElementById("galleryNextBtn")?.addEventListener("click", (e) => {
        e.preventDefault();
        setActiveGalleryImage(currentIndex + 1);
      });

      const handleKeyNavigation = (e) => {
        if (["INPUT", "TEXTAREA", "SELECT"].includes(document.activeElement?.tagName)) return;
        if (e.key === "ArrowLeft") {
          e.preventDefault();
          setActiveGalleryImage(currentIndex - 1);
        } else if (e.key === "ArrowRight") {
          e.preventDefault();
          setActiveGalleryImage(currentIndex + 1);
        }
      };

      if (window._productKeyNavHandler) {
        document.removeEventListener("keydown", window._productKeyNavHandler);
      }
      window._productKeyNavHandler = handleKeyNavigation;
      document.addEventListener("keydown", window._productKeyNavHandler);
    }

    bindCartActions(p, inStock, inCart, cartQty, stockQty);
  }

  async function loadProductReviews(pid) {
    const container = document.getElementById("reviewsContainer");
    if (!container) return;

    try {
      const [summaryRes, reviewsRes, verifyRes] = await Promise.all([
        api.get(`/reviews/product/${pid}/summary`).catch(() => null),
        api.get(`/reviews/product/${pid}`).catch(() => null),
        api.get(`/orders/verify-purchase?productId=${pid}`).catch(() => ({ data: { purchased: false } }))
      ]);

      const summary = summaryRes?.data || { averageRating: 0.0, totalReviews: 0, ratingBreakdown: {} };
      const reviews = reviewsRes?.data || [];
      const isVerifiedBuyer = !!verifyRes?.data?.purchased;

      const avg = summary.averageRating || 0.0;
      const total = summary.totalReviews || 0;
      const breakdown = summary.ratingBreakdown || {};

      const renderStars = (score) => {
        const full = Math.floor(score);
        let stars = "";
        for (let i = 1; i <= 5; i++) {
          if (i <= full) stars += `<span style="color:#f59e0b; font-size:16px;">★</span>`;
          else stars += `<span style="color:var(--border); font-size:16px;">★</span>`;
        }
        return stars;
      };

      // Review Submission Card
      const reviewFormHtml = isVerifiedBuyer
        ? `<div style="background:var(--surface); border:1px solid var(--border-light); border-radius:var(--radius-lg); padding:20px; margin-bottom:24px; box-shadow:var(--shadow-sm);">
            <div style="font-size:15px; font-weight:700; color:var(--text-primary); margin-bottom:12px; display:flex; align-items:center; gap:8px;">
              <span>Write a Customer Review</span>
              <span style="font-size:11px; font-weight:700; background:rgba(16,185,129,0.12); color:#10b981; padding:2px 8px; border-radius:12px;">✓ Verified Buyer</span>
            </div>
            <form id="writeReviewForm" style="display:flex; flex-direction:column; gap:12px;">
              <div style="display:flex; align-items:center; gap:12px;">
                <label style="font-size:13px; font-weight:600; color:var(--text-secondary);">Rating:</label>
                <div id="starPicker" style="display:flex; gap:4px; cursor:pointer;">
                  ${[1, 2, 3, 4, 5].map((s) => `<span class="star-opt" data-star="${s}" style="font-size:22px; color:${s <= 5 ? "#f59e0b" : "var(--border)"}; transition:transform 0.15s;">★</span>`).join("")}
                </div>
                <input type="hidden" id="reviewRatingInput" value="5" />
              </div>
              <textarea id="reviewCommentInput" rows="3" placeholder="Share your experience with this product..." required style="width:100%; padding:10px 14px; border-radius:var(--radius-md); border:1px solid var(--border-light); background:var(--surface-2); color:var(--text-primary); font-size:13px; font-family:inherit; resize:vertical;"></textarea>
              <div style="display:flex; justify-content:flex-end;">
                <button type="submit" id="submitReviewBtn" class="btn btn-primary btn-sm" style="font-weight:700; padding:8px 20px;">Submit Review</button>
              </div>
            </form>
          </div>`
        : `<div style="background:var(--surface-2); border:1px dashed var(--border-light); border-radius:var(--radius-lg); padding:16px 20px; margin-bottom:24px; display:flex; align-items:center; justify-content:space-between; gap:12px;">
            <div style="display:flex; align-items:center; gap:10px;">
              <div style="width:36px; height:36px; border-radius:50%; background:var(--surface); display:flex; align-items:center; justify-content:center; font-size:18px;">🔒</div>
              <div>
                <div style="font-size:13px; font-weight:700; color:var(--text-primary);">Verified Buyers Only</div>
                <div style="font-size:12px; color:var(--text-muted);">Only customers who have purchased this product can leave a review.</div>
              </div>
            </div>
          </div>`;

      // Rating Summary Bar
      const summaryHtml = `
        <div style="display:grid; grid-template-columns: 200px 1fr; gap:24px; background:var(--surface); border:1px solid var(--border-light); border-radius:var(--radius-lg); padding:20px; margin-bottom:24px; box-shadow:var(--shadow-sm);">
          <div style="text-align:center; display:flex; flex-direction:column; align-items:center; justify-content:center; border-right:1px solid var(--border-light); padding-right:20px;">
            <div style="font-size:42px; font-weight:900; color:var(--text-primary); line-height:1;">${avg.toFixed(1)}</div>
            <div style="margin:6px 0;">${renderStars(avg)}</div>
            <div style="font-size:12px; font-weight:600; color:var(--text-muted);">${total} ${total === 1 ? "rating" : "ratings"}</div>
          </div>
          <div style="display:flex; flex-direction:column; justify-content:center; gap:6px;">
            ${[5, 4, 3, 2, 1].map((s) => {
              const count = breakdown[s] || 0;
              const pct = total > 0 ? Math.round((count / total) * 100) : 0;
              return `
                <div style="display:flex; align-items:center; gap:10px; font-size:12px;">
                  <span style="width:24px; font-weight:700; color:var(--text-secondary); text-align:right;">${s}★</span>
                  <div style="flex:1; height:8px; background:var(--surface-2); border-radius:4px; overflow:hidden;">
                    <div style="width:${pct}%; height:100%; background:#f59e0b; border-radius:4px; transition:width 0.5s ease;"></div>
                  </div>
                  <span style="width:36px; color:var(--text-muted); font-size:11px;">${count}</span>
                </div>`;
            }).join("")}
          </div>
        </div>`;

      // Reviews List
      const listHtml = reviews.length > 0
        ? reviews.map((r) => `
            <div style="background:var(--surface); border:1px solid var(--border-light); border-radius:var(--radius-lg); padding:16px; margin-bottom:12px; box-shadow:var(--shadow-sm);">
              <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:8px;">
                <div style="display:flex; align-items:center; gap:8px;">
                  <span style="font-weight:700; font-size:14px; color:var(--text-primary);">${r.userName || "Customer"}</span>
                  ${r.verifiedBuyer ? `<span style="font-size:10px; font-weight:700; background:rgba(16,185,129,0.12); color:#10b981; padding:2px 6px; border-radius:10px;">✓ Verified Purchase</span>` : ""}
                </div>
                <span style="font-size:11px; color:var(--text-muted);">${r.createdAt ? new Date(r.createdAt).toLocaleDateString() : ""}</span>
              </div>
              <div style="margin-bottom:6px;">${renderStars(r.rating || 5)}</div>
              <p style="font-size:13px; color:var(--text-secondary); margin:0; line-height:1.5;">${r.comment}</p>
            </div>
          `).join("")
        : `<div style="text-align:center; padding:30px; background:var(--surface-2); border-radius:var(--radius-lg); color:var(--text-muted); font-size:13px;">No reviews yet. Be the first verified buyer to share your thoughts!</div>`;

      container.innerHTML = summaryHtml + reviewFormHtml + listHtml;

      // Wire Interactive Star Picker
      if (isVerifiedBuyer) {
        const starOpts = container.querySelectorAll(".star-opt");
        const ratingInput = document.getElementById("reviewRatingInput");
        starOpts.forEach((star) => {
          star.addEventListener("click", () => {
            const val = parseInt(star.dataset.star);
            if (ratingInput) ratingInput.value = val;
            starOpts.forEach((s) => {
              const sVal = parseInt(s.dataset.star);
              s.style.color = sVal <= val ? "#f59e0b" : "var(--border)";
            });
          });
        });

        // Wire Form Submission
        const form = document.getElementById("writeReviewForm");
        form?.addEventListener("submit", async (e) => {
          e.preventDefault();
          const submitBtn = document.getElementById("submitReviewBtn");
          const comment = document.getElementById("reviewCommentInput")?.value;
          const rating = parseInt(document.getElementById("reviewRatingInput")?.value || 5);

          if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = "Submitting...";
          }

          try {
            await api.post("/reviews", {
              productId: pid,
              rating: rating,
              comment: comment,
              userName: "Verified Buyer"
            });
            showToast("Review submitted successfully! ⭐", "success");
            loadProductReviews(pid);
          } catch (err) {
            if (submitBtn) {
              submitBtn.disabled = false;
              submitBtn.textContent = "Submit Review";
            }
            showToast(err.response?.data?.message || "Failed to submit review.", "error");
          }
        });
      }
    } catch {
      container.innerHTML = `<div style="padding:20px; text-align:center; color:var(--text-muted);">Reviews unavailable right now.</div>`;
    }
  }

  function updateProductControls(p, stockQty, cartItems) {
    if (typeof stockQty === "number") p._currentStockQty = stockQty;
    const inStock = stockQty !== null && stockQty !== undefined ? stockQty > 0 : true;
    const cartItem = cartItems.find((ci) => ci.productId === productId);
    const inCart = !!cartItem;
    const cartQty = cartItem ? cartItem.quantity : 0;

    const stockBadge = document.getElementById("productStockBadge");
    const stockText = document.getElementById("stockText");
    if (stockBadge && stockText) {
      if (stockQty === 0 || !inStock) {
        stockBadge.className = "product-detail-stock out-of-stock";
        stockText.textContent = "Out of Stock";
      } else if (stockQty > 0) {
        stockBadge.className = "product-detail-stock in-stock";
        stockText.textContent = `${stockQty} in stock`;
      }
    }

    const actionsWrap = document.getElementById("productActionsWrap");
    if (actionsWrap) {
      actionsWrap.innerHTML = renderActionControls(inStock, inCart, cartQty, productId);
      bindCartActions(p, inStock, inCart, cartQty, stockQty);
    }
  }

  function bindCartActions(p, inStock, inCart, cartQty, stockQty) {
    const currentStock = typeof stockQty === "number" ? stockQty : (typeof p._currentStockQty === "number" ? p._currentStockQty : null);
    const addBtn = document.getElementById("addToCartBtn");
    const removeBtn = document.getElementById("removeFromCartBtn");
    const buyBtn = document.getElementById("buyNowBtn");
    const wBtn = document.getElementById("productWishlistBtn");
    const shareBtn = document.getElementById("shareBtn");

    if (addBtn && inStock && !inCart) {
      addBtn.addEventListener("click", async () => {
        const addMsg = document.getElementById("addToCartMsg");
        addBtn.disabled = true;
        addBtn.classList.add("btn-loading");
        addBtn.innerHTML = '<span class="btn-spinner"></span> Adding...';

        try {
          await api.post("/cart", { productId, quantity: 1 });
          showToast("Added to Cart! 🛒", "success");
          refreshCartBadge();
          updateProductControls(p, currentStock, [{ productId, quantity: 1 }]);
        } catch (err) {
          addBtn.disabled = false;
          addBtn.classList.remove("btn-loading");
          addBtn.innerHTML = "Add to Cart";
          if (addMsg) {
            addMsg.textContent = err.response?.data?.message || "Failed to add to cart.";
            addMsg.className = "error";
          }
        }
      });
    }

    if (removeBtn) {
      removeBtn.addEventListener("click", async () => {
        removeBtn.disabled = true;
        try {
          await api.delete(`/cart/${productId}`);
          showToast("Removed from Cart", "info");
          refreshCartBadge();
          updateProductControls(p, currentStock, []);
        } catch (err) {
          removeBtn.disabled = false;
          showToast("Removed from Cart", "info");
          updateProductControls(p, currentStock, []);
        }
      });
    }

    if (inCart) {
      const actionsWrap = document.getElementById("productActionsWrap");
      const plusBtn = actionsWrap?.querySelector(".qtyPlus");
      const minusBtn = actionsWrap?.querySelector(".qtyMinus");

      plusBtn?.addEventListener("click", async () => {
        const newQty = cartQty + 1;
        updateProductControls(p, currentStock, [{ productId, quantity: newQty }]);
        try {
          await api.put(`/cart/${productId}?quantity=${newQty}`);
          refreshCartBadge();
        } catch {}
      });

      minusBtn?.addEventListener("click", async () => {
        if (cartQty <= 1) {
          updateProductControls(p, currentStock, []);
          try {
            await api.delete(`/cart/${productId}`);
            showToast("Removed from Cart", "info");
            refreshCartBadge();
          } catch {}
        } else {
          const newQty = cartQty - 1;
          updateProductControls(p, currentStock, [{ productId, quantity: newQty }]);
          try {
            await api.put(`/cart/${productId}?quantity=${newQty}`);
            refreshCartBadge();
          } catch {}
        }
      });
    }

    if (buyBtn && inStock) {
      buyBtn.addEventListener("click", async () => {
        buyBtn.disabled = true;
        buyBtn.innerHTML = '<span class="btn-spinner"></span> Redirecting...';
        try {
          if (!inCart) {
            await api.post("/cart", { productId, quantity: 1 }).catch(() => {});
          }
          window.location.href = "/checkout.html";
        } catch (err) {
          window.location.href = "/checkout.html";
        }
      });
    }

    if (wBtn) {
      wBtn.addEventListener("click", () => {
        const isSaved = toggleWishlist(productId);
        const svg = wBtn.querySelector("svg");
        if (isSaved) {
          wBtn.style.color = "#ef4444";
          wBtn.style.borderColor = "rgba(239,68,68,0.4)";
          svg.setAttribute("fill", "#ef4444");
          svg.setAttribute("stroke", "#ef4444");
          wBtn.childNodes[2].textContent = " Saved";
          showToast("Added to Wishlist! ❤️", "success");
        } else {
          wBtn.style.color = "";
          wBtn.style.borderColor = "";
          svg.setAttribute("fill", "none");
          svg.setAttribute("stroke", "currentColor");
          wBtn.childNodes[2].textContent = " Wishlist";
          showToast("Removed from Wishlist", "info");
        }
      });
    }

    if (shareBtn) {
      shareBtn.addEventListener("click", async () => {
        const shareData = { title: p.name, text: p.description, url: window.location.href };
        if (navigator.share) {
          try { await navigator.share(shareData); } catch {}
        } else {
          await navigator.clipboard.writeText(window.location.href);
          showToast("Product link copied to clipboard!", "success");
        }
      });
    }
  }

  initProductPage();
}

renderFooter();
