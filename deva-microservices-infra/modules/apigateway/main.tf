variable "project_name" { type = string }
variable "environment"  { type = string }
variable "aws_region"   { type = string }

variable "cognito_user_pool_id" { type = string }
variable "cognito_client_id"    { type = string }

variable "product_lambda_arn"   { type = string }
variable "product_lambda_name"  { type = string }
variable "inventory_lambda_arn" { type = string }
variable "inventory_lambda_name" { type = string }
variable "cart_lambda_arn"      { type = string }
variable "cart_lambda_name"     { type = string }
variable "order_lambda_arn"     { type = string }
variable "order_lambda_name"    { type = string }
variable "payment_lambda_arn"   { type = string }
variable "payment_lambda_name"  { type = string }
variable "review_lambda_arn"    { type = string }
variable "review_lambda_name"   { type = string }

resource "aws_apigatewayv2_api" "http_api" {
  name          = "${var.project_name}-http-api-${var.environment}"
  protocol_type = "HTTP"
  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"]
    allow_headers = ["content-type", "authorization", "x-amz-date", "x-api-key", "x-amz-security-token"]
    max_age       = 300
  }
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.http_api.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_apigatewayv2_authorizer" "cognito" {
  api_id           = aws_apigatewayv2_api.http_api.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "cognito-authorizer"

  jwt_configuration {
    audience = [var.cognito_client_id]
    issuer   = "https://cognito-idp.${var.aws_region}.amazonaws.com/${var.cognito_user_pool_id}"
  }
}

# Generic integration and routing macro
locals {
  services = {
    products  = { arn = var.product_lambda_arn, name = var.product_lambda_name, auth = false }
    inventory = { arn = var.inventory_lambda_arn, name = var.inventory_lambda_name, auth = true }
    cart      = { arn = var.cart_lambda_arn, name = var.cart_lambda_name, auth = true }
    orders    = { arn = var.order_lambda_arn, name = var.order_lambda_name, auth = true }
    payments  = { arn = var.payment_lambda_arn, name = var.payment_lambda_name, auth = true }
    reviews   = { arn = var.review_lambda_arn, name = var.review_lambda_name, auth = false }
  }
}

# API Gateway Integrations
resource "aws_apigatewayv2_integration" "lambda_integration" {
  for_each = local.services

  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = each.value.arn
  payload_format_version = "2.0"
}

# API Gateway Routes
resource "aws_apigatewayv2_route" "routes" {
  for_each = local.services

  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "ANY /${each.key}/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration[each.key].id}"

  authorization_type = each.value.auth ? "JWT" : "NONE"
  authorizer_id      = each.value.auth ? aws_apigatewayv2_authorizer.cognito.id : null
}

# API Gateway root routes (without proxy suffix)
resource "aws_apigatewayv2_route" "root_routes" {
  for_each = local.services

  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "ANY /${each.key}"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration[each.key].id}"

  authorization_type = each.value.auth ? "JWT" : "NONE"
  authorizer_id      = each.value.auth ? aws_apigatewayv2_authorizer.cognito.id : null
}

# Permissions for API Gateway to invoke Lambda functions
resource "aws_lambda_permission" "apigw_lambda" {
  for_each = local.services

  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = each.value.name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.http_api.execution_arn}/*/*"
}

output "api_endpoint" {
  value = aws_apigatewayv2_api.http_api.api_endpoint
}

output "api_id" {
  value = aws_apigatewayv2_api.http_api.id
}
