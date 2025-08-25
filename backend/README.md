# Customer Management App with File Upload

A full-stack customer management application that demonstrates modern web development practices. Users can manage customer records and upload profile pictures through a clean, responsive interface.

## What It Does

**Customer Management**: Complete CRUD operations for customer records including personal details and contact information.

**File Upload**: Drag-and-drop interface for uploading customer profile pictures with proper validation and error handling.

**Flexible Storage**: Automatically uses AWS S3 in production or local file storage during development. No AWS setup required for local testing.

## Technical Stack

### Backend
- **Java 17** - Modern Java features and performance
- **Spring Boot** - Java framework with embedded server, data access, and security
- **PostgreSQL** - Relational database for data persistence
- **AWS S3 SDK** - Cloud storage integration
- **Maven** - Build and dependency management

### Frontend
- **React** - Component-based user interface library
- **Chakra UI** - Modern, accessible component framework

### Testing & Tools
- **JUnit 5** - Unit testing framework
- **Testcontainers** - Integration testing with containerized databases
- **Docker** - Containerized deployment
- **Flyway** - Database migrations

## Key Features

The application uses a layered architecture separating presentation, business logic, and data access concerns. The storage system implements a strategy pattern allowing seamless switching between AWS S3 and local file storage without code changes.

Security is handled through Spring Security with stateless JWT authentication. The frontend follows React best practices with functional components and proper state management.

The system includes comprehensive testing with both unit tests and integration tests using real database containers, ensuring reliability across different environments.

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
