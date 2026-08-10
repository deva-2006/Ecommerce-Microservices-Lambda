import axios from "axios";
import { getAccessToken } from "./auth.js";

const BASE_URL = import.meta.env.VITE_API_URL || "https://73svzbgcrf.execute-api.us-east-1.amazonaws.com";

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 15000
});

const inFlightRequests = new Map();

// Retry interceptor for GET requests (skips health check requests)
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config;
    const isHealthUrl = config?.url?.includes("/health/");
    if (config && config.method?.toLowerCase() === "get" && !config._retryAttempted && !isHealthUrl) {
      config._retryAttempted = true;
      await new Promise(r => setTimeout(r, 1000));
      return api(config);
    }
    const isOrders404 = config?.url?.includes("/orders") && error.response?.status === 404;
    if (!isHealthUrl && !isOrders404) {
      console.error("API error:", error.response?.status, error.response?.data);
    }
    return Promise.reject(error);
  }
);

api.interceptors.request.use(async (config) => {
  const token = await getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Cache configurations
const PRODUCTS_CACHE_KEY = "cache:products:persistent";
const PRODUCTS_TTL_MS = 300_000;

export async function getProductsCached() {
  const persistent = localStorage.getItem(PRODUCTS_CACHE_KEY);
  if (persistent) {
    const { data, timestamp } = JSON.parse(persistent);
    if (Date.now() - timestamp < PRODUCTS_TTL_MS) {
      // Revalidate in background
      fetchAndCacheProducts().catch(() => {});
      return { data };
    }
  }
  return fetchAndCacheProducts();
}

async function fetchAndCacheProducts() {
  const cacheKey = "/products";
  if (inFlightRequests.has(cacheKey)) {
    return inFlightRequests.get(cacheKey);
  }
  
  const promise = api.get("/products").then(res => {
    localStorage.setItem(PRODUCTS_CACHE_KEY, JSON.stringify({ data: res.data, timestamp: Date.now() }));
    inFlightRequests.delete(cacheKey);
    return res;
  }).catch(err => {
    inFlightRequests.delete(cacheKey);
    throw err;
  });
  
  inFlightRequests.set(cacheKey, promise);
  return promise;
}

export function invalidateProductsCache() {
  localStorage.removeItem(PRODUCTS_CACHE_KEY);
  sessionStorage.removeItem("cache:products");
}

export async function uploadProductImage(file) {
  if (!file) return null;
  const { data } = await api.get("/products/upload-url", {
    params: { fileName: file.name, contentType: file.type }
  });
  await axios.put(data.uploadUrl, file, { headers: { "Content-Type": file.type } });
  return data.publicUrl;
}

export async function uploadProductImages(files) {
  const validFiles = files.filter(Boolean);
  if (validFiles.length === 0) return [];
  const results = await Promise.all(validFiles.map((f) => uploadProductImage(f)));
  return results.filter(Boolean);
}

export function prefetchProduct(id) {
  const productKey = `cache:product:${id}`;
  const inventoryKey = `cache:inventory:${id}`;
  
  const p1 = api.get(`/products/${id}`).then(res => {
    sessionStorage.setItem(productKey, JSON.stringify({ data: res.data, timestamp: Date.now() }));
  }).catch(() => {});
  
  const p2 = api.get(`/inventory/${id}`).then(res => {
    sessionStorage.setItem(inventoryKey, JSON.stringify({ data: res.data, timestamp: Date.now() }));
  }).catch(() => {});
  
  return Promise.all([p1, p2]);
}

// Warm up Lambdas via unauthenticated health endpoints (work for customer and admin)
const HEALTH_ENDPOINTS = [
  "/health/product",
  "/health/inventory",
  "/health/cart",
  "/health/order",
  "/health/payment",
  "/health/review"
];

export function warmUpAllServices() {
  const lastWarmUp = sessionStorage.getItem("cache:warmup:last");
  if (lastWarmUp && Date.now() - Number(lastWarmUp) < 5_000) return;
  sessionStorage.setItem("cache:warmup:last", String(Date.now()));
  HEALTH_ENDPOINTS.forEach((path) => api.get(path).catch(() => {}));
}

export function warmUpProductsService() {
  getProductsCached().catch(() => {});
}

export function warmUpOtherServices() {
  api.get("/cart").catch(() => {});
  api.get("/orders").catch(() => {});
  api.get("/inventory").catch(() => {});
}

export function warmUpLambdas() {
  warmUpAllServices();
}

// Keep-Alive: continuously pings health endpoints every 60s until the user leaves the page
let keepAliveTimer = null;
function startKeepAlive() {
  if (keepAliveTimer) return;
  warmUpLambdas();
  keepAliveTimer = setInterval(warmUpLambdas, 60_000);
}

if (typeof window !== "undefined") {
  startKeepAlive();
}

export default api;
