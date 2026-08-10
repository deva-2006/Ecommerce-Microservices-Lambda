import Razorpay from "razorpay";
import crypto from "crypto";

function getRazorpayInstance() {
  const key_id = process.env.RAZORPAY_KEY_ID || process.env.VITE_RAZORPAY_KEY_ID || "rzp_test_TGTUEnN9TtOs0I";
  const key_secret = process.env.RAZORPAY_KEY_SECRET || "17bdz31Y7Du0PH2TaUyO7c06";

  return {
    razorpay: new Razorpay({ key_id, key_secret }),
    key_secret,
  };
}

export async function handleCreateOrder(req, res) {
  try {
    let body = req.body;
    if (typeof body === "string") {
      try { body = JSON.parse(body); } catch {}
    }
    const amount = Number(body?.amount);
    const currency = body?.currency || "INR";
    const receipt = String(body?.receipt || `rcpt_${Date.now()}`).slice(0, 40);

    if (!amount || isNaN(amount) || amount < 100) {
      res.statusCode = 400;
      res.setHeader("Content-Type", "application/json");
      return res.end(JSON.stringify({ error: "Amount must be at least 100 paise (₹1)." }));
    }

    const { razorpay } = getRazorpayInstance();
    const orderOptions = {
      amount: Math.round(amount),
      currency,
      receipt,
    };

    const order = await razorpay.orders.create(orderOptions);
    res.statusCode = 200;
    res.setHeader("Content-Type", "application/json");
    return res.end(JSON.stringify({
      order_id: order.id,
      amount: order.amount,
      currency: order.currency,
      receipt: order.receipt,
    }));
  } catch (err) {
    console.error("Razorpay Order Creation Error:", err);
    const errMsg = err.error?.description || err.description || err.message || "Internal server error during order creation";
    res.statusCode = err.statusCode || 500;
    res.setHeader("Content-Type", "application/json");
    return res.end(JSON.stringify({ error: errMsg }));
  }
}

export async function handleVerifyPayment(req, res) {
  try {
    let body = req.body;
    if (typeof body === "string") {
      try { body = JSON.parse(body); } catch {}
    }

    const { razorpay_payment_id, razorpay_order_id, razorpay_signature } = body || {};

    if (!razorpay_payment_id || !razorpay_order_id || !razorpay_signature) {
      res.statusCode = 400;
      res.setHeader("Content-Type", "application/json");
      return res.end(JSON.stringify({ error: "Missing required fields: razorpay_payment_id, razorpay_order_id, razorpay_signature" }));
    }

    const { key_secret } = getRazorpayInstance();
    const generated_signature = crypto
      .createHmac("sha256", key_secret)
      .update(`${razorpay_order_id}|${razorpay_payment_id}`)
      .digest("hex");

    if (generated_signature === razorpay_signature) {
      res.statusCode = 200;
      res.setHeader("Content-Type", "application/json");
      return res.end(JSON.stringify({
        success: true,
        message: "Payment verified successfully",
        payment_id: razorpay_payment_id,
        order_id: razorpay_order_id,
      }));
    } else {
      res.statusCode = 400;
      res.setHeader("Content-Type", "application/json");
      return res.end(JSON.stringify({
        success: false,
        error: "Signature verification failed",
      }));
    }
  } catch (err) {
    console.error("Razorpay Signature Verification Error:", err);
    const errMsg = err.error?.description || err.description || err.message || "Internal server error during payment verification";
    res.statusCode = err.statusCode || 500;
    res.setHeader("Content-Type", "application/json");
    return res.end(JSON.stringify({ error: errMsg }));
  }
}
