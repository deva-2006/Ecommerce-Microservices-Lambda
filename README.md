# 🛒 Serverless E-Commerce Microservices Backend

A cloud-native serverless e-commerce backend built using **Spring Boot 3**, **Java 21**, **AWS Lambda**, **Amazon API Gateway**, **Amazon DynamoDB**, and **Amazon Cognito**.

The application consists of independent microservices deployed as AWS Lambda functions behind a shared HTTP API Gateway. Authentication is handled using **Amazon Cognito** and **API Gateway JWT Authorizers**, providing a secure, scalable, and production-style backend architecture.

---

## Architecture Diagram

![Architecture Diagram](architecture-diagram.png)

---

# Tech Stack

## Backend

- Java 21
- Spring Boot 3
- Spring Data
- OpenFeign
- REST APIs
- Maven

## Cloud

- AWS Lambda
- Amazon API Gateway (HTTP API)
- Amazon DynamoDB
- Amazon Cognito
- AWS IAM
- Amazon CloudWatch

---

# Microservices

| Service | Responsibility |
|----------|---------------|
| Product | Product Management |
| Inventory | Stock Management |
| Cart | Shopping Cart |
| Order | Order Processing |
| Payment | Payment Processing |

---

# Authentication

Authentication is implemented using **Amazon Cognito**.

### Components

- Amazon Cognito User Pool
- Cognito App Client
- API Gateway JWT Authorizer
- JWT Access Tokens

### Authentication Flow

```
Client
      │
      │ Login
      ▼
Amazon Cognito
      │
      │ Issues JWT
      ▼
Client
      │
      │ Authorization: Bearer <JWT>
      ▼
Amazon API Gateway
      │
      │ JWT Authorizer
      ▼
AWS Lambda Microservices
```

### User Identification

The backend no longer trusts client-provided user IDs.

Previous request:

```json
{
    "userId": "user001",
    "productId": "P101",
    "quantity": 2
}
```

Current request:

```json
{
    "productId": "P101",
    "quantity": 2
}
```

The authenticated user's identity is extracted directly from the validated JWT.

---

# Cloud Deployment

Each microservice is deployed independently as an AWS Lambda function.

| Lambda Function |
|-----------------|
| product-service-lambda |
| inventory-service-lambda |
| cart-service-lambda |
| order-service-lambda |
| payment-service-lambda |

---

# API Gateway

A shared Amazon HTTP API Gateway acts as the entry point.

```
Client
   │
   ▼
Amazon API Gateway
   │
   ├── /products
   ├── /inventory
   ├── /cart
   ├── /orders
   └── /payments
```

Protected endpoints are secured using API Gateway JWT Authorizers backed by Amazon Cognito.

---

# DynamoDB Tables

- Products
- Inventory
- Cart
- Orders
- Payments

---

# Service Communication

The microservices communicate internally using OpenFeign through the shared API Gateway.

```
Cart
 ├──► Product
 └──► Inventory

Order
 ├──► Cart
 ├──► Product
 ├──► Inventory
 └──► Payment

Payment
 └──► Order
```

Protected service-to-service communication forwards the authenticated user's JWT to downstream services.

---

# Features

- Serverless Architecture
- RESTful APIs
- Independent Microservices
- Amazon DynamoDB Integration
- Amazon Cognito Authentication
- JWT Authorization
- API Gateway JWT Authorizers
- OpenFeign Inter-Service Communication
- JWT Propagation Between Services
- API Gateway Routing
- Lambda-based Deployment
- CloudWatch Logging
- IAM-based Permissions

---

# Project Structure

```
product-service/
inventory-service/
cart-service/
order-service/
payment-service/
```

Each service contains:

- Controllers
- Services
- Repositories
- DTOs
- Entities
- Lambda Handler
- DynamoDB Configuration
- Cognito JWT Integration

---

# Deployment Flow

```
Spring Boot Project
        │
        ▼
Build JAR
        │
        ▼
AWS Lambda
        │
        ▼
Amazon API Gateway
        │
        ▼
Amazon Cognito JWT Authorizer
        │
        ▼
Client
```

---

# Testing

The APIs were tested using:

- Amazon Cognito Authentication
- Postman
- AWS Lambda Test Events
- HTTP API Gateway

---

# Future Enhancements

- Amazon SNS & Amazon SQS Event-Driven Architecture
- AWS Step Functions for Order Workflow
- Frontend Deployment using Amazon S3 & CloudFront
- CI/CD using GitHub Actions
- Infrastructure as Code using AWS SAM or Terraform

---

# Author

**Deva**

B.Tech Artificial Intelligence & Data Science

**Java Backend Developer | Spring Boot | AWS Serverless | Amazon Cognito**
