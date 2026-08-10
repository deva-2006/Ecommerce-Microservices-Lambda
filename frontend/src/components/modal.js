// Reusable accessible confirm modal.
export function confirmModal(title, description = "") {
  return new Promise((resolve) => {
    const overlay = document.createElement("div");
    overlay.className = "modal-overlay";
    overlay.setAttribute("role", "dialog");
    overlay.setAttribute("aria-modal", "true");
    overlay.setAttribute("aria-labelledby", "modalTitleId");
    
    overlay.innerHTML = `
      <div class="modal-box">
        <h4 id="modalTitleId">${title}</h4>
        ${description ? `<p>${description}</p>` : ""}
        <div class="modal-actions">
          <button class="btn btn-outline" id="modalCancelBtn">Cancel</button>
          <button class="btn btn-danger" id="modalConfirmBtn">Delete</button>
        </div>
      </div>`;
    document.body.appendChild(overlay);

    const cancelBtn = overlay.querySelector("#modalCancelBtn");
    const confirmBtn = overlay.querySelector("#modalConfirmBtn");
    confirmBtn.focus();

    const cleanup = (result) => {
      document.removeEventListener("keydown", handleKeyDown);
      overlay.remove();
      resolve(result);
    };

    const handleKeyDown = (e) => {
      if (e.key === "Escape") {
        cleanup(false);
      } else if (e.key === "Tab") {
        const focusables = [cancelBtn, confirmBtn];
        const index = focusables.indexOf(document.activeElement);
        if (e.shiftKey && index === 0) {
          e.preventDefault();
          confirmBtn.focus();
        } else if (!e.shiftKey && index === focusables.length - 1) {
          e.preventDefault();
          cancelBtn.focus();
        }
      }
    };

    document.addEventListener("keydown", handleKeyDown);
    cancelBtn.addEventListener("click", () => cleanup(false));
    confirmBtn.addEventListener("click", () => cleanup(true));
    overlay.addEventListener("click", (e) => {
      if (e.target === overlay) cleanup(false);
    });
  });
}