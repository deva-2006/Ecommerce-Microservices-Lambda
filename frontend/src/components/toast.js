let container = null;
const MAX_TOASTS = 3;

function getContainer() {
  if (container) return container;
  container = document.getElementById("toastContainer");
  if (!container) {
    container = document.createElement("div");
    container.id = "toastContainer";
    container.setAttribute("role", "region");
    container.setAttribute("aria-label", "Notifications");
    document.body.appendChild(container);
  }
  return container;
}

export function showToast(message, type = "info", duration = 3500) {
  const cont = getContainer();
  
  // Cap at MAX_TOASTS
  const existing = cont.querySelectorAll(".toast");
  if (existing.length >= MAX_TOASTS) {
    existing[0].remove();
  }

  const el = document.createElement("div");
  el.className = `toast ${type}`;
  el.setAttribute("role", "alert");
  el.setAttribute("aria-live", "polite");

  const textSpan = document.createElement("span");
  textSpan.textContent = message;

  const closeBtn = document.createElement("button");
  closeBtn.className = "toast-close";
  closeBtn.innerHTML = "&times;";
  closeBtn.setAttribute("aria-label", "Close notification");
  closeBtn.onclick = () => removeToast();

  el.appendChild(textSpan);
  el.appendChild(closeBtn);
  cont.appendChild(el);

  let timer = setTimeout(removeToast, duration);

  function removeToast() {
    clearTimeout(timer);
    el.classList.add("leaving");
    setTimeout(() => el.remove(), 200);
  }
}