variable "project_name" { type = string }
variable "environment"  { type = string }

resource "aws_sns_topic" "payment_events" {
  name = "payment-events"
  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# SQS Queue for Order Service
resource "aws_sqs_queue" "order_queue" {
  name                      = "order-payment-consumer-queue"
  message_retention_seconds = 86400
  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# SQS Queue for Notification Service
resource "aws_sqs_queue" "notification_queue" {
  name                      = "notification-consumer-queue"
  message_retention_seconds = 86400
  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# SQS Policy allowing SNS to publish to Order Queue
resource "aws_sqs_queue_policy" "order_queue_policy" {
  queue_url = aws_sqs_queue.order_queue.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = "*"
        Action    = "sqs:SendMessage"
        Resource  = aws_sqs_queue.order_queue.arn
        Condition = {
          ArnEquals = {
            "aws:SourceArn" = aws_sns_topic.payment_events.arn
          }
        }
      }
    ]
  })
}

# SQS Policy allowing SNS to publish to Notification Queue
resource "aws_sqs_queue_policy" "notification_queue_policy" {
  queue_url = aws_sqs_queue.notification_queue.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = "*"
        Action    = "sqs:SendMessage"
        Resource  = aws_sqs_queue.notification_queue.arn
        Condition = {
          ArnEquals = {
            "aws:SourceArn" = aws_sns_topic.payment_events.arn
          }
        }
      }
    ]
  })
}

# SNS Subscriptions
resource "aws_sns_topic_subscription" "order_subscription" {
  topic_arn = aws_sns_topic.payment_events.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.order_queue.arn
}

resource "aws_sns_topic_subscription" "notification_subscription" {
  topic_arn = aws_sns_topic.payment_events.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.notification_queue.arn
}

output "sns_topic_arn" {
  value = aws_sns_topic.payment_events.arn
}

output "order_queue_arn" {
  value = aws_sqs_queue.order_queue.arn
}

output "notification_queue_arn" {
  value = aws_sqs_queue.notification_queue.arn
}
