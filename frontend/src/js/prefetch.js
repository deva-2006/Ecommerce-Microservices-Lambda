import { prefetchProduct } from './api.js';

export function setupHoverPrefetch() {
  document.addEventListener('mouseover', (e) => {
    const card = e.target.closest('.product-card');
    if (!card) return;
    
    // Only prefetch once per card
    if (card.dataset.prefetched) return;
    card.dataset.prefetched = "true";
    
    const id = card.dataset.productId;
    if (id) {
      prefetchProduct(id);
    }
  });
}

export function prefetchRoutes(routes) {
  routes.forEach(route => {
    const link = document.createElement('link');
    link.rel = 'prefetch';
    link.href = route;
    document.head.appendChild(link);
  });
}
