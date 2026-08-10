import "./admin-common.js";
import api from "./api.js";
import { showToast } from "../components/toast.js";
import { confirmModal } from "../components/modal.js";
import { renderFooter } from "../components/footer.js";

const listEl = document.getElementById("reviewsList");
const productSelect = document.getElementById("productFilterSelect");
const ratingSelect = document.getElementById("ratingFilterSelect");
const sortSelect = document.getElementById("sortSelect");
const searchInput = document.getElementById("searchInput");
const searchClear = document.getElementById("searchClear");
const resultsCount = document.getElementById("resultsCount");
const clearAllBtn = document.getElementById("clearAllFilters");

const statTotalEl = document.getElementById("statTotalReviews");
const statAvgEl = document.getElementById("statAvgRating");
const statFiveStarEl = document.getElementById("statFiveStar");
const statOneStarEl = document.getElementById("statOneStar");

let productsMap = {};
let allReviews = [];

const RATING_COLORS = {
  1: { main: "#ef4444", soft: "#fef2f2", gradient: "#ef444422, #ef444444" },
  2: { main: "#f97316", soft: "#fff7ed", gradient: "#f9731622, #f9731644" },
  3: { main: "#eab308", soft: "#fefce8", gradient: "#eab30822, #eab30844" },
  4: { main: "#84cc16", soft: "#f7fee7", gradient: "#84cc1622, #84cc1644" },
  5: { main: "#10b981", soft: "#ecfdf5", gradient: "#10b98122, #10b98144" },
};

function getRatingColor(r) {
  return RATING_COLORS[r] || RATING_COLORS[3];
}

function renderStars(rating, size = 15) {
  let stars = "";
  for (let i = 1; i <= 5; i++) {
    const filled = i <= rating;
    stars += `<span class="rv-card-star" style="color:${filled ? "#f59e0b" : "var(--border)"};">${filled ? "★" : "☆"}</span>`;
  }
  return `<div class="rv-card-stars">${stars}</div>`;
}

function updateStats(reviews) {
  if (!reviews || reviews.length === 0) {
    if (statTotalEl) statTotalEl.textContent = "0";
    if (statAvgEl) statAvgEl.textContent = "0.0";
    if (statFiveStarEl) statFiveStarEl.textContent = "0";
    if (statOneStarEl) statOneStarEl.textContent = "0";
    updateDistribution([]);
    return;
  }

  const total = reviews.length;
  const sum = reviews.reduce((acc, r) => acc + (r.rating || 0), 0);
  const avg = (sum / total).toFixed(1);
  const fiveStars = reviews.filter((r) => r.rating === 5).length;
  const oneStars = reviews.filter((r) => r.rating === 1).length;

  if (statTotalEl) statTotalEl.textContent = total;
  if (statAvgEl) statAvgEl.textContent = avg;
  if (statFiveStarEl) statFiveStarEl.textContent = fiveStars;
  if (statOneStarEl) statOneStarEl.textContent = oneStars;

  updateDistribution(reviews);
}

function updateDistribution(reviews) {
  const total = reviews.length;
  for (let i = 1; i <= 5; i++) {
    const count = reviews.filter((r) => r.rating === i).length;
    const pct = total > 0 ? Math.round((count / total) * 100) : 0;
    const barEl = document.getElementById(`distBar${i}`);
    const countEl = document.getElementById(`distCount${i}`);
    if (barEl) barEl.style.width = `${pct}%`;
    if (countEl) countEl.textContent = count;
  }
}

function renderReviews(reviews) {
  if (!reviews || reviews.length === 0) {
    listEl.innerHTML = `
      <div class="rv-empty">
        <div class="rv-empty-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        </div>
        <h3 class="rv-empty-title">No reviews found</h3>
        <p class="rv-empty-desc">Try adjusting your filters or search query to find what you're looking for.</p>
      </div>`;
    return;
  }

  listEl.innerHTML = reviews
    .map((r) => {
      const prod = typeof productsMap[r.productId] === "object" ? productsMap[r.productId] : null;
      const pName = prod ? prod.name : (typeof productsMap[r.productId] === "string" ? productsMap[r.productId] : (r.productId || "Product"));
      
      const prodImgs = prod?.imageUrls?.length ? prod.imageUrls : prod?.imageUrl ? [prod.imageUrl] : [];
      const prodImg = prodImgs[0] || "/placeholder.svg";

      const dateStr = r.createdAt
        ? new Date(r.createdAt).toLocaleDateString("en-IN", {
            day: "numeric",
            month: "short",
            year: "numeric",
          })
        : "";
      const initial = (r.userName || "C").charAt(0).toUpperCase();
      const rc = getRatingColor(r.rating);

      return `
        <div class="rv-card" data-review-id="${r.reviewId}" data-rating="${r.rating}">
          <!-- Top Product Level Banner -->
          <div class="rv-product-header">
            <div class="rv-product-info">
              <img src="${prodImg}" alt="${pName}" class="rv-product-img" onerror="this.src='/placeholder.svg'" />
              <div class="rv-product-meta">
                <a href="/product.html?id=${r.productId}" target="_blank" class="rv-product-title">${pName}</a>
                <span class="rv-product-sku">Product ID: #${r.productId.substring(0, 12)}</span>
              </div>
            </div>
            <div class="rv-product-actions">
              <a href="/product.html?id=${r.productId}" target="_blank" class="btn btn-outline btn-sm" style="padding:4px 10px; font-size:12px; font-weight:600; gap:4px;">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                View Product
              </a>
              <button class="rv-card-delete delete-review-btn" data-id="${r.reviewId}" title="Delete Review">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
              </button>
            </div>
          </div>

          <!-- Bottom Customer Feedback Details -->
          <div class="rv-card-content">
            <div class="rv-user-bar">
              <div class="rv-user-details">
                <div class="rv-card-avatar" style="background:linear-gradient(135deg, ${rc.gradient}); color:${rc.main}; border-color:${rc.main}33;">
                  ${initial}
                </div>
                <div>
                  <div class="rv-card-user-row">
                    <span class="rv-card-username">${r.userName || "Verified Buyer"}</span>
                    ${r.verifiedBuyer !== false ? `<span class="rv-card-verified">✓ Verified Buyer</span>` : ""}
                  </div>
                  <span class="rv-card-date">${dateStr}</span>
                </div>
              </div>

              <div class="rv-rating-badge-group">
                ${renderStars(r.rating)}
                <span class="rv-card-rating-badge" style="color:${rc.main}; background:${rc.soft}; border:1px solid ${rc.main}33;">
                  ${r.rating}.0 / 5.0
                </span>
              </div>
            </div>

            <div class="rv-comment-box">
              <p class="rv-card-comment">"${r.comment || "No comment left."}"</p>
            </div>
          </div>
        </div>
      `;
    })
    .join("");

  listEl.querySelectorAll(".delete-review-btn").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const reviewId = btn.dataset.id;
      const ok = await confirmModal(
        "Delete customer review?",
        "This action will permanently remove the review."
      );
      if (!ok) return;

      try {
        await api.delete(`/reviews/${reviewId}`);
        showToast("Review deleted successfully", "success");
        allReviews = allReviews.filter((r) => r.reviewId !== reviewId);
        filterAndRender();
      } catch (err) {
        showToast(
          err.response?.data?.message || "Failed to delete review",
          "error"
        );
      }
    });
  });
}

function sortReviews(reviews) {
  const sortBy = sortSelect.value;
  const sorted = [...reviews];
  switch (sortBy) {
    case "newest":
      sorted.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
      break;
    case "oldest":
      sorted.sort((a, b) => new Date(a.createdAt || 0) - new Date(b.createdAt || 0));
      break;
    case "highest":
      sorted.sort((a, b) => (b.rating || 0) - (a.rating || 0));
      break;
    case "lowest":
      sorted.sort((a, b) => (a.rating || 0) - (b.rating || 0));
      break;
  }
  return sorted;
}

function filterAndRender() {
  let filtered = [...allReviews];
  const pId = productSelect.value;
  const ratingVal = ratingSelect.value;
  const query = searchInput.value.trim().toLowerCase();

  if (pId !== "all") {
    filtered = filtered.filter((r) => r.productId === pId);
  }
  if (ratingVal !== "all") {
    filtered = filtered.filter((r) => r.rating === parseInt(ratingVal));
  }
  if (query) {
    filtered = filtered.filter(
      (r) =>
        (r.userName || "").toLowerCase().includes(query) ||
        (r.comment || "").toLowerCase().includes(query)
    );
  }

  filtered = sortReviews(filtered);

  updateStats(filtered);

  const total = filtered.length;
  const hasFilters =
    pId !== "all" || ratingVal !== "all" || query.length > 0;
  resultsCount.textContent = `Showing ${total} review${total !== 1 ? "s" : ""}${hasFilters ? " (filtered)" : ""}`;
  clearAllBtn.style.display = hasFilters ? "inline-flex" : "none";

  // highlight active distribution bar
  document.querySelectorAll(".rv-dist-row").forEach((row) => {
    const rr = row.dataset.rating;
    row.classList.toggle("active", ratingVal === rr);
  });

  renderReviews(filtered);
}

// Search handlers
let searchDebounce;
searchInput?.addEventListener("input", () => {
  clearTimeout(searchDebounce);
  searchClear.style.display = searchInput.value ? "flex" : "none";
  searchDebounce = setTimeout(() => filterAndRender(), 250);
});

searchClear?.addEventListener("click", () => {
  searchInput.value = "";
  searchClear.style.display = "none";
  filterAndRender();
  searchInput.focus();
});

// Clear all filters
clearAllBtn?.addEventListener("click", () => {
  productSelect.value = "all";
  ratingSelect.value = "all";
  sortSelect.value = "newest";
  searchInput.value = "";
  searchClear.style.display = "none";
  filterAndRender();
});

// Distribution bar click to filter
document.querySelectorAll(".rv-dist-row").forEach((row) => {
  row.addEventListener("click", () => {
    const rr = row.dataset.rating;
    if (ratingSelect.value === rr) {
      ratingSelect.value = "all";
    } else {
      ratingSelect.value = rr;
    }
    filterAndRender();
  });
});

productSelect?.addEventListener("change", filterAndRender);
ratingSelect?.addEventListener("change", filterAndRender);
sortSelect?.addEventListener("change", filterAndRender);

async function loadData() {
  listEl.innerHTML = `
    <div class="skeleton skeleton-row" style="height:180px;"></div>
    <div class="skeleton skeleton-row" style="height:180px;"></div>
    <div class="skeleton skeleton-row" style="height:180px;"></div>`;

  try {
    const prodRes = await api.get("/products").catch(() => ({ data: [] }));
    const products = prodRes.data || [];

    productsMap = {};
    productSelect.innerHTML = `<option value="all">All Products (${products.length})</option>`;

    products.forEach((p) => {
      productsMap[p.productId] = p;
      const opt = document.createElement("option");
      opt.value = p.productId;
      opt.textContent = p.name;
      productSelect.appendChild(opt);
    });

    allReviews = [];

    await Promise.all(
      products.map(async (p) => {
        try {
          const revRes = await api.get(`/reviews/product/${p.productId}`);
          if (revRes && revRes.data && Array.isArray(revRes.data)) {
            allReviews.push(...revRes.data);
          }
        } catch {}
      })
    );

    allReviews.sort(
      (a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
    );

    filterAndRender();
  } catch (err) {
    listEl.innerHTML = `<div class="rv-empty"><h3 class="rv-empty-title">Failed to load reviews</h3><p class="rv-empty-desc">${err.message || "Unknown error"}</p></div>`;
  }
}

loadData();
renderFooter();
