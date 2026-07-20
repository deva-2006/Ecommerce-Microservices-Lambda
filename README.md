# Serverless E-Commerce Microservices Backend

A cloud-native serverless e-commerce backend built using **Spring Boot 3.2.5**, **Java 21**, **AWS Lambda**, **Amazon API Gateway**, **Amazon DynamoDB**, **Amazon Cognito**, **Amazon SNS/SQS**, **Amazon SES**, and **Amazon S3**.

The application consists of independent microservices deployed as AWS Lambda functions behind a shared HTTP API Gateway. Authentication is handled using **Amazon Cognito** and **API Gateway JWT Authorizers**. Event-driven workflows use **Amazon SNS** and **Amazon SQS** for payment and order processing, with **Amazon SES** for email notifications.

## Live Demo

https://dhvfhexmyhpvv.cloudfront.net/

---

## Architecture Diagram

![Architecture Diagram](architecture.png)

---

## Tech Stack

### Backend

- Java 21
- Spring Boot 3.2.5
- Spring Data
- OpenFeign
- Lombok
- REST APIs
- Maven

### Cloud Services

- AWS Lambda
- Amazon API Gateway (HTTP API)
- Amazon DynamoDB (Enhanced Client)
- Amazon Cognito
- Amazon SNS
- Amazon SQS
- Amazon SES
- Amazon S3 (Presigned URLs)
- Amazon CloudFront
- AWS IAM

### Deployment

- aws-serverless-java-container (Spring Boot 3)
- Maven Shade Plugin (Fat JAR packaging)

---

## Microservices

| Service | Responsibility |
|---------|---------------|
| Product | Product Management, S3 Image Upload |
| Inventory | Stock Management |
| Cart | Shopping Cart |
| Order | Order Processing, Payment Event Publishing |
| Payment | Payment Processing, SNS Event Publishing |
| Notification | Email Notifications via SES |

---

## Authentication

Authentication is implemented using **Amazon Cognito**.

### Components

- Amazon Cognito User Pool
- Cognito App Client
- API Gateway JWT Authorizer
- JWT Access Tokens
- Custom `@AuthUserId` Annotation
- Argument Resolvers

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
      │
      │ Extracts user ID from JWT
      ▼
@AuthUserId Annotation
```

### User Identification

The backend extracts the authenticated user's identity directly from the validated JWT using custom `@AuthUserId` annotation and `AuthUserIdArgumentResolver`.

Request body (no userId required):

```json
{
    "productId": "P101",
    "quantity": 2
}
```

---

## Event-Driven Architecture

### SNS/SQS Payment Events

```
Payment Service (SUCCESS)
         │
         ▼
Amazon SNS (payment-events topic)
         │
         ├──► SQS ──► Order Service (updates order status)
         │
         └──► SQS ──► Notification Lambda (sends SES email)
```

### Services

- **Payment Service**: Publishes payment success events to SNS
- **Order Service**: Consumes payment events via SQS Lambda trigger
- **Notification Lambda**: Consumes payment events, sends confirmation emails via SES

---

## Cloud Deployment

Each microservice is deployed independently as an AWS Lambda function.

| Lambda Function | Type |
|-----------------|------|
| product-service-lambda | HTTP Lambda (Spring Boot) |
| inventory-service-lambda | HTTP Lambda (Spring Boot) |
| cart-service-lambda | HTTP Lambda (Spring Boot) |
| order-service-lambda | HTTP Lambda (Spring Boot) |
| payment-service-lambda | HTTP Lambda (Spring Boot) |
| notification-consumer-lambda | SQS Lambda (SES Email) |

---

## API Gateway

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

## DynamoDB Tables

| Table | Partition Key | Service |
|-------|--------------|---------|
| Products | productId | product-service |
| Inventory | productId | inventory-service |
| Cart | userId (PK), productId (SK) | cart-service |
| Orders | orderId | order-service |
| Payments | paymentId | payment-service |

---

## Service Communication

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

Protected service-to-service communication forwards the authenticated user's JWT to downstream services using `FeignClientConfig` request interceptors.

---

## Features

- Serverless Architecture (AWS Lambda)
- RESTful APIs
- Independent Microservices
- Amazon DynamoDB Integration (Enhanced Client)
- Amazon Cognito Authentication
- JWT Authorization
- API Gateway JWT Authorizers
- Custom @AuthUserId Annotation
- Argument Resolvers for User Extraction
- OpenFeign Inter-Service Communication
- JWT Propagation Between Services
- Event-Driven Architecture (SNS/SQS)
- Email Notifications (SES)
- S3 Presigned URLs for Image Uploads
- CloudFront CDN Distribution
- API Gateway Routing
- Lambda-based Deployment
- Maven Shade Plugin (Fat JAR Packaging)
- aws-serverless-java-container (Spring Boot 3)

---

## Project Structure

```
product-service/
inventory-service/
cart-service/
order-service/
payment-service/
notification-consumer-lambda/
```

Each Spring Boot service contains:

- Controllers
- Services (Interface + Implementation)
- Repositories
- DTOs (Request/Response)
- Entities
- Lambda Handler (StreamLambdaHandler)
- DynamoDB Configuration
- Cognito JWT Integration
- Custom Security Annotations

---

## Deployment Flow

```
Spring Boot Project
        │
        ▼
Maven Build (Shade Plugin)
        │
        ▼
Fat JAR
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

## Testing

The APIs were tested using:

- Amazon Cognito Authentication
- Postman
- AWS Lambda Test Events
- HTTP API Gateway

---

## Author

**Deva**

B.Tech Artificial Intelligence & Data Science

**Java Backend Developer | Spring Boot | AWS Serverless | Amazon Cognito**
