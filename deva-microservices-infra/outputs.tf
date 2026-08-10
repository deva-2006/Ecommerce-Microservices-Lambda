output "api_gateway_url" {
  value       = module.apigateway.api_endpoint
  description = "The HTTP API Gateway endpoint URL"
}

output "cloudfront_domain_name" {
  value       = module.frontend.cloudfront_domain_name
  description = "The CloudFront distribution domain name"
}

output "cognito_user_pool_id" {
  value       = module.cognito.user_pool_id
  description = "Cognito User Pool ID"
}

output "cognito_client_id" {
  value       = module.cognito.user_pool_client_id
  description = "Cognito User Pool Client ID"
}

output "cloudwatch_dashboard_url" {
  value       = module.cloudwatch.dashboard_url
  description = "Direct link to the CloudWatch operations dashboard"
}

output "product_images_bucket_name" {
  value       = module.frontend.product_images_bucket_name
  description = "S3 bucket for product image uploads"
}

