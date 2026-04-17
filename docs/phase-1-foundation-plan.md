# Phase 1: Project Foundation

## Goal

Create a clean Spring Boot backend foundation that boots successfully, connects to PostgreSQL, supports Flyway migrations, and provides the shared infrastructure needed by later phases.

## Phase 1 Outcomes

At the end of Phase 1, the project should provide:

- Maven-based Spring Boot project
- Java package skeleton
- PostgreSQL configuration via `application.yml`
- Flyway migration support
- base audit entity
- common API response model
- global exception handling
- health endpoint
- OpenAPI/Swagger support
- placeholder security configuration that is easy to harden in Phase 2

## Task Breakdown

### 1. Project Bootstrap

- create `pom.xml`
- choose Java version
- add Spring Boot dependencies
- define project coordinates and artifact name

### 2. Core Configuration

- create `application.yml`
- define datasource placeholders
- configure JPA defaults
- configure Flyway
- configure OpenAPI metadata

### 3. Shared Infrastructure

- create main application class
- create `BaseEntity` with audit fields
- create shared API response wrapper
- create global exception handler
- create health check endpoint

### 4. Security Foundation

- add Spring Security dependency
- create permissive baseline `SecurityConfig`
- allow health and Swagger endpoints
- leave Phase 2-ready extension points for JWT

### 5. Database-First Preparation

- create first Flyway migration folder
- add initial schema for foundational tables
- include constraints for core identities and academic structure

### 6. Package Skeleton

- create feature-first package layout for upcoming modules

## First Migration Wave

These tables should exist in Phase 1 because Phase 2 and Phase 3 depend on them:

- `users`
- `refresh_tokens`
- `departments`
- `programs`
- `academic_terms`
- `sections`
- `subjects`
- `subject_prerequisites`
- `teachers`
- `students`
- `course_offerings`
- `enrollments`

## Suggested Verification

Once the scaffold is in place, verify:

1. `mvn spring-boot:run` starts successfully
2. Flyway picks up the first migration
3. `/api/v1/health` responds
4. Swagger UI is reachable

## What Phase 1 Does Not Yet Do

- JWT authentication
- login APIs
- role-based endpoint protection
- business workflows
- enrollment logic
- attendance and grading logic

Those begin in Phase 2 and later.
