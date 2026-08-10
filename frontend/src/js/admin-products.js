import "./admin-common.js";
import api, { invalidateProductsCache, uploadProductImage, uploadProductImages } from "./api.js";
import { confirmModal } from "../components/modal.js";
import { showToast } from "../components/toast.js";
import { renderFooter } from "../components/footer.js";

const listEl = document.getElementById("productList");
const listMsg = document.getElementById("listMsg");
const createMsg = document.getElementById("createMsg");
const searchInput = document.getElementById("searchInput");

const MAX_IMAGE_MB = 5;
let allProducts = [];

function validateImage(file) {
  if (!file) return true;
  if (!file.type.startsWith("image/")) {
    showToast("Please choose an image file.", "error");
    return false;
  }
  if (file.size > MAX_IMAGE_MB * 1024 * 1024) {
    showToast(`Image must be under ${MAX_IMAGE_MB}MB.`, "error");
    return false;
  }
  return true;
}

function resolveProductImages(p) {
  if (p.imageUrls && p.imageUrls.length > 0) return p.imageUrls;
  if (p.imageUrl) return [p.imageUrl];
  return [];
}

function renderProducts(products) {
  if (products.length === 0) {
    listEl.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/></svg>
        </div>
        <h3 class="empty-state-title">No products yet</h3>
        <p class="empty-state-desc">Create your first product using the form above.</p>
      </div>`;
    return;
  }

  listEl.innerHTML = products
    .map(
      (p) => {
        const images = resolveProductImages(p);
        const imagesJson = JSON.stringify(images).replace(/"/g, '&quot;');
        return `
      <div class="admin-item-card" data-id="${p.productId}" data-images='${imagesJson}'>
        <div class="admin-card-header" style="display:flex; justify-content:space-between; align-items:center; width:100%; margin-bottom:4px; padding-bottom:8px; border-bottom:1px solid var(--border-light);">
          <span style="font-family:monospace; font-size:12px; font-weight:700; color:var(--text-secondary); background:var(--surface-2); border:1px solid var(--border-light); padding:3px 10px; border-radius:var(--radius-full);">ID: ${p.productId}</span>
          <span class="badge badge-confirmed" style="font-size:11px; text-transform:uppercase; font-weight:700;">${p.category || 'General'}</span>
        </div>

        <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--text-muted); margin-top:8px; margin-bottom:4px;">Product Gallery (Up to 7)</div>
        <div class="admin-edit-images" style="margin-bottom:8px;">
          ${images.map((url, i) => `
            <div class="thumb-drop-zone edit-thumb-zone has-image" data-edit-slot="${i}" data-removed="false">
              <input type="file" accept="image/*" class="editImageFile" hidden />
              <img src="${url || '/placeholder.svg'}" alt="${p.name} ${i + 1}" class="admin-item-thumb" onerror="this.src='/placeholder.svg'" />
              <div class="thumb-drop-overlay">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                ${i === 0 ? 'Main' : `Image ${i + 1}`}
              </div>
              <button class="thumb-delete-btn" title="Remove image">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
          `).join("")}
          ${images.length < 7 ? `
            <div class="thumb-drop-zone edit-thumb-zone add-more-thumb" data-edit-slot="${images.length}">
              <input type="file" accept="image/*" class="editImageFile" hidden />
              <div class="thumb-drop-overlay" style="opacity:1;">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                <span style="font-size:11px; font-weight:600;">Add Image</span>
              </div>
            </div>
          ` : ""}
        </div>

        <div class="admin-item-fields" style="gap:10px; margin-top:8px;">
          <div class="form-group" style="margin-bottom:0;">
            <label class="form-label" style="font-size:12px; margin-bottom:4px;">Product Name</label>
            <input class="form-input editName" value="${p.name}" style="height:38px;" />
          </div>
          <div class="form-group" style="margin-bottom:0;">
            <label class="form-label" style="font-size:12px; margin-bottom:4px;">Description</label>
            <textarea class="form-textarea editDesc" rows="2" style="font-size:13px;">${p.description || ""}</textarea>
          </div>
          <div style="display:grid; grid-template-columns:1fr 1fr; gap:10px;">
            <div class="form-group" style="margin-bottom:0;">
              <label class="form-label" style="font-size:12px; margin-bottom:4px;">Category</label>
              <input class="form-input editCategory" value="${p.category || ""}" placeholder="Category" style="height:38px;" />
            </div>
            <div class="form-group" style="margin-bottom:0;">
              <label class="form-label" style="font-size:12px; margin-bottom:4px;">Price (₹)</label>
              <input class="form-input editPrice" type="number" value="${p.price}" placeholder="Price" style="height:38px;" />
            </div>
          </div>
          <div class="form-group" style="margin-top:8px; margin-bottom:0;">
            <label class="form-label" style="font-size:12px; margin-bottom:4px;">Highlights (Comma-separated)</label>
            <input class="form-input editHighlights" value="${Array.isArray(p.highlights) ? p.highlights.join(', ') : (p.highlights || '')}" placeholder="e.g. 1 Year Warranty, Fast Shipping" style="height:38px;" />
          </div>
        </div>

        <div class="admin-item-actions" style="display:flex; justify-content:flex-end; gap:10px; margin-top:14px; padding-top:12px; border-top:1px solid var(--border-light);">
          <button class="btn btn-outline btn-sm deleteBtn" style="height:36px; padding:0 16px; font-size:13px;">Delete</button>
          <button class="btn btn-primary btn-sm saveBtn" style="height:36px; padding:0 18px; font-size:13px; font-weight:700;">Save Changes</button>
        </div>
      </div>`;
      }
    )
    .join("");

  listEl.querySelectorAll(".edit-thumb-zone").forEach((zone) => {
    const input = zone.querySelector(".editImageFile");
    const img = zone.querySelector("img");
    let ignoreNextClick = false;

    zone.querySelector(".thumb-delete-btn")?.addEventListener("click", (e) => {
      e.stopPropagation();
      zone.dataset.removed = "true";
      zone.style.opacity = "0.3";
      zone.style.pointerEvents = "none";
    });

    zone.addEventListener("click", () => {
      if (ignoreNextClick || zone.dataset.removed === "true") return;
      input.click();
    });

    ["dragenter", "dragover"].forEach((eventName) => {
      zone.addEventListener(eventName, (e) => {
        e.preventDefault();
        e.stopPropagation();
        zone.classList.add("drag-over");
      }, false);
    });

    ["dragleave", "dragend"].forEach((eventName) => {
      zone.addEventListener(eventName, (e) => {
        e.preventDefault();
        e.stopPropagation();
        zone.classList.remove("drag-over");
      }, false);
    });

    zone.addEventListener("drop", (e) => {
      e.preventDefault();
      e.stopPropagation();
      ignoreNextClick = true;
      setTimeout(() => { ignoreNextClick = false; }, 300);

      zone.classList.remove("drag-over");
      const dt = e.dataTransfer;
      const files = dt ? dt.files : null;
      if (files && files.length > 0) {
        const file = files[0];
        if (file && validateImage(file)) {
          try {
            const container = new DataTransfer();
            container.items.add(file);
            input.files = container.files;
          } catch {}
          if (img) {
            const reader = new FileReader();
            reader.onload = (ev) => { img.src = ev.target.result; };
            reader.readAsDataURL(file);
          } else {
            const reader = new FileReader();
            reader.onload = (ev) => {
              const newImg = document.createElement("img");
              newImg.src = ev.target.result;
              newImg.alt = "Preview";
              newImg.className = "admin-item-thumb";
              zone.prepend(newImg);
              zone.classList.remove("add-more-thumb");
              zone.querySelector(".thumb-drop-overlay span")?.remove();
            };
            reader.readAsDataURL(file);
          }
        }
      }
    }, false);

    input.addEventListener("change", () => {
      if (input.files[0]) {
        const reader = new FileReader();
        reader.onload = (ev) => {
          if (img) {
            img.src = ev.target.result;
          } else {
            const newImg = document.createElement("img");
            newImg.src = ev.target.result;
            newImg.alt = "Preview";
            newImg.className = "admin-item-thumb";
            zone.prepend(newImg);
            zone.classList.remove("add-more-thumb");
            zone.querySelector(".thumb-drop-overlay span")?.remove();
          }
        };
        reader.readAsDataURL(input.files[0]);
      }
    });
  });

  listEl.querySelectorAll(".saveBtn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const card = e.target.closest(".admin-item-card");
      const id = card.dataset.id;
      const existingImages = JSON.parse(card.dataset.images || "[]");

      const editZones = card.querySelectorAll(".edit-thumb-zone");
      const newFiles = [];
      const keptUrls = [];

      editZones.forEach((zone) => {
        const input = zone.querySelector(".editImageFile");
        const slot = parseInt(zone.dataset.editSlot);
        const isRemoved = zone.dataset.removed === "true";
        if (isRemoved) return;
        if (input && input.files[0]) {
          newFiles.push({ slot, file: input.files[0] });
        } else if (existingImages[slot]) {
          keptUrls.push({ slot, url: existingImages[slot] });
        }
      });

      const allNewValid = newFiles.every(({ file }) => validateImage(file));
      if (!allNewValid) return;

      btn.disabled = true;
      btn.textContent = "Saving...";

      try {
        const uploadedUrls = await uploadProductImages(newFiles.map(({ file }) => file));

        const slotUrlMap = new Map();
        keptUrls.forEach(({ slot, url }) => slotUrlMap.set(slot, url));
        newFiles.forEach(({ slot }, i) => {
          if (uploadedUrls[i]) slotUrlMap.set(slot, uploadedUrls[i]);
        });

        const imageUrls = [];
        for (let i = 0; i < 7; i++) {
          if (slotUrlMap.has(i)) imageUrls.push(slotUrlMap.get(i));
        }

        const rawHighlights = card.querySelector(".editHighlights")?.value || "";
        const highlights = rawHighlights.split(/,|\n/).map(h => h.trim()).filter(Boolean);

        const body = {
          name: card.querySelector(".editName").value,
          description: card.querySelector(".editDesc").value,
          category: card.querySelector(".editCategory").value,
          price: parseFloat(card.querySelector(".editPrice").value),
          highlights,
          imageUrls,
          imageUrl: imageUrls.length > 0 ? imageUrls[0] : null
        };

        await api.put(`/products/${id}`, body);
        invalidateProductsCache();
        showToast("Product updated.", "success");
        await loadProducts();
      } catch (err) {
        showToast(err.response?.data?.message || "Update failed.", "error");
        btn.disabled = false;
        btn.textContent = "Save";
      }
    });
  });

  listEl.querySelectorAll(".deleteBtn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const card = e.target.closest(".admin-item-card");
      const id = card.dataset.id;
      const name = card.querySelector(".editName").value;

      const ok = await confirmModal(`Delete "${name}"?`, "This can't be undone.");
      if (!ok) return;

      try {
        await api.delete(`/products/${id}`);
        invalidateProductsCache();
        showToast("Product deleted.", "success");
        await loadProducts();
      } catch (err) {
        showToast(err.response?.data?.message || "Delete failed.", "error");
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
      (p.productId || "").toLowerCase().includes(q)
  );
}

searchInput?.addEventListener("input", () => {
  renderProducts(filterProducts(searchInput.value));
});

async function loadProducts() {
  listEl.innerHTML = `
    <div class="skeleton skeleton-row"></div>
    <div class="skeleton skeleton-row"></div>
    <div class="skeleton skeleton-row"></div>`;

  try {
    const res = await api.get("/products");
    allProducts = res.data || [];

    if (allProducts.length === 0) {
      listEl.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/></svg>
          </div>
          <h3 class="empty-state-title">No products yet</h3>
          <p class="empty-state-desc">Create your first product using the form above.</p>
        </div>`;
      return;
    }

    renderProducts(allProducts);
  } catch (err) {
    listEl.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon" style="background:var(--danger-soft);">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--danger)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        </div>
        <h3 class="empty-state-title">Failed to load products</h3>
        <p class="empty-state-desc">Please try refreshing the page.</p>
      </div>`;
  }
}

["dragenter", "dragover", "dragleave", "drop"].forEach((eventName) => {
  document.addEventListener(eventName, (e) => {
    e.preventDefault();
    e.stopPropagation();
  }, false);
  window.addEventListener(eventName, (e) => {
    e.preventDefault();
    e.stopPropagation();
  }, false);
});

const createDropZones = document.querySelectorAll(".multi-drop-zone");
const createFiles = [null, null, null, null, null, null, null];

createDropZones.forEach((zone) => {
  const input = zone.querySelector(".imageFileInput");
  const slot = parseInt(zone.dataset.slot);
  let dragCounter = 0;
  let ignoreNextClick = false;

  zone.addEventListener("click", (e) => {
    if (ignoreNextClick || e.target.closest(".drop-zone-remove")) return;
    input.click();
  });

  zone.addEventListener("dragenter", (e) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounter++;
    zone.classList.add("drag-over");
  }, false);

  zone.addEventListener("dragover", (e) => {
    e.preventDefault();
    e.stopPropagation();
    zone.classList.add("drag-over");
  }, false);

  zone.addEventListener("dragleave", (e) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounter--;
    if (dragCounter <= 0) {
      dragCounter = 0;
      zone.classList.remove("drag-over");
    }
  }, false);

  zone.addEventListener("drop", (e) => {
    e.preventDefault();
    e.stopPropagation();
    ignoreNextClick = true;
    setTimeout(() => { ignoreNextClick = false; }, 300);

    dragCounter = 0;
    zone.classList.remove("drag-over");

    const dt = e.dataTransfer;
    const files = dt ? dt.files : null;
    if (files && files.length > 0) {
      const file = files[0];
      if (file && validateImage(file)) {
        try {
          const container = new DataTransfer();
          container.items.add(file);
          input.files = container.files;
        } catch {}
        setCreatePreview(zone, slot, file);
      }
    }
  }, false);

  input.addEventListener("change", () => {
    if (input.files[0] && validateImage(input.files[0])) {
      setCreatePreview(zone, slot, input.files[0]);
    }
  });

  zone.querySelector(".drop-zone-remove")?.addEventListener("click", (e) => {
    e.stopPropagation();
    clearCreatePreview(zone, slot);
  });
});

function setCreatePreview(zone, slot, file) {
  createFiles[slot] = file;
  const preview = zone.querySelector(".drop-zone-preview");
  const content = zone.querySelector(".drop-zone-content");
  const img = preview.querySelector("img");
  const reader = new FileReader();
  reader.onload = (e) => {
    img.src = e.target.result;
    content.style.display = "none";
    preview.style.display = "flex";
    zone.classList.add("has-file");
  };
  reader.readAsDataURL(file);
}

function clearCreatePreview(zone, slot) {
  createFiles[slot] = null;
  const preview = zone.querySelector(".drop-zone-preview");
  const content = zone.querySelector(".drop-zone-content");
  const input = zone.querySelector(".imageFileInput");
  input.value = "";
  content.style.display = "";
  preview.style.display = "none";
  zone.classList.remove("has-file");
}

document.getElementById("createBtn").addEventListener("click", async (e) => {
  const btn = e.target;
  const uploadStatus = document.getElementById("uploadStatus");

  const filesToUpload = createFiles.filter(Boolean);
  if (filesToUpload.length > 0) {
    const allValid = filesToUpload.every(validateImage);
    if (!allValid) return;
  }

  btn.disabled = true;
  btn.textContent = "Creating...";

  try {
    let imageUrls = [];
    if (filesToUpload.length > 0) {
      uploadStatus.textContent = "Uploading images...";
      imageUrls = await uploadProductImages(filesToUpload);
      uploadStatus.textContent = "Images uploaded.";
    }

    const rawHighlights = document.getElementById("highlights")?.value || "";
    const highlights = rawHighlights.split(/,|\n/).map(h => h.trim()).filter(Boolean);

    const body = {
      name: document.getElementById("name").value,
      description: document.getElementById("description").value,
      category: document.getElementById("category").value,
      price: parseFloat(document.getElementById("price").value),
      highlights,
      imageUrls,
      imageUrl: imageUrls.length > 0 ? imageUrls[0] : null
    };

    await api.post("/products", body);
    invalidateProductsCache();
    showToast("Product created!", "success");
    document.getElementById("name").value = "";
    document.getElementById("description").value = "";
    document.getElementById("category").value = "";
    document.getElementById("price").value = "";
    if (document.getElementById("highlights")) document.getElementById("highlights").value = "";
    createDropZones.forEach((zone, i) => clearCreatePreview(zone, i));
    uploadStatus.textContent = "";
    await loadProducts();
  } catch (err) {
    showToast(err.response?.data?.message || "Create failed.", "error");
  } finally {
    btn.disabled = false;
    btn.textContent = "Create Product";
  }
});

loadProducts();

const sortSelect = document.getElementById("sortSelect");

function updateProductList() {
  const query = (searchInput?.value || "").toLowerCase().trim();
  const sortMode = sortSelect?.value || "default";

  let filtered = allProducts.filter(
    (p) =>
      (p.name || "").toLowerCase().includes(query) ||
      (p.category || "").toLowerCase().includes(query) ||
      (p.productId || "").toLowerCase().includes(query)
  );

  if (sortMode === "price-asc") {
    filtered.sort((a, b) => (a.price || 0) - (b.price || 0));
  } else if (sortMode === "price-desc") {
    filtered.sort((a, b) => (b.price || 0) - (a.price || 0));
  } else if (sortMode === "name-asc") {
    filtered.sort((a, b) => (a.name || "").localeCompare(b.name || ""));
  }

  renderProducts(filtered);
}

searchInput?.addEventListener("input", updateProductList);
sortSelect?.addEventListener("change", updateProductList);

renderFooter();
