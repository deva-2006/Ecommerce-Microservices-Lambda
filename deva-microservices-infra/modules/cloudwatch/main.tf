variable "project_name" { type = string }
variable "environment"  { type = string }
variable "aws_region"   { type = string }

# ── Variables passed from root ──────────────────────────────────────────────
variable "api_id"             { type = string }
variable "sns_topic_name"     { type = string }
variable "order_queue_name"   { type = string }
variable "notification_queue_name" { type = string }

locals {
  lambdas = [
    "product-service",
    "inventory-service",
    "cart-service",
    "order-service",
    "payment-service",
    "review-service",
    "notification-consumer-lambda",
    "order-payment-consumer-lambda"
  ]
  tables = ["Products", "Inventory", "Cart", "Orders", "Payments", "Reviews"]

  # Build one Lambda widget per service (invocations + errors + duration)
  lambda_widgets = [for fn in local.lambdas : {
    type   = "metric"
    x      = 0
    y      = 0   # will be overridden by dashboard JSON array ordering
    width  = 8
    height = 6
    properties = {
      title  = "Lambda: ${fn}"
      region = var.aws_region
      stat   = "Sum"
      period = 60
      view   = "timeSeries"
      metrics = [
        ["AWS/Lambda", "Invocations",          "FunctionName", fn, { label = "Invocations", color = "#2563eb" }],
        ["AWS/Lambda", "Errors",               "FunctionName", fn, { label = "Errors",      color = "#dc2626" }],
        ["AWS/Lambda", "Throttles",            "FunctionName", fn, { label = "Throttles",   color = "#f97316" }],
        ["AWS/Lambda", "Duration",             "FunctionName", fn, { label = "Duration ms", color = "#7c3aed", stat = "p95" }],
        ["AWS/Lambda", "ConcurrentExecutions", "FunctionName", fn, { label = "Concurrency", color = "#0891b2", stat = "Maximum" }]
      ]
    }
  }]
}

resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "${var.project_name}-${var.environment}-dashboard"

  dashboard_body = jsonencode({
    widgets = [

      # ═══════════════════════════════════════════════════════════
      # ROW 0 — Section Header: Overview
      # ═══════════════════════════════════════════════════════════
      {
        type   = "text"
        x      = 0; y = 0; width = 24; height = 2
        properties = {
          markdown = "# 🛒 ShopVibe Microservices — Live Operations Dashboard\n**Environment**: `${var.environment}` | **Region**: `${var.aws_region}` | **Stack**: 8 Lambda Functions · API Gateway · 6 DynamoDB Tables · SQS · SNS"
        }
      },

      # ═══════════════════════════════════════════════════════════
      # ROW 1 — API Gateway Overview (full width)
      # ═══════════════════════════════════════════════════════════
      {
        type   = "text"
        x      = 0; y = 2; width = 24; height = 1
        properties = { markdown = "## 🌐 API Gateway" }
      },
      {
        type   = "metric"
        x      = 0; y = 3; width = 6; height = 6
        properties = {
          title  = "Total Requests"
          region = var.aws_region
          stat   = "Sum"
          period = 60
          view   = "timeSeries"
          metrics = [["AWS/ApiGateway", "Count", "ApiId", var.api_id, { label = "Requests", color = "#2563eb" }]]
        }
      },
      {
        type   = "metric"
        x      = 6; y = 3; width = 6; height = 6
        properties = {
          title  = "4xx & 5xx Errors"
          region = var.aws_region
          stat   = "Sum"
          period = 60
          view   = "timeSeries"
          metrics = [
            ["AWS/ApiGateway", "4XXError", "ApiId", var.api_id, { label = "4xx Client Error", color = "#f97316" }],
            ["AWS/ApiGateway", "5XXError", "ApiId", var.api_id, { label = "5xx Server Error", color = "#dc2626" }]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12; y = 3; width = 6; height = 6
        properties = {
          title  = "Latency (p50 / p95 / p99)"
          region = var.aws_region
          period = 60
          view   = "timeSeries"
          metrics = [
            ["AWS/ApiGateway", "Latency", "ApiId", var.api_id, { label = "p50 Latency", stat = "p50", color = "#10b981" }],
            ["AWS/ApiGateway", "Latency", "ApiId", var.api_id, { label = "p95 Latency", stat = "p95", color = "#f59e0b" }],
            ["AWS/ApiGateway", "Latency", "ApiId", var.api_id, { label = "p99 Latency", stat = "p99", color = "#dc2626" }]
          ]
        }
      },
      {
        type   = "metric"
        x      = 18; y = 3; width = 6; height = 6
        properties = {
          title  = "Integration Latency (Lambda time)"
          region = var.aws_region
          period = 60
          view   = "timeSeries"
          metrics = [
            ["AWS/ApiGateway", "IntegrationLatency", "ApiId", var.api_id, { label = "p50", stat = "p50", color = "#7c3aed" }],
            ["AWS/ApiGateway", "IntegrationLatency", "ApiId", var.api_id, { label = "p95", stat = "p95", color = "#e11d48" }]
          ]
        }
      },

      # ═══════════════════════════════════════════════════════════
      # ROW 2 — Lambda Functions
      # ═══════════════════════════════════════════════════════════
      {
        type   = "text"
        x      = 0; y = 9; width = 24; height = 1
        properties = { markdown = "## ⚡ Lambda Functions — Invocations · Errors · Duration" }
      },

      # product-service
      {
        type = "metric"; x = 0; y = 10; width = 8; height = 6
        properties = {
          title   = "product-service"
          region  = var.aws_region
          period  = 60
          view    = "timeSeries"
          metrics = [
            ["AWS/Lambda", "Invocations", "FunctionName", "product-service",          { label = "Invocations", stat = "Sum",     color = "#2563eb" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "product-service",          { label = "Errors",      stat = "Sum",     color = "#dc2626" }],
            ["AWS/Lambda", "Duration",    "FunctionName", "product-service",          { label = "p95 ms",      stat = "p95",     color = "#7c3aed" }],
            ["AWS/Lambda", "Throttles",   "FunctionName", "product-service",          { label = "Throttles",   stat = "Sum",     color = "#f97316" }]
          ]
        }
      },

      # inventory-service
      {
        type = "metric"; x = 8; y = 10; width = 8; height = 6
        properties = {
          title   = "inventory-service"
          region  = var.aws_region
          period  = 60
          view    = "timeSeries"
          metrics = [
            ["AWS/Lambda", "Invocations", "FunctionName", "inventory-service",        { label = "Invocations", stat = "Sum",  color = "#2563eb" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "inventory-service",        { label = "Errors",      stat = "Sum",  color = "#dc2626" }],
            ["AWS/Lambda", "Duration",    "FunctionName", "inventory-service",        { label = "p95 ms",      stat = "p95",  color = "#7c3aed" }],
            ["AWS/Lambda", "Throttles",   "FunctionName", "inventory-service",        { label = "Throttles",   stat = "Sum",  color = "#f97316" }]
          ]
        }
      },

      # cart-service
      {
        type = "metric"; x = 16; y = 10; width = 8; height = 6
        properties = {
          title   = "cart-service"
          region  = var.aws_region
          period  = 60
          view    = "timeSeries"
          metrics = [
            ["AWS/Lambda", "Invocations", "FunctionName", "cart-service",             { label = "Invocations", stat = "Sum",  color = "#2563eb" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "cart-service",             { label = "Errors",      stat = "Sum",  color = "#dc2626" }],
            ["AWS/Lambda", "Duration",    "FunctionName", "cart-service",             { label = "p95 ms",      stat = "p95",  color = "#7c3aed" }],
            ["AWS/Lambda", "Throttles",   "FunctionName", "cart-service",             { label = "Throttles",   stat = "Sum",  color = "#f97316" }]
          ]
        }
      },

      # order-service
      {
        type = "metric"; x = 0; y = 16; width = 8; height = 6
        properties = {
          title   = "order-service"
          region  = var.aws_region
          period  = 60
          view    = "timeSeries"
          metrics = [
            ["AWS/Lambda", "Invocations", "FunctionName", "order-service",            { label = "Invocations", stat = "Sum",  color = "#2563eb" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "order-service",            { label = "Errors",      stat = "Sum",  color = "#dc2626" }],
            ["AWS/Lambda", "Duration",    "FunctionName", "order-service",            { label = "p95 ms",      stat = "p95",  color = "#7c3aed" }],
            ["AWS/Lambda", "Throttles",   "FunctionName", "order-service",            { label = "Throttles",   stat = "Sum",  color = "#f97316" }]
          ]
        }
      },

      # payment-service
      {
        type = "metric"; x = 8; y = 16; width = 8; height = 6
        properties = {
          title   = "payment-service"
          region  = var.aws_region
          period  = 60
          view    = "timeSeries"
          metrics = [
            ["AWS/Lambda", "Invocations", "FunctionName", "payment-service",          { label = "Invocations", stat = "Sum",  color = "#2563eb" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "payment-service",          { label = "Errors",      stat = "Sum",  color = "#dc2626" }],
            ["AWS/Lambda", "Duration",    "FunctionName", "payment-service",          { label = "p95 ms",      stat = "p95",  color = "#7c3aed" }],
            ["AWS/Lambda", "Throttles",   "FunctionName", "payment-service",          { label = "Throttles",   stat = "Sum",  color = "#f97316" }]
          ]
        }
      },

      # review-service
      {
        type = "metric"; x = 16; y = 16; width = 8; height = 6
        properties = {
          title   = "review-service"
          region  = var.aws_region
          period  = 60
          view    = "timeSeries"
          metrics = [
            ["AWS/Lambda", "Invocations", "FunctionName", "review-service",           { label = "Invocations", stat = "Sum",  color = "#2563eb" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "review-service",           { label = "Errors",      stat = "Sum",  color = "#dc2626" }],
            ["AWS/Lambda", "Duration",    "FunctionName", "review-service",           { label = "p95 ms",      stat = "p95",  color = "#7c3aed" }],
            ["AWS/Lambda", "Throttles",   "FunctionName", "review-service",           { label = "Throttles",   stat = "Sum",  color = "#f97316" }]
          ]
        }
      },

      # Consumer Lambdas
      {
        type = "metric"; x = 0; y = 22; width = 12; height = 6
        properties = {
          title   = "notification-consumer-lambda"
          region  = var.aws_region
          period  = 60
          view    = "timeSeries"
          metrics = [
            ["AWS/Lambda", "Invocations", "FunctionName", "notification-consumer-lambda",      { stat = "Sum",  color = "#2563eb" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "notification-consumer-lambda",      { stat = "Sum",  color = "#dc2626" }],
            ["AWS/Lambda", "Duration",    "FunctionName", "notification-consumer-lambda",      { stat = "p95",  color = "#7c3aed" }]
          ]
        }
      },
      {
        type = "metric"; x = 12; y = 22; width = 12; height = 6
        properties = {
          title   = "order-payment-consumer-lambda"
          region  = var.aws_region
          period  = 60
          view    = "timeSeries"
          metrics = [
            ["AWS/Lambda", "Invocations", "FunctionName", "order-payment-consumer-lambda",     { stat = "Sum",  color = "#2563eb" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "order-payment-consumer-lambda",     { stat = "Sum",  color = "#dc2626" }],
            ["AWS/Lambda", "Duration",    "FunctionName", "order-payment-consumer-lambda",     { stat = "p95",  color = "#7c3aed" }]
          ]
        }
      },

      # ═══════════════════════════════════════════════════════════
      # ROW 3 — Cold Starts (Java is slow!)
      # ═══════════════════════════════════════════════════════════
      {
        type   = "text"
        x      = 0; y = 28; width = 24; height = 1
        properties = { markdown = "## 🥶 Cold Start Duration (Java Lambdas)" }
      },
      {
        type = "metric"; x = 0; y = 29; width = 24; height = 6
        properties = {
          title  = "Init Duration (Cold Start) — All Services"
          region = var.aws_region
          stat   = "p99"
          period = 300
          view   = "bar"
          metrics = [
            ["AWS/Lambda", "InitDuration", "FunctionName", "product-service",                 { label = "product" }],
            ["AWS/Lambda", "InitDuration", "FunctionName", "inventory-service",               { label = "inventory" }],
            ["AWS/Lambda", "InitDuration", "FunctionName", "cart-service",                    { label = "cart" }],
            ["AWS/Lambda", "InitDuration", "FunctionName", "order-service",                   { label = "order" }],
            ["AWS/Lambda", "InitDuration", "FunctionName", "payment-service",                 { label = "payment" }],
            ["AWS/Lambda", "InitDuration", "FunctionName", "review-service",                  { label = "review" }],
            ["AWS/Lambda", "InitDuration", "FunctionName", "notification-consumer-lambda",    { label = "notification" }],
            ["AWS/Lambda", "InitDuration", "FunctionName", "order-payment-consumer-lambda",   { label = "order-consumer" }]
          ]
        }
      },

      # ═══════════════════════════════════════════════════════════
      # ROW 4 — DynamoDB Tables
      # ═══════════════════════════════════════════════════════════
      {
        type   = "text"
        x      = 0; y = 35; width = 24; height = 1
        properties = { markdown = "## 🗄️ DynamoDB — Read/Write Capacity & Latency" }
      },
      {
        type = "metric"; x = 0; y = 36; width = 12; height = 6
        properties = {
          title  = "Consumed Read Capacity (all tables)"
          region = var.aws_region
          stat   = "Sum"
          period = 60
          view   = "timeSeries"
          metrics = [for t in local.tables :
            ["AWS/DynamoDB", "ConsumedReadCapacityUnits", "TableName", t, { label = t }]
          ]
        }
      },
      {
        type = "metric"; x = 12; y = 36; width = 12; height = 6
        properties = {
          title  = "Consumed Write Capacity (all tables)"
          region = var.aws_region
          stat   = "Sum"
          period = 60
          view   = "timeSeries"
          metrics = [for t in local.tables :
            ["AWS/DynamoDB", "ConsumedWriteCapacityUnits", "TableName", t, { label = t }]
          ]
        }
      },
      {
        type = "metric"; x = 0; y = 42; width = 12; height = 6
        properties = {
          title  = "DynamoDB Successful Request Latency — GetItem p99"
          region = var.aws_region
          stat   = "p99"
          period = 60
          view   = "timeSeries"
          metrics = [for t in local.tables :
            ["AWS/DynamoDB", "SuccessfulRequestLatency", "TableName", t, "Operation", "GetItem", { label = t }]
          ]
        }
      },
      {
        type = "metric"; x = 12; y = 42; width = 12; height = 6
        properties = {
          title  = "DynamoDB Errors (UserErrors + SystemErrors)"
          region = var.aws_region
          stat   = "Sum"
          period = 60
          view   = "timeSeries"
          metrics = flatten([for t in local.tables : [
            ["AWS/DynamoDB", "UserErrors",   "TableName", t, { label = "${t} UserError" }],
            ["AWS/DynamoDB", "SystemErrors", "TableName", t, { label = "${t} SystemError" }]
          ]])
        }
      },

      # ═══════════════════════════════════════════════════════════
      # ROW 5 — SQS Queues
      # ═══════════════════════════════════════════════════════════
      {
        type   = "text"
        x      = 0; y = 48; width = 24; height = 1
        properties = { markdown = "## 📬 SQS Queues — Throughput & Backlog" }
      },
      {
        type = "metric"; x = 0; y = 49; width = 8; height = 6
        properties = {
          title  = "Messages Sent to Queues"
          region = var.aws_region
          stat   = "Sum"
          period = 60
          view   = "timeSeries"
          metrics = [
            ["AWS/SQS", "NumberOfMessagesSent", "QueueName", var.order_queue_name,        { label = "Order Queue",        color = "#2563eb" }],
            ["AWS/SQS", "NumberOfMessagesSent", "QueueName", var.notification_queue_name, { label = "Notification Queue", color = "#7c3aed" }]
          ]
        }
      },
      {
        type = "metric"; x = 8; y = 49; width = 8; height = 6
        properties = {
          title  = "Queue Backlog (Age of Oldest Message)"
          region = var.aws_region
          stat   = "Maximum"
          period = 60
          view   = "timeSeries"
          metrics = [
            ["AWS/SQS", "ApproximateAgeOfOldestMessage", "QueueName", var.order_queue_name,        { label = "Order Queue Age (s)",        color = "#f97316" }],
            ["AWS/SQS", "ApproximateAgeOfOldestMessage", "QueueName", var.notification_queue_name, { label = "Notification Queue Age (s)", color = "#dc2626" }]
          ]
        }
      },
      {
        type = "metric"; x = 16; y = 49; width = 8; height = 6
        properties = {
          title  = "Messages Not Visible (In-Flight)"
          region = var.aws_region
          stat   = "Maximum"
          period = 60
          view   = "timeSeries"
          metrics = [
            ["AWS/SQS", "ApproximateNumberOfMessagesNotVisible", "QueueName", var.order_queue_name,        { label = "Order Queue",        color = "#2563eb" }],
            ["AWS/SQS", "ApproximateNumberOfMessagesNotVisible", "QueueName", var.notification_queue_name, { label = "Notification Queue", color = "#7c3aed" }]
          ]
        }
      },

      # ═══════════════════════════════════════════════════════════
      # ROW 6 — SNS
      # ═══════════════════════════════════════════════════════════
      {
        type   = "text"
        x      = 0; y = 55; width = 24; height = 1
        properties = { markdown = "## 📣 SNS — Event Publishing (payment-events topic)" }
      },
      {
        type = "metric"; x = 0; y = 56; width = 8; height = 6
        properties = {
          title  = "Messages Published"
          region = var.aws_region
          stat   = "Sum"
          period = 60
          view   = "timeSeries"
          metrics = [["AWS/SNS", "NumberOfMessagesPublished", "TopicName", var.sns_topic_name, { color = "#10b981" }]]
        }
      },
      {
        type = "metric"; x = 8; y = 56; width = 8; height = 6
        properties = {
          title  = "Notifications Delivered vs Failed"
          region = var.aws_region
          stat   = "Sum"
          period = 60
          view   = "timeSeries"
          metrics = [
            ["AWS/SNS", "NumberOfNotificationsDelivered", "TopicName", var.sns_topic_name, { label = "Delivered", color = "#10b981" }],
            ["AWS/SNS", "NumberOfNotificationsFailed",    "TopicName", var.sns_topic_name, { label = "Failed",    color = "#dc2626" }]
          ]
        }
      },
      {
        type = "metric"; x = 16; y = 56; width = 8; height = 6
        properties = {
          title  = "Overall Lambda Error Rate % (All Services)"
          region = var.aws_region
          period = 60
          view   = "timeSeries"
          metrics = [
            [{ expression = "(e1+e2+e3+e4+e5+e6)/(i1+i2+i3+i4+i5+i6)*100", label = "Error Rate %", color = "#dc2626" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "product-service",   { id = "e1", visible = false, stat = "Sum" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "inventory-service", { id = "e2", visible = false, stat = "Sum" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "cart-service",      { id = "e3", visible = false, stat = "Sum" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "order-service",     { id = "e4", visible = false, stat = "Sum" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "payment-service",   { id = "e5", visible = false, stat = "Sum" }],
            ["AWS/Lambda", "Errors",      "FunctionName", "review-service",    { id = "e6", visible = false, stat = "Sum" }],
            ["AWS/Lambda", "Invocations", "FunctionName", "product-service",   { id = "i1", visible = false, stat = "Sum" }],
            ["AWS/Lambda", "Invocations", "FunctionName", "inventory-service", { id = "i2", visible = false, stat = "Sum" }],
            ["AWS/Lambda", "Invocations", "FunctionName", "cart-service",      { id = "i3", visible = false, stat = "Sum" }],
            ["AWS/Lambda", "Invocations", "FunctionName", "order-service",     { id = "i4", visible = false, stat = "Sum" }],
            ["AWS/Lambda", "Invocations", "FunctionName", "payment-service",   { id = "i5", visible = false, stat = "Sum" }],
            ["AWS/Lambda", "Invocations", "FunctionName", "review-service",    { id = "i6", visible = false, stat = "Sum" }]
          ]
        }
      }
    ]
  })
}

output "dashboard_url" {
  value       = "https://${var.aws_region}.console.aws.amazon.com/cloudwatch/home?region=${var.aws_region}#dashboards:name=${var.project_name}-${var.environment}-dashboard"
  description = "Direct URL to the CloudWatch dashboard"
}
