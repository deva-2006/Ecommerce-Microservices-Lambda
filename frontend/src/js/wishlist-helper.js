export function toggleWishlist(productId) {
  let list = getWishlist();
  if (list.includes(productId)) {
    list = list.filter(id => id !== productId);
  } else {
    list.push(productId);
  }
  localStorage.setItem('shopvibe_wishlist', JSON.stringify(list));
  return list.includes(productId);
}

export function getWishlist() {
  try {
    return JSON.parse(localStorage.getItem('shopvibe_wishlist')) || [];
  } catch {
    return [];
  }
}

export function isInWishlist(productId) {
  return getWishlist().includes(productId);
}
