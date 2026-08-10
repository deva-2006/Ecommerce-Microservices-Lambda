/**
 * Premium background effects — glow orbs, parallax, floating shapes.
 * Self-initializing. Respects prefers-reduced-motion.
 * Zero impact on layout/functionality. All elements are pointer-events:none.
 */
(function () {
  return; // Disabled globally for ultra-fast, smooth performance on PC & mobile

  const root = document.documentElement;
  let mx = 0, my = 0, sy = 0;
  let rafId = null;
  let ticking = false;

  // --- Create DOM container for floating shapes ---
  const layer = document.createElement("div");
  layer.className = "bg-effects-layer";
  layer.setAttribute("aria-hidden", "true");

  const orbConfigs = [
    { className: "bg-orb bg-orb-1", w: 500, h: 500 },
    { className: "bg-orb bg-orb-2", w: 400, h: 400 },
    { className: "bg-orb bg-orb-3", w: 350, h: 350 },
    { className: "bg-orb bg-orb-4", w: 280, h: 280 },
  ];

  const shapeConfigs = [
    { className: "bg-shape bg-shape-1" },
    { className: "bg-shape bg-shape-2" },
    { className: "bg-shape bg-shape-3" },
  ];

  orbConfigs.forEach((c) => {
    const el = document.createElement("div");
    el.className = c.className;
    el.style.width = c.w + "px";
    el.style.height = c.h + "px";
    layer.appendChild(el);
  });

  shapeConfigs.forEach((c) => {
    const el = document.createElement("div");
    el.className = c.className;
    layer.appendChild(el);
  });

  // Insert as first child of body so it sits behind everything
  document.body.prepend(layer);

  // --- Mouse parallax ---
  function onMove(e) {
    mx = (e.clientX / window.innerWidth - 0.5) * 2;
    my = (e.clientY / window.innerHeight - 0.5) * 2;
    scheduleUpdate();
  }

  // --- Scroll parallax ---
  function onScroll() {
    sy = window.scrollY;
    scheduleUpdate();
  }

  function scheduleUpdate() {
    if (!ticking) {
      ticking = true;
      rafId = requestAnimationFrame(update);
    }
  }

  function update() {
    ticking = false;
    root.style.setProperty("--mx", mx.toFixed(3));
    root.style.setProperty("--my", my.toFixed(3));
    root.style.setProperty("--scroll-y", sy.toFixed(0));
  }

  // Passive listeners for 60fps
  document.addEventListener("mousemove", onMove, { passive: true });
  document.addEventListener("scroll", onScroll, { passive: true });

  // Initial values
  root.style.setProperty("--mx", "0");
  root.style.setProperty("--my", "0");
  root.style.setProperty("--scroll-y", "0");
})();
