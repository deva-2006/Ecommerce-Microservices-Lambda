package com.deva.notification;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationSqsHandler implements RequestHandler<SQSEvent, Void> {


    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SesClient sesClient = SesClient.builder().region(Region.US_EAST_1).build();
    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder().region(Region.US_EAST_1).build();
    private static final String FROM_EMAIL = System.getenv("FROM_EMAIL");
    private static final String ORDERS_TABLE = "Orders";
    private static final String STORE_NAME = "ShopVibe";
    private static final String FRONTEND_URL = "https://dhvfhexmyhpvv.cloudfront.net";

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                JsonNode snsEnvelope = objectMapper.readTree(message.getBody());
                String innerMessage = snsEnvelope.get("Message").asText();
                JsonNode payload = objectMapper.readTree(innerMessage);

                String orderId = payload.get("orderId").asText();
                String email = payload.get("email").asText();
                double amount = payload.has("amount") ? payload.get("amount").asDouble() : 0.0;
                String paymentMethod = payload.has("paymentMethod") ? payload.get("paymentMethod").asText() : "N/A";
                String paymentId = payload.has("paymentId") ? payload.get("paymentId").asText() : "N/A";
                String timestamp = payload.has("timestamp") ? payload.get("timestamp").asText() : Instant.now().toString();

                Map<String, Object> orderDetails = fetchOrderDetails(orderId);
                String shippingAddress = orderDetails.containsKey("shippingAddress")
                        ? String.valueOf(orderDetails.get("shippingAddress")) : "Address not available";
                List<String[]> items = parseItems(orderDetails);
                double totalAmount = orderDetails.containsKey("totalAmount")
                        ? Double.parseDouble(String.valueOf(orderDetails.get("totalAmount"))) : amount;

                String formattedDate = formatTimestamp(timestamp);
                String shortOrderId = orderId.length() > 8 ? orderId.substring(0, 8) : orderId;

                sendEmail(orderId, email, amount, totalAmount, paymentMethod, paymentId,
                        formattedDate, shippingAddress, items, shortOrderId);

            } catch (Exception e) {
                throw new RuntimeException("Failed processing SQS message", e);
            }
        }
        return null;
    }

    private Map<String, Object> fetchOrderDetails(String orderId) {
        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(ORDERS_TABLE)
                    .key(Map.of("orderId", AttributeValue.builder().s(orderId).build()))
                    .build();
            GetItemResponse response = dynamoDb.getItem(request);
            if (response.hasItem()) {
                Map<String, Object> order = new HashMap<>();
                Map<String, AttributeValue> item = response.item();
                if (item.containsKey("shippingAddress")) order.put("shippingAddress", item.get("shippingAddress").s());
                if (item.containsKey("totalAmount")) order.put("totalAmount", Double.parseDouble(item.get("totalAmount").n()));
                if (item.containsKey("items")) {
                    List<Map<String, Object>> items = new ArrayList<>();
                    for (AttributeValue av : item.get("items").l()) {
                        Map<String, Object> itemMap = new HashMap<>();
                        Map<String, AttributeValue> m = av.m();
                        if (m.containsKey("productName")) itemMap.put("productName", m.get("productName").s());
                        if (m.containsKey("quantity")) itemMap.put("quantity", Integer.parseInt(m.get("quantity").n()));
                        if (m.containsKey("price")) itemMap.put("price", Double.parseDouble(m.get("price").n()));
                        if (m.containsKey("subtotal")) itemMap.put("subtotal", Double.parseDouble(m.get("subtotal").n()));
                        items.add(itemMap);
                    }
                    order.put("items", items);
                }
                return order;
            }
        } catch (Exception e) {
            // Order fetch failed — return empty map
        }
        return Map.of();
    }

    private List<String[]> parseItems(Map<String, Object> orderDetails) {
        List<String[]> items = new ArrayList<>();
        if (orderDetails.containsKey("items") && orderDetails.get("items") instanceof List) {
            for (Object obj : (List<?>) orderDetails.get("items")) {
                if (obj instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) obj;
                    String name = m.containsKey("productName") ? String.valueOf(m.get("productName")) : "Item";
                    int qty = m.containsKey("quantity") ? Integer.parseInt(String.valueOf(m.get("quantity"))) : 1;
                    double price = m.containsKey("price") ? Double.parseDouble(String.valueOf(m.get("price"))) : 0.0;
                    items.add(new String[]{name, String.valueOf(qty), String.format("%.2f", price)});
                }
            }
        }
        return items;
    }

    private String formatTimestamp(String ts) {
        try {
            Instant instant = Instant.parse(ts);
            return DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
                    .withZone(ZoneId.of("Asia/Kolkata"))
                    .format(instant);
        } catch (Exception e) {
            return ts;
        }
    }

    private String buildItemRowsHtml(List<String[]> items) {
        StringBuilder sb = new StringBuilder();
        for (String[] item : items) {
            sb.append(String.format("""
                    <tr>
                      <td style="padding:10px 12px;border-bottom:1px solid #f3f4f6;font-size:14px;color:#374151;">%s</td>
                      <td style="padding:10px 12px;border-bottom:1px solid #f3f4f6;font-size:14px;color:#6b7280;text-align:center;">%s</td>
                      <td style="padding:10px 12px;border-bottom:1px solid #f3f4f6;font-size:14px;color:#374151;text-align:right;">&#8377;%s</td>
                    </tr>
                    """, item[0], item[1], item[2]));
        }
        return sb.toString();
    }

    private String buildItemRowsPlain(List<String[]> items) {
        StringBuilder sb = new StringBuilder();
        for (String[] item : items) {
            sb.append(String.format("  %-30s x%-4s Rs. %s%n", item[0], item[1], item[2]));
        }
        return sb.toString();
    }

    private void sendEmail(String orderId, String email, double amount, double totalAmount,
                           String paymentMethod, String paymentId, String date,
                           String shippingAddress, List<String[]> items, String shortOrderId) {

        String itemRowsHtml = buildItemRowsHtml(items);
        String itemRowsPlain = buildItemRowsPlain(items);
        String totalFormatted = String.format("%.2f", totalAmount);
        String amountFormatted = String.format("%.2f", amount);
        String orderUrl = FRONTEND_URL + "/order-confirmation.html?id=" + orderId;

        String itemSectionHtml = items.isEmpty() ? "" : """
                <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top:20px;">
                  <tr>
                    <td style="padding:0 0 10px;font-size:11px;font-weight:600;color:#9ca3af;text-transform:uppercase;letter-spacing:0.5px;">Items Ordered</td>
                  </tr>
                </table>
                <table width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #e5e7eb;border-radius:8px;overflow:hidden;">
                  <tr style="background-color:#f9fafb;">
                    <td style="padding:10px 12px;font-size:11px;font-weight:600;color:#9ca3af;text-transform:uppercase;letter-spacing:0.5px;">Product</td>
                    <td style="padding:10px 12px;font-size:11px;font-weight:600;color:#9ca3af;text-transform:uppercase;letter-spacing:0.5px;text-align:center;">Qty</td>
                    <td style="padding:10px 12px;font-size:11px;font-weight:600;color:#9ca3af;text-transform:uppercase;letter-spacing:0.5px;text-align:right;">Price</td>
                  </tr>
                  %s
                  <tr>
                    <td colspan="2" style="padding:12px;font-size:14px;font-weight:700;color:#374151;border-top:2px solid #e5e7eb;">Total</td>
                    <td style="padding:12px;font-size:16px;font-weight:700;color:#6c5ce7;text-align:right;border-top:2px solid #e5e7eb;">&#8377;%s</td>
                  </tr>
                </table>
                """.formatted(itemRowsHtml, totalFormatted);

        String htmlBody = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Order Confirmed</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f9;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.06);">

                          <tr>
                            <td style="background:linear-gradient(135deg,#6c5ce7,#a855f7);padding:32px 40px;text-align:center;">
                              <h1 style="margin:0;font-size:24px;font-weight:700;color:#ffffff;letter-spacing:-0.5px;">%s</h1>
                              <p style="margin:6px 0 0;font-size:13px;color:rgba(255,255,255,0.8);">Order Confirmation</p>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:32px 40px 8px;text-align:center;">
                              <div style="width:64px;height:64px;border-radius:50%%;background-color:#d1fae5;margin:0 auto 16px;line-height:64px;font-size:32px;">&#10003;</div>
                              <h2 style="margin:0;font-size:22px;color:#1a1a2e;font-weight:700;">Thank You, Your Order is Confirmed!</h2>
                              <p style="margin:8px 0 0;font-size:15px;color:#6b7280;">We have received your order and payment.</p>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:24px 40px 0;">
                              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f9fafb;border-radius:8px;border:1px solid #e5e7eb;">
                                <tr>
                                  <td style="padding:20px 24px;">
                                    <table width="100%%" cellpadding="0" cellspacing="0">
                                      <tr>
                                        <td style="padding:0 0 12px;">
                                          <p style="margin:0;font-size:11px;font-weight:600;color:#9ca3af;text-transform:uppercase;letter-spacing:0.5px;">Order ID</p>
                                          <p style="margin:4px 0 0;font-size:14px;color:#1a1a2e;font-weight:600;font-family:monospace;">%s</p>
                                        </td>
                                        <td style="padding:0 0 12px;text-align:right;">
                                          <p style="margin:0;font-size:11px;font-weight:600;color:#9ca3af;text-transform:uppercase;letter-spacing:0.5px;">Order Date</p>
                                          <p style="margin:4px 0 0;font-size:14px;color:#1a1a2e;">%s</p>
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:16px 40px 0;">
                              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f9fafb;border-radius:8px;border:1px solid #e5e7eb;">
                                <tr>
                                  <td style="padding:16px 24px;">
                                    <p style="margin:0 0 8px;font-size:11px;font-weight:600;color:#9ca3af;text-transform:uppercase;letter-spacing:0.5px;">Shipping Address</p>
                                    <p style="margin:0;font-size:14px;color:#374151;line-height:1.6;">%s</p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:16px 40px 0;">%s</td>
                          </tr>

                          <tr>
                            <td style="padding:16px 40px 0;">
                              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f9fafb;border-radius:8px;border:1px solid #e5e7eb;">
                                <tr>
                                  <td style="padding:16px 24px;">
                                    <p style="margin:0 0 10px;font-size:11px;font-weight:600;color:#9ca3af;text-transform:uppercase;letter-spacing:0.5px;">Payment Details</p>
                                    <table width="100%%" cellpadding="0" cellspacing="0">
                                      <tr>
                                        <td style="padding:0 0 6px;font-size:13px;color:#6b7280;">Payment ID</td>
                                        <td style="padding:0 0 6px;font-size:13px;color:#374151;text-align:right;font-family:monospace;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="padding:0 0 6px;font-size:13px;color:#6b7280;">Method</td>
                                        <td style="padding:0 0 6px;font-size:13px;color:#374151;text-align:right;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="padding:0 0 6px;font-size:13px;color:#6b7280;">Status</td>
                                        <td style="padding:0 0 6px;font-size:13px;text-align:right;"><span style="background:#d1fae5;color:#059669;padding:2px 8px;border-radius:4px;font-size:12px;font-weight:600;">PAID</span></td>
                                      </tr>
                                      <tr>
                                        <td style="padding:12px 0 0;font-size:14px;font-weight:600;color:#374151;border-top:1px solid #e5e7eb;">Amount Paid</td>
                                        <td style="padding:12px 0 0;font-size:22px;font-weight:700;color:#6c5ce7;text-align:right;border-top:1px solid #e5e7eb;">&#8377;%s</td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:28px 40px 0;text-align:center;">
                              <a href="%s" style="display:inline-block;padding:14px 32px;background:linear-gradient(135deg,#6c5ce7,#a855f7);color:#ffffff;font-size:15px;font-weight:600;text-decoration:none;border-radius:8px;">View Order Details</a>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:24px 40px 0;text-align:center;">
                              <p style="margin:0;font-size:13px;color:#9ca3af;">
                                Questions? Contact us at <a href="mailto:deva.s.professional@gmail.com" style="color:#6c5ce7;text-decoration:none;">deva.s.professional@gmail.com</a>
                              </p>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:28px 40px 0;">
                              <table width="100%%" cellpadding="0" cellspacing="0" style="background:linear-gradient(135deg,#6c5ce7,#a855f7);border-radius:12px;overflow:hidden;">
                                <tr>
                                  <td style="padding:28px 24px;text-align:center;">
                                    <p style="margin:0 0 4px;font-size:14px;font-weight:700;color:#ffffff;">Stay Connected</p>
                                    <p style="margin:0 0 20px;font-size:12px;color:rgba(255,255,255,0.8);">Follow us for updates, deals, and more.</p>
                                    <table cellpadding="0" cellspacing="0" style="margin:0 auto;">
                                      <tr>
                                        <td style="padding:0 6px;">
                                          <a href="mailto:deva.s.professional@gmail.com" style="display:inline-block;width:44px;height:44px;border-radius:50%%;background:rgba(255,255,255,0.2);text-align:center;line-height:44px;text-decoration:none;font-size:18px;" title="Email Us">&#9993;</a>
                                        </td>
                                        <td style="padding:0 6px;">
                                          <a href="https://linkedin.com/in/deva21" style="display:inline-block;width:44px;height:44px;border-radius:50%%;background:rgba(255,255,255,0.2);text-align:center;line-height:44px;text-decoration:none;font-size:18px;" title="LinkedIn">in</a>
                                        </td>
                                        <td style="padding:0 6px;">
                                          <a href="https://instagram.com/devzz_21" style="display:inline-block;width:44px;height:44px;border-radius:50%%;background:rgba(255,255,255,0.2);text-align:center;line-height:44px;text-decoration:none;font-size:18px;" title="Instagram">&#9733;</a>
                                        </td>
                                        <td style="padding:0 6px;">
                                          <a href="tel:+919363090510" style="display:inline-block;width:44px;height:44px;border-radius:50%%;background:rgba(255,255,255,0.2);text-align:center;line-height:44px;text-decoration:none;font-size:18px;" title="Call Us">&#9742;</a>
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <tr>
                            <td style="background-color:#f9fafb;padding:24px 40px;border-top:1px solid #e5e7eb;margin-top:24px;text-align:center;">
                              <p style="margin:0 0 6px;font-size:15px;font-weight:700;color:#6c5ce7;">ShopVibe</p>
                              <p style="margin:0 0 12px;font-size:12px;color:#6b7280;line-height:1.6;">Your one-stop destination for premium products at unbeatable prices. Shop smart, live better.</p>
                              <table cellpadding="0" cellspacing="0" style="margin:0 auto 12px;">
                                <tr>
                                  <td style="padding:0 8px;font-size:12px;color:#9ca3af;">
                                    <a href="tel:+919363090510" style="color:#6c5ce7;text-decoration:none;">&#9742; +91 9363090510</a>
                                  </td>
                                  <td style="padding:0 8px;font-size:12px;color:#d1d5db;">|</td>
                                  <td style="padding:0 8px;font-size:12px;color:#9ca3af;">
                                    <a href="mailto:deva.s.professional@gmail.com" style="color:#6c5ce7;text-decoration:none;">&#9993; Email Us</a>
                                  </td>
                                  <td style="padding:0 8px;font-size:12px;color:#d1d5db;">|</td>
                                  <td style="padding:0 8px;font-size:12px;color:#9ca3af;">
                                    <a href="https://instagram.com/devzz_21" style="color:#6c5ce7;text-decoration:none;">&#9733; Instagram</a>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:12px 0 0;font-size:11px;color:#d1d5db;">&copy; 2026 %s. All rights reserved.</p>
                              <p style="margin:4px 0 0;font-size:10px;color:#e5e7eb;">This is a transactional email regarding your recent order.</p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                STORE_NAME,
                orderId, date,
                shippingAddress,
                itemSectionHtml,
                paymentId, paymentMethod, amountFormatted,
                orderUrl,
                STORE_NAME
        );

        String itemSectionPlain = items.isEmpty() ? "" : """
                Items Ordered:
                %s
                -----------------------------------------------
                %-30s Rs. %s
                """.formatted(itemRowsPlain.trim(), "Total", totalFormatted);

        String plainText = """
                ================================================
                  %s - Order Confirmation
                ================================================

                Hi,

                Thank you for your order! Here are the details:

                -----------------------------------------------
                ORDER DETAILS
                -----------------------------------------------
                Order ID:       %s
                Order Date:     %s

                -----------------------------------------------
                SHIPPING ADDRESS
                -----------------------------------------------
                %s

                -----------------------------------------------
                ITEMS ORDERED
                -----------------------------------------------
                %s
                -----------------------------------------------

                -----------------------------------------------
                PAYMENT DETAILS
                -----------------------------------------------
                Payment ID:     %s
                Method:         %s
                Status:         PAID
                Amount Paid:    Rs. %s

                -----------------------------------------------

                View your order: %s

                -----------------------------------------------
                STAY CONNECTED
                -----------------------------------------------
                Email:    deva.s.professional@gmail.com
                Phone:    +91 9363090510
                LinkedIn: linkedin.com/in/deva21
                Instagram: instagram.com/devzz_21
                -----------------------------------------------

                Questions? Contact us at deva.s.professional@gmail.com

                -----------------------------------------------
                ShopVibe - Your one-stop destination for premium products
                at unbeatable prices. Shop smart, live better.
                -----------------------------------------------
                (C) 2026 %s. All rights reserved.
                This is a transactional email regarding your recent order.
                ================================================
                """.formatted(
                STORE_NAME,
                orderId, date,
                shippingAddress,
                itemSectionPlain.trim(),
                paymentId, paymentMethod, amountFormatted,
                orderUrl,
                STORE_NAME
        );

        SendEmailRequest request = SendEmailRequest.builder()
                .source(STORE_NAME + " <" + FROM_EMAIL + ">")
                .destination(Destination.builder().toAddresses(email).build())
                .message(Message.builder()
                        .subject(Content.builder()
                                .data("Your " + STORE_NAME + " Order #" + shortOrderId + " is Confirmed")
                                .build())
                        .body(Body.builder()
                                .html(Content.builder().data(htmlBody).build())
                                .text(Content.builder().data(plainText).build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
    }
}
// Snyk trigger 4



