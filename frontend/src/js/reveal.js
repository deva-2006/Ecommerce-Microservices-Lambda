// Immediately reveal all data-reveal elements globally for max performance
document.querySelectorAll("[data-reveal]").forEach((el) => {
  el.classList.add("revealed");
});
