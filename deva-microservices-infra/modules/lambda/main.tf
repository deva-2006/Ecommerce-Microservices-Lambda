variable "project_name" { type = string }
variable "environment"  { type = string }
variable "aws_region"   { type = string }

variable "from_email"                 { type = string }
variable "product_images_bucket_name" { type = string }
variable "product_images_bucket_arn"  { type = string }

variable "cognito_user_pool_id"   { type = string }
variable "cognito_client_id"      { type = string }
variable "sns_topic_arn"          { type = string }
variable "order_queue_arn"        { type = string }
variable "notification_queue_arn" { type = string }

variable "products_table_name"   { type = string }
variable "inventory_table_name"  { type = string }
variable "cart_table_name"       { type = string }
variable "orders_table_name"     { type = string }
variable "payments_table_name"   { type = string }
variable "reviews_table_name"    { type = string }

# Create a placeholder ZIP file using Archive provider
data "archive_file" "placeholder" {
  type        = "zip"
  output_path = "${path.module}/placeholder.zip"

  source {
    content  = "placeholder"
    filename = "placeholder.txt"
  }
}

# General Lambda Execution Role
resource "aws_iam_role" "lambda_role" {
  name = "${var.project_name}-lambda-execution-role-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

# IAM Policy for Lambda Execution Role
resource "aws_iam_role_policy" "lambda_policy" {
  name = "${var.project_name}-lambda-policy-${var.environment}"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Effect = "Allow"
        Action = [
          "xray:PutTraceSegments",
          "xray:PutTelemetryRecords"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "dynamodb:Scan",
          "dynamodb:Query",
          "dynamodb:BatchWriteItem",
          "dynamodb:BatchGetItem"
        ]
        Resource = "arn:aws:dynamodb:${var.aws_region}:*:table/*"
      },
      {
        Effect = "Allow"
        Action = [
          "sns:Publish"
        ]
        Resource = var.sns_topic_arn
      },
      {
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = [
          var.order_queue_arn,
          var.notification_queue_arn
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "ses:SendEmail",
          "ses:SendRawEmail"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:PutObjectAcl"
        ]
        Resource = [
          var.product_images_bucket_arn,
          "${var.product_images_bucket_arn}/*"
        ]
      }
    ]
  })
}

# Helper local for common settings
locals {
  common_env = {
    COGNITO_USER_POOL_ID   = var.cognito_user_pool_id
    COGNITO_CLIENT_ID      = var.cognito_client_id
    SNS_TOPIC_ARN          = var.sns_topic_arn
    PRODUCTS_TABLE         = var.products_table_name
    INVENTORY_TABLE        = var.inventory_table_name
    CART_TABLE             = var.cart_table_name
    ORDERS_TABLE           = var.orders_table_name
    PAYMENTS_TABLE         = var.payments_table_name
    REVIEWS_TABLE          = var.reviews_table_name
    FROM_EMAIL             = var.from_email
    S3_BUCKET_NAME         = var.product_images_bucket_name
    AWS_S3_BUCKET_NAME     = var.product_images_bucket_name
  }
}

# 1. Product Service
resource "aws_lambda_function" "product_service" {
  function_name    = "product-service"
  role             = aws_iam_role.lambda_role.arn
  handler          = "com.deva.productservice.StreamLambdaHandler::handleRequest"
  runtime          = "java21"
  filename         = data.archive_file.placeholder.output_path
  source_code_hash = data.archive_file.placeholder.output_base64sha256
  timeout          = 30
  memory_size      = 1024

  tracing_config {
    mode = "Active"
  }

  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = local.common_env
  }
}

# 2. Inventory Service
resource "aws_lambda_function" "inventory_service" {
  function_name    = "inventory-service"
  role             = aws_iam_role.lambda_role.arn
  handler          = "com.deva.inventoryservice.StreamLambdaHandler::handleRequest"
  runtime          = "java21"
  filename         = data.archive_file.placeholder.output_path
  source_code_hash = data.archive_file.placeholder.output_base64sha256
  timeout          = 30
  memory_size      = 1024

  tracing_config {
    mode = "Active"
  }

  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = local.common_env
  }
}

# 3. Cart Service
resource "aws_lambda_function" "cart_service" {
  function_name    = "cart-service"
  role             = aws_iam_role.lambda_role.arn
  handler          = "com.deva.cartservice.StreamLambdaHandler::handleRequest"
  runtime          = "java21"
  filename         = data.archive_file.placeholder.output_path
  source_code_hash = data.archive_file.placeholder.output_base64sha256
  timeout          = 30
  memory_size      = 1024

  tracing_config {
    mode = "Active"
  }

  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = local.common_env
  }
}

# 4. Order Service
resource "aws_lambda_function" "order_service" {
  function_name    = "order-service"
  role             = aws_iam_role.lambda_role.arn
  handler          = "com.deva.orderservice.StreamLambdaHandler::handleRequest"
  runtime          = "java21"
  filename         = data.archive_file.placeholder.output_path
  source_code_hash = data.archive_file.placeholder.output_base64sha256
  timeout          = 30
  memory_size      = 1024

  tracing_config {
    mode = "Active"
  }

  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = local.common_env
  }
}

# 5. Payment Service
resource "aws_lambda_function" "payment_service" {
  function_name    = "payment-service"
  role             = aws_iam_role.lambda_role.arn
  handler          = "com.deva.paymentservice.StreamLambdaHandler::handleRequest"
  runtime          = "java21"
  filename         = data.archive_file.placeholder.output_path
  source_code_hash = data.archive_file.placeholder.output_base64sha256
  timeout          = 30
  memory_size      = 1024

  tracing_config {
    mode = "Active"
  }

  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = local.common_env
  }
}

# 6. Review Service
resource "aws_lambda_function" "review_service" {
  function_name    = "review-service"
  role             = aws_iam_role.lambda_role.arn
  handler          = "com.deva.reviewservice.StreamLambdaHandler::handleRequest"
  runtime          = "java21"
  filename         = data.archive_file.placeholder.output_path
  source_code_hash = data.archive_file.placeholder.output_base64sha256
  timeout          = 30
  memory_size      = 1024

  tracing_config {
    mode = "Active"
  }

  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = local.common_env
  }
}

# 7. Notification Consumer Lambda
resource "aws_lambda_function" "notification_consumer" {
  function_name    = "notification-consumer-lambda"
  role             = aws_iam_role.lambda_role.arn
  handler          = "com.deva.notification.NotificationSqsHandler::handleRequest"
  runtime          = "java21"
  filename         = data.archive_file.placeholder.output_path
  source_code_hash = data.archive_file.placeholder.output_base64sha256
  timeout          = 30
  memory_size      = 1024

  tracing_config {
    mode = "Active"
  }

  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = local.common_env
  }
}

# 8. Order Payment Consumer Lambda
resource "aws_lambda_function" "order_payment_consumer" {
  function_name    = "order-payment-consumer-lambda"
  role             = aws_iam_role.lambda_role.arn
  handler          = "com.deva.orderservice.PaymentSuccessSqsHandler::handleRequest"
  runtime          = "java21"
  filename         = data.archive_file.placeholder.output_path
  source_code_hash = data.archive_file.placeholder.output_base64sha256
  timeout          = 30
  memory_size      = 1024

  tracing_config {
    mode = "Active"
  }

  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = local.common_env
  }
}

# Event Source Mappings (SQS triggers)
resource "aws_lambda_event_source_mapping" "order_queue_trigger" {
  event_source_arn = var.order_queue_arn
  function_name    = aws_lambda_function.order_payment_consumer.function_name
  batch_size       = 10
}

resource "aws_lambda_event_source_mapping" "notification_queue_trigger" {
  event_source_arn = var.notification_queue_arn
  function_name    = aws_lambda_function.notification_consumer.function_name
  batch_size       = 10
}

# Outputs for API Gateway routing
output "product_lambda_arn" { value = aws_lambda_function.product_service.arn }
output "product_lambda_name" { value = aws_lambda_function.product_service.function_name }

output "inventory_lambda_arn" { value = aws_lambda_function.inventory_service.arn }
output "inventory_lambda_name" { value = aws_lambda_function.inventory_service.function_name }

output "cart_lambda_arn" { value = aws_lambda_function.cart_service.arn }
output "cart_lambda_name" { value = aws_lambda_function.cart_service.function_name }

output "order_lambda_arn" { value = aws_lambda_function.order_service.arn }
output "order_lambda_name" { value = aws_lambda_function.order_service.function_name }

output "payment_lambda_arn" { value = aws_lambda_function.payment_service.arn }
output "payment_lambda_name" { value = aws_lambda_function.payment_service.function_name }

output "review_lambda_arn" { value = aws_lambda_function.review_service.arn }
output "review_lambda_name" { value = aws_lambda_function.review_service.function_name }

