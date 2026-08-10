import { defineConfig, loadEnv } from "vite";
import { resolve } from "path";
import { handleCreateOrder, handleVerifyPayment } from "./api/razorpay-server.js";

function razorpayDevApiPlugin() {
  return {
    name: "razorpay-dev-api",
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const url = req.url.split("?")[0];
        if (req.method === "POST" && (url === "/api/create-order" || url === "/api/verify-payment")) {
          let body = "";
          req.on("data", (chunk) => { body += chunk.toString(); });
          req.on("end", () => {
            req.body = body;
            if (url === "/api/create-order") {
              handleCreateOrder(req, res);
            } else {
              handleVerifyPayment(req, res);
            }
          });
          return;
        }
        next();
      });
    }
  };
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  Object.assign(process.env, env);

  return {
    plugins: [razorpayDevApiPlugin()],
    root: "src",
    base: "./",
    build: {
      target: "es2022",
      outDir: "../dist",
      rollupOptions: {
        input: {
          main: resolve(__dirname, "src/index.html"),
          signup: resolve(__dirname, "src/signup.html"),
          confirm: resolve(__dirname, "src/confirm.html"),
          login: resolve(__dirname, "src/login.html"),
          home: resolve(__dirname, "src/home.html"),
          product: resolve(__dirname, "src/product.html"),
          cart: resolve(__dirname, "src/cart.html"),
          checkout: resolve(__dirname, "src/checkout.html"),
          orders: resolve(__dirname, "src/orders.html"),
          profile: resolve(__dirname, "src/profile.html"),
          adminDashboard: resolve(__dirname, "src/admin/dashboard.html"),
          adminProducts: resolve(__dirname, "src/admin/products.html"),
          adminInventory: resolve(__dirname, "src/admin/inventory.html"),
          adminOrders: resolve(__dirname, "src/admin/orders.html"),
          adminUsers: resolve(__dirname, "src/admin/users.html"),
          adminReviews: resolve(__dirname, "src/admin/reviews.html"),
          orderConfirmation: resolve(__dirname, "src/order-confirmation.html"),
          notFound: resolve(__dirname, "src/404.html")
        }
      }
    }
  };
});
