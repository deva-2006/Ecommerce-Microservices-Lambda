const CACHE_NAME = 'shopvibe-cache-v2';
const DYNAMIC_CACHE_NAME = 'shopvibe-dynamic-v2';

const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/home.html',
  '/css/style.css',
  '/js/theme.js',
  '/js/api.js'
];

self.addEventListener('install', (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(STATIC_ASSETS);
    }).catch(() => {})
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.filter((key) => key !== CACHE_NAME && key !== DYNAMIC_CACHE_NAME)
          .map((key) => caches.delete(key))
      );
    }).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  if (
    event.request.method !== 'GET' ||
    url.protocol === 'ws:' || url.protocol === 'wss:' ||
    url.hostname === 'localhost' || url.hostname === '127.0.0.1' ||
    url.hostname !== location.hostname ||
    url.pathname.startsWith('/@') ||
    url.pathname.startsWith('/__') ||
    url.pathname.includes('node_modules') ||
    url.origin.includes('execute-api') ||
    event.request.headers.get('purpose') === 'invoke'
  ) {
    return;
  }

  event.respondWith(
    caches.match(event.request).then((cached) => {
      const networkFetch = fetch(event.request).then((response) => {
        if (response && response.status === 200 && response.type === 'basic') {
          const clone = response.clone();
          caches.open(DYNAMIC_CACHE_NAME).then((c) => c.put(event.request, clone)).catch(() => {});
        }
        return response;
      }).catch(() => {
        if (event.request.mode === 'navigate') {
          return caches.match('/index.html').then((r) => r || new Response('Offline', { status: 503, headers: { 'Content-Type': 'text/html' } }));
        }
        return new Response('', { status: 504 });
      });
      return cached || networkFetch;
    }).catch(() => new Response('Service Unavailable', { status: 503 }))
  );
});
