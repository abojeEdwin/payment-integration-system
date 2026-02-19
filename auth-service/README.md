# Auth Service

Merchant authentication and API key management for the Payment Integration System.

## Features

- ✅ Merchant registration and authentication
- ✅ JWT-based admin authentication
- ✅ API key generation and management
- ✅ Secure password hashing (BCrypt)
- ✅ API key validation endpoint (for payment-service)
- ✅ Role-based access control
- ✅ Audit logging for all key operations

## Architecture
┌─────────────────┐
│ Merchant │
│ (Admin UI) │
└────────┬────────┘
│
▼
┌─────────────────────────────┐
│ Auth Service │
│ - /auth/register │
│ - /auth/login (JWT) │
│ - /api-keys (CRUD) │
└────────┬────────────────────┘
│
▼
┌─────────────────────────────┐
│ PostgreSQL │
│ - merchants │
│ - api_keys │
└─────────────────────────────┘



## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register new merchant |
| POST | `/auth/login` | Login and get JWT token |
| GET | `/auth/me` | Get current merchant details |
| PUT | `/auth/me` | Update merchant details |

### API Keys

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api-keys` | Generate new API key |
| GET | `/api-keys` | List all API keys |
| DELETE | `/api-keys/{id}` | Revoke API key by ID |
| DELETE | `/api-keys/prefix/{prefix}` | Revoke API key by prefix |

## Configuration

### Environment Variables

```bash
JWT_SECRET=your-secret-key
DB_HOST=localhost
DB_PORT=5432
DB_NAME=auth_db
DB_USERNAME=auth_user
DB_PASSWORD=your_password
```

Security
- Passwords are hashed using BCrypt
- API keys are 64 characters long (cryptographically secure)
- JWT tokens expire after 24 hours
- All endpoints are protected by Spring Security
- API key validation uses secure comparison

Development
# Run with H2 database (in-memory)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

# Run tests
```bash
mvn test
```

# Build Docker image
```bash
docker build -t auth-service .
```

# Run with Docker
```bash
docker run -p 8081:8081 \
  -e JWT_SECRET=secret \
  -e DB_HOST=host.docker.internal \
  auth-service
```




## Dependencies
- Spring Boot 3.3+
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT (io.jsonwebtoken)
- MapStruct
- Lombok

## License
**MIT

## 📋 Summary

This Auth Service module provides:

✅ **Complete merchant lifecycle** (register, login, update, deactivate)  
✅ **API key management** (generate, list, revoke) with security best practices  
✅ **JWT-based authentication** for admin portal  
✅ **Layered architecture** (controller → service → repository)  
✅ **MapStruct** for clean entity-DTO mapping  
✅ **Global exception handling** with RFC 7807 compliance  
✅ **TDD-ready** with unit tests  
✅ **Docker support** with multi-stage build  
✅ **Profile-based configuration** (dev, prod)  
✅ **Security hardening** (BCrypt, secure random keys, audit logging)