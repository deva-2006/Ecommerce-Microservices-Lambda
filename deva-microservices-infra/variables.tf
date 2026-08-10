variable "aws_region" {
  type        = string
  description = "The AWS region to deploy resources into"
  default     = "us-east-1"
}

variable "project_name" {
  type        = string
  description = "Project name prefix for resources"
  default     = "deva-ecommerce"
}

variable "environment" {
  type        = string
  description = "Environment name (e.g. dev, staging, prod)"
  default     = "dev"
}

variable "from_email" {
  type        = string
  description = "Verified SES sender email address for notification consumer"
  default     = "deva.s.professional@gmail.com"
}

variable "product_images_bucket_name" {
  type        = string
  description = "S3 bucket name for product image uploads"
  default     = "my-ecommerce-images-deva2006"
}

