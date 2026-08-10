const THEME_KEY = "theme-preference";

export function initTheme() {
  const saved = localStorage.getItem(THEME_KEY) || "light";
  document.documentElement.setAttribute("data-theme", saved);
  applyTimeTint();
}

export function toggleTheme() {
  const current = document.documentElement.getAttribute("data-theme") || "light";
  const next = current === "light" ? "dark" : "light";
  document.documentElement.setAttribute("data-theme", next);
  localStorage.setItem(THEME_KEY, next);
  return next;
}

export function getTheme() {
  return document.documentElement.getAttribute("data-theme") || "light";
}

function applyTimeTint() {
  const h = new Date().getHours();
  let hue, sat, light;
  if (h >= 6 && h < 12) {
    hue = 30; sat = 15; light = 97;
  } else if (h >= 12 && h < 17) {
    hue = 200; sat = 10; light = 98;
  } else if (h >= 17 && h < 21) {
    hue = 270; sat = 12; light = 96;
  } else {
    hue = 220; sat = 8; light = 95;
  }
  document.documentElement.style.setProperty("--ambient-bg", `hsl(${hue}, ${sat}%, ${light}%)`);
}

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    const isDev = location.hostname === 'localhost' || location.hostname === '127.0.0.1';
    if (isDev) {
      navigator.serviceWorker.getRegistrations().then((regs) => {
        regs.forEach((reg) => reg.unregister());
      });
      caches.keys().then((keys) => {
        keys.forEach((k) => caches.delete(k));
      });
    } else {
      navigator.serviceWorker.register('/sw.js').catch(() => {});
    }
  });
}

// Page Transition Loader
document.addEventListener("DOMContentLoaded", () => {
  const loader = document.createElement("div");
  loader.id = "pageTransitionLoader";
  document.body.appendChild(loader);

  document.querySelectorAll("a[href]").forEach((link) => {
    link.addEventListener("click", (e) => {
      const href = link.getAttribute("href");
      if (href && !href.startsWith("#") && !href.startsWith("javascript:") && link.target !== "_blank") {
        loader.classList.add("active");
      }
    });
  });
});
