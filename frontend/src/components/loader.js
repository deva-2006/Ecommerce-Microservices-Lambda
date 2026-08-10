export function showLoader(containerId) {
  const container = document.getElementById(containerId);
  if (container) container.innerHTML = `<div class="spinner"></div>`;
}

export function hideLoader(containerId) {
  const container = document.getElementById(containerId);
  if (container) container.innerHTML = "";
}
