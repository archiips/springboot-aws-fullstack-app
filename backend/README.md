# Architos Backend

Spring Boot REST API for the Architos application.

## Features

- RESTful API endpoints
- JWT authentication
- File upload with S3 integration
- PostgreSQL database integration
- Comprehensive error handling
- Metrics and monitoring
- Docker containerization

## API Endpoints

- `POST /api/v1/auth/login` - User authentication
- `POST /api/v1/customers` - Create customer
- `GET /api/v1/customers` - Get all customers
- `GET /api/v1/customers/{id}` - Get customer by ID
- `PUT /api/v1/customers/{id}` - Update customer
- `DELETE /api/v1/customers/{id}` - Delete customer
- `POST /api/v1/customers/{id}/profile-image` - Upload profile image

## Configuration

Configure the following environment variables:

- `SPRING_DATASOURCE_URL` - Database URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `AWS_S3_BUCKETS_CUSTOMER` - S3 bucket name
