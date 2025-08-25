# Customer Management App with File Upload

A full-stack customer management application that demonstrates modern web development practices. Users can manage customer records and upload profile pictures through a clean, responsive interface.

## What It Does

**Customer Management**: Complete CRUD operations for customer records including personal details and contact information.

**File Upload**: Drag-and-drop interface for uploading customer profile pictures with proper validation and error handling.

**Flexible Storage**: Automatically uses AWS S3 in production or local file storage during development. No AWS setup required for local testing.

**Responsive Design**: Works seamlessly across desktop and mobile devices with an intuitive user interface.

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
- Docker containerization


## Getting Started

### Prerequisites
- Java 17+
- Node.js 16+
- Docker
- PostgreSQL

### Running the Application

1. **Backend (Spring Boot)**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

2. **Frontend (React)**
   ```bash
   cd frontend/react
   npm install
   npm run dev
   ```

3. **Frontend (Angular)**
   ```bash
   cd frontend/angular
   npm install
   ng serve
   ```

## Project Structure

- `backend/` - Spring Boot REST API
- `frontend/react/` - React frontend application
- `frontend/angular/` - Angular frontend application

## Features

- User authentication and authorization
- Customer management
- File upload functionality
- Responsive UI design
- RESTful API design
- Database integration with PostgreSQL
- Cloud deployment ready
