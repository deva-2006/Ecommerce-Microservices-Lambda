terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

module "dynamodb" {
  source       = "./modules/dynamodb"
  project_name = var.project_name
  environment  = var.environment
}

module "cognito" {
  source       = "./modules/cognito"
  project_name = var.project_name
  environment  = var.environment
}

module "messaging" {
  source       = "./modules/messaging"
  project_name = var.project_name
  environment  = var.environment
}

module "frontend" {
  source                     = "./modules/frontend"
  project_name               = var.project_name
  environment                = var.environment
  product_images_bucket_name = var.product_images_bucket_name
}

module "lambda" {
  source                     = "./modules/lambda"
  project_name               = var.project_name
  environment                = var.environment
  aws_region                 = var.aws_region
  from_email                 = var.from_email
  cognito_user_pool_id       = module.cognito.user_pool_id
  cognito_client_id          = module.cognito.user_pool_client_id
  sns_topic_arn              = module.messaging.sns_topic_arn
  order_queue_arn            = module.messaging.order_queue_arn
  notification_queue_arn      = module.messaging.notification_queue_arn
  products_table_name        = module.dynamodb.products_table_name
  inventory_table_name       = module.dynamodb.inventory_table_name
  cart_table_name            = module.dynamodb.cart_table_name
  orders_table_name          = module.dynamodb.orders_table_name
  payments_table_name        = module.dynamodb.payments_table_name
  reviews_table_name         = module.dynamodb.reviews_table_name
  product_images_bucket_name = module.frontend.product_images_bucket_name
  product_images_bucket_arn  = module.frontend.product_images_bucket_arn
}

module "apigateway" {
  source                    = "./modules/apigateway"
  project_name              = var.project_name
  environment               = var.environment
  aws_region                = var.aws_region
  cognito_user_pool_id      = module.cognito.user_pool_id
  cognito_client_id         = module.cognito.user_pool_client_id
  product_lambda_arn        = module.lambda.product_lambda_arn
  product_lambda_name       = module.lambda.product_lambda_name
  inventory_lambda_arn      = module.lambda.inventory_lambda_arn
  inventory_lambda_name     = module.lambda.inventory_lambda_name
  cart_lambda_arn           = module.lambda.cart_lambda_arn
  cart_lambda_name          = module.lambda.cart_lambda_name
  order_lambda_arn          = module.lambda.order_lambda_arn
  order_lambda_name         = module.lambda.order_lambda_name
  payment_lambda_arn        = module.lambda.payment_lambda_arn
  payment_lambda_name       = module.lambda.payment_lambda_name
  review_lambda_arn         = module.lambda.review_lambda_arn
  review_lambda_name        = module.lambda.review_lambda_name
}



module "cloudwatch" {
  source                   = "./modules/cloudwatch"
  project_name             = var.project_name
  environment              = var.environment
  aws_region               = var.aws_region
  api_id                   = module.apigateway.api_id
  sns_topic_name           = "payment-events"
  order_queue_name         = "order-payment-consumer-queue"
  notification_queue_name  = "notification-consumer-queue"
}
