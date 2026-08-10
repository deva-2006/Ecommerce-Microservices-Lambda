import "./admin-common.js";
import api from "./api.js";
import { showToast } from "../components/toast.js";
import { confirmModal } from "../components/modal.js";
import { renderFooter } from "../components/footer.js";

const listEl = document.getElementById("inventoryList");
const searchInput = document.getElementById("searchInput");

let allProducts = [];
let allItems = [];

function stockBadge(quantity) {
  let statusClass = "stock-healthy";
  if (quantity < 5) statusClass = "stock-critical";
  else if (quantity <= 20) statusClass = "stock-low";
  
  const fillPct = Math.min(100, (quantity / 50) * 100);
  
  return `
    <div style="width:100%; padding:4px;">
      <div style="font-size:10px;text-align:center;margin-bottom:2px;font-weight:600;">${quantity} in stock</div>
      <div class="stock-bar-bg">
        <div class="stock-bar-fill ${statusClass}" style="width: ${fillPct}%;"></div>
      </div>
    </div>
  `;
}

function renderProducts(items) {
  if (items.length === 0) {
    listEl.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7 4A2 2 0 0 0 21 16z"/></svg>
        </div>
        <h3 class="empty-state-title">No products found</h3>
        <p class="empty-state-desc">Create products in the Products section first.</p>
      </div>`;
    return;
  }

  listEl.innerHTML = items
    .map(
      (item) => `
      <div class="admin-item-card" data-id="${item.productId}" data-has-inventory="${item.hasInventory}">
        <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;width:120px;height:90px;background:var(--surface-2);border-radius:var(--radius-sm);flex-shrink:0;overflow:hidden;">
          <img src="${(item.imageUrls && item.imageUrls[0]) || item.imageUrl || '/placeholder.svg'}" alt="${item.name}" style="width:100%;height:60px;object-fit:cover;" onerror="this.style.display='none';this.nextElementSibling.style.display='flex';" />
          <div style="display:none;flex-direction:column;align-items:center;justify-content:center;height:100%;">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
          </div>
          ${stockBadge(item.quantity)}
        </div>
        <div class="admin-item-fields">
          <div style="font-weight:600;font-size:14px;margin-bottom:2px;">${item.name}</div>
          <div style="font-family:monospace;font-size:12px;color:var(--text-muted);margin-bottom:6px;word-break:break-all;">${item.productId}</div>
          <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
            <div class="form-group" style="margin:0;">
              <input class="form-input qtyInput" type="number" placeholder="Amount" style="width:120px;" />
            </div>
            <button class="btn btn-outline btn-sm quickAdd10">+10</button>
            <button class="btn btn-outline btn-sm quickAdd50">+50</button>
          </div>
        </div>
        <div class="admin-item-actions">
          <button class="btn btn-primary btn-sm addBtn">Add Stock</button>
          <button class="btn btn-outline btn-sm setBtn">Set Exact</button>
          ${item.hasInventory ? `<button class="btn btn-ghost btn-sm deleteBtn" style="color:var(--danger);">Delete</button>` : ""}
        </div>
      </div>`
    )
    .join("");

  listEl.querySelectorAll(".addBtn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const card = e.target.closest(".admin-item-card");
      const id = card.dataset.id;
      const hasInv = card.dataset.hasInventory === "true";
      const qty = card.querySelector(".qtyInput").value;
      if (!qty) return;
      try {
        if (hasInv) {
          await api.put(`/inventory/${id}/add-stock?quantity=${qty}`);
        } else {
          await api.post("/inventory", { productId: id, quantity: parseInt(qty) });
        }
        showToast("Stock added.", "success");
        await loadData();
      } catch (err) {
        showToast(err.response?.data?.message || "Add stock failed.", "error");
      }
    });
  });

  listEl.querySelectorAll(".quickAdd10").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const card = e.target.closest(".admin-item-card");
      const id = card.dataset.id;
      const hasInv = card.dataset.hasInventory === "true";
      try {
        if (hasInv) {
          await api.put(`/inventory/${id}/add-stock?quantity=10`);
        } else {
          await api.post("/inventory", { productId: id, quantity: 10 });
        }
        showToast("+10 stock added.", "success");
        await loadData();
      } catch (err) {
        showToast(err.response?.data?.message || "Quick add failed.", "error");
      }
    });
  });

  listEl.querySelectorAll(".quickAdd50").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const card = e.target.closest(".admin-item-card");
      const id = card.dataset.id;
      const hasInv = card.dataset.hasInventory === "true";
      try {
        if (hasInv) {
          await api.put(`/inventory/${id}/add-stock?quantity=50`);
        } else {
          await api.post("/inventory", { productId: id, quantity: 50 });
        }
        showToast("+50 stock added.", "success");
        await loadData();
      } catch (err) {
        showToast(err.response?.data?.message || "Quick add failed.", "error");
      }
    });
  });

  listEl.querySelectorAll(".setBtn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const card = e.target.closest(".admin-item-card");
      const id = card.dataset.id;
      const hasInv = card.dataset.hasInventory === "true";
      const qty = card.querySelector(".qtyInput").value;
      if (!qty) return;
      try {
        if (hasInv) {
          await api.put(`/inventory/${id}/update-stock?quantity=${qty}`);
        } else {
          await api.post("/inventory", { productId: id, quantity: parseInt(qty) });
        }
        showToast("Stock updated.", "success");
        await loadData();
      } catch (err) {
        showToast(err.response?.data?.message || "Update failed.", "error");
      }
    });
  });

  listEl.querySelectorAll(".deleteBtn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const card = e.target.closest(".admin-item-card");
      const id = card.dataset.id;
      const ok = await confirmModal("Delete inventory record?", "This action cannot be undone.");
      if (!ok) return;
      try {
        await api.delete(`/inventory/${id}`);
        showToast("Record deleted.", "success");
        await loadData();
      } catch (err) {
        showToast(err.response?.data?.message || "Delete failed.", "error");
      }
    });
  });
}

function filterProducts(query) {
  const q = query.toLowerCase().trim();
  if (!q) return allItems;
  return allItems.filter(
    (item) =>
      (item.name || "").toLowerCase().includes(q) ||
      (item.productId || "").toLowerCase().includes(q)
  );
}

searchInput?.addEventListener("input", () => {
  renderProducts(filterProducts(searchInput.value));
});

async function loadData() {
  listEl.innerHTML = `
    <div class="skeleton skeleton-row"></div>
    <div class="skeleton skeleton-row"></div>
    <div class="skeleton skeleton-row"></div>`;

  try {
    const [prodRes, invRes] = await Promise.all([
      api.get("/products").catch(() => ({ data: [] })),
      api.get("/inventory").catch(() => ({ data: [] }))
    ]);

    const products = prodRes.data || [];
    const inventory = invRes.data || [];

    const invMap = {};
    inventory.forEach((i) => { invMap[i.productId] = i.quantity; });

    allItems = products.map((p) => ({
      productId: p.productId,
      name: p.name,
      imageUrl: p.imageUrl || "",
      quantity: invMap[p.productId] ?? 0,
      hasInventory: invMap[p.productId] !== undefined
    }));

    allProducts = products;

    renderProducts(allItems);
  } catch (err) {
    showToast("Failed to load data.", "error");
  }
}

loadData();

document.getElementById("bulkRestockBtn")?.addEventListener("click", async () => {
  const lowItems = allItems.filter(i => i.quantity <= 20);
  if (!lowItems.length) {
    showToast("All products have healthy stock levels (>20)!", "info");
    return;
  }
  const btn = document.getElementById("bulkRestockBtn");
  btn.disabled = true;
  btn.textContent = "Restocking...";
  try {
    for (const item of lowItems) {
      if (item.hasInventory) {
        await api.put(`/inventory/${item.productId}/add-stock?quantity=20`);
      } else {
        await api.post("/inventory", { productId: item.productId, quantity: 20 });
      }
    }
    showToast(`Restocked ${lowItems.length} products with +20 units!`, "success");
    await loadData();
  } catch (err) {
    showToast("Bulk restock partially failed.", "error");
  } finally {
    btn.disabled = false;
    btn.textContent = "Restock All Low Stock (+20)";
  }
});

renderFooter();
