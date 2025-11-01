# Backend Data Governance Service — Report

**Technologies**: Spring Boot, MongoDB, JUnit, Docker, Docker Compose  
**Author**: Prethish Kumar S  
**Date**: 2/11/2025  
**Github**: https://github.com/Prethish-Kumar/Backend-Data-Governance-Service  
**Postman Testing**: https://github.com/Prethish-Kumar/Backend-Data-Governance-Service/blob/main/Data%20Governance%20Service.postman_collection.json  

---

## 1. Introduction

In this project, I designed and implemented a Data Governance Service that manages User Profiles, User Preferences, and User Posts.

The goal was to build a production-ready backend service capable of handling CRUD operations, enforcing data integrity, and maintaining auditability through soft and hard deletion mechanisms with cascading effects across entities.

### Technology Stack:

**Spring Boot (Java)** — Application framework for RESTful API design.

**MongoDB** — NoSQL database for flexible document persistence.

**JUnit + Mockito** — Unit testing and mocking frameworks.

**Docker & Docker Compose** — Containerization and local orchestration.

---

### 2. Requirements Analysis

The system requirements defined in FR1–FR10 cover user, preference, and post management with specific business rules for data governance.

### Functional Overview

- **User Profiles**: Create, read, update, soft delete, and hard delete operations with a 24-hour grace period before permanent removal.

- **Preferences**: Create, update, and retrieve user-specific settings.

- **Posts**: Create, read, and soft delete posts associated with a user.

- **Governance**: Enforce soft deletion filters, cascading deletions, and validation to maintain consistency across entities.

### Core Business Rules

- Soft-deleted users and posts are excluded from read operations.
- Hard deletion is allowed only after the grace period expires.
- User deletion triggers cascading deletions of related posts and preferences.

- Posts cannot be created by or for deleted users.

- All critical operations are logged in an internal audit trail.

- These requirements guided the architectural design and implementation priorities.

## 3. High-Level Design (HLD)

The system follows a three-layered architecture ensuring modularity and maintainability.

### [Controller Layer] → [Service Layer] → [Repository Layer] → [MongoDB]

### Component Description :

- **Controller Layer**: Handles HTTP requests and response mapping.
- **Service Layer**: Encapsulates business logic, validation, and cascading deletion behavior.
- **Repository Layer**: Provides database abstraction via Spring Data MongoDB.

### Data Flow Overview :

- **When a user is soft-deleted**, associated posts and preferences are marked as deleted through a cascade.
- **Hard deletion** of the user and related entities may be performed only after a configurable grace period (default: 24 hours)
- **All operations update timestamps and append entries to the audit trail for traceability.**

This layered approach promotes separation of concerns and simplifies testing and maintenance.

## 4. Low-Level Design (LLD)

### Entity Models Used:

**UserProfile**

```js
{
  "id": "U123",
  "name": "Alice",
  "email": "alice@example.com",
  "roles": ["USER"],
  "deleted": false,
  "deletedAt": null,
  "lastModified": "2025-11-01T12:00:00Z",
  "auditTrail": []
}
```

**UserPreferences**

```js
{
  "userId": "U123",
  "notificationsEnabled": true,
  "theme": "dark"
}
```

**Post**

```js
{
"id": "P987",
  "userId": "U123",
  "content": "Hello world!",
  "deleted": false,
  "deletedAt": null
}
```

### Business Logic

- **Soft Delete:** Marks the entity as deleted and records a deletion timestamp.

- **Hard Delete:** Permanently removes entities after the grace period.

- **Cascade Deletion:** Deletion of a user cascades to all related posts and preferences.

- **Validation:** Prevents creation of posts or preferences for deleted users.

- **Auditing:** Logs every lifecycle event with timestamps for transparency.

### Service Responsibilities

- **UserService:** Handles user lifecycle operations and cascading deletions.

- **PostService:** Validates user state before post creation and manages post deletions.

- **PreferenceService:** Maintains preference integrity and validation checks.

## 5. Implementation Details

The service is implemented using Spring Boot 3.5.7 and Spring Data MongoDB.
Business logic is encapsulated within service classes, and each critical operation is unit tested using JUnit 5 and Mockito.

### Key Implementation Points

- **@Transactional** ensures atomic operations during cascading deletions.

- **Custom exceptions** (NotFoundException, ForbiddenException, ConflictException) provide precise error handling.

- **Validation annotations** enforce input correctness at the API level.

### Example Snippet (Soft Delete Implementation)

```js
    @Transactional
    public void softDeleteUser(String id) {
        UserProfile user = getUser(id);
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());

        postRepo.findByUserIdAndDeletedFalse(id).forEach(post -> {
            post.setDeleted(true);
            post.setDeletedAt(Instant.now());
            postRepo.save(post);
        });

        prefRepo.findByUserId(id).ifPresent(pref -> {
            pref.setDeleted(true);
            pref.setDeletedAt(Instant.now());
            prefRepo.save(pref);
        });

        addAudit(user, "SOFT_DELETE", "User soft-deleted");
        repo.save(user);
    }
```

## 6. Testing Strategy

A total of **33 unit tests** were created to validate all major business rules, including grace period checks, cascading deletions, and validation logic.

### Test Scenarios

- Soft delete idempotency (multiple deletions handled gracefully).

- Hard delete restricted until grace period expiration.

- Creation of posts and preferences blocked for deleted users.

- Cascading deletions validated across users, posts, and preferences.

- Repository layer mocked to isolate business logic.

**Result: All unit tests executed successfully with expected outputs.**

## 7. Docker Setup

The service is fully containerized for consistent local and production deployment.

### Configuration

**Dockerfile:** Builds the Spring Boot JAR and runs it with OpenJDK 17.
**docker-compose.yml:** Launches both the service and a MongoDB instance.

### To Run The Container :

```
docker compose build
docker compose up
```

### Environment Variables

**MONGO_URI** and **Grace Period** duration are configurable.

Application exposed on port **8080**, MongoDB on **27017**.

## 8. Code Quality and Best Practices

**The project adheres to modern backend engineering standards:**

- Clear separation of concerns across layers.

- Centralized exception handling and error responses.

- Externalized configuration for environment-dependent variables.

- Unit testing for core business logic.

- Consistent logging and audit tracking for traceability.

- Code formatted and organized for readability and extensibility.

## 9. Conclusion

- The Data Governance Service successfully implements all required functionality defined in the PRD.
- It enforces data integrity, cascading deletions, and auditability while maintaining clean architecture and code quality standards.
- The service is fully containerized, tested, and deployable with minimal setup effort.
