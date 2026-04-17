# Phase 0: Planning and Design

## Goal

Lock the scope, core domain model, and architectural direction before writing Spring Boot code.

This phase exists to prevent expensive redesign in later modules like enrollment, attendance, grading, and fees.

## Exit Criteria

Phase 0 is complete only when all of these are decided:

- project scope is fixed for MVP
- roles and permissions are defined
- core entities and relationships are finalized
- naming conventions are agreed
- module boundaries are clear
- API style is chosen
- package structure is chosen
- database backbone is understood
- optional features are separated from MVP

## Required Decisions

### 1. Product Scope

Project name:

- `Student Academic Management System`

Product boundary:

- single institution
- backend-first
- three core roles
- term-based academic workflow

In scope for MVP:

- auth and RBAC
- academic master data
- course offerings
- enrollment
- attendance
- exams and grading
- fee tracking
- notifications

Out of scope for MVP:

- real-time chat
- AI features
- advanced analytics dashboards
- queue-based async architecture
- external payment integration

### 2. Role Definitions

#### ADMIN

- manage users
- create students and teachers
- manage academic master data
- configure offerings
- manage fee and grade rules
- view reports
- moderate system operations

#### TEACHER

- view assigned offerings
- mark attendance
- enter marks
- publish announcements

#### STUDENT

- view own profile
- enroll in allowed offerings
- view attendance
- view results
- view fees and notifications

### 3. Core Domain Decisions

#### 3.1 Subject vs Course Offering

This must be locked before schema design.

- `Subject` = catalog definition
- `CourseOffering` = term-specific delivery of a subject

`CourseOffering` owns:

- teacher assignment
- section assignment
- term assignment
- enrollment window
- capacity
- attendance sessions
- exams

#### 3.2 User State

User account status:

- `ACTIVE`
- `SUSPENDED`
- `LOCKED`
- `DISABLED`

Student academic lifecycle:

- `ACTIVE`
- `ON_HOLD`
- `GRADUATED`
- `DROPPED`

#### 3.3 Eligibility Policy

Use one centralized policy layer for:

- enrollment eligibility
- exam eligibility
- fee blocking
- attendance shortage alerts

### 4. Academic Backbone

These entities must exist in the model before workflows are built:

- `Department`
- `Program`
- `AcademicTerm`
- `Section`
- `Subject`
- `SubjectPrerequisite`
- `Teacher`
- `Student`
- `CourseOffering`

### 5. API Direction

Use resource-oriented REST naming.

Examples:

- `POST /api/v1/auth/login`
- `GET /api/v1/users/me`
- `POST /api/v1/course-offerings`
- `POST /api/v1/enrollments`

Avoid:

- `createStudent`
- `getAllSubjectsBySemester`

### 6. Package Structure

Use feature-first packages:

```text
com.example.sams
  config
  common
  security
  auth
  user
  academic
  offering
  enrollment
  attendance
  grading
  fee
  notification
  chat
  ai
  audit
```

## Phase 0 Deliverables

At the end of this phase, the project should have:

1. implementation spec
2. ER model
3. module list
4. role and permission map
5. MVP feature list
6. initial build order

Existing deliverable:

- [student-academic-management-spec.md](C:/Users/Niranjan/Desktop/Student_mngmnt/docs/student-academic-management-spec.md)

## Phase 0 Task Checklist

### A. Scope Lock

- finalize project name
- finalize MVP features
- move chat and AI to post-MVP
- confirm single-institution scope

### B. Role and Access Lock

- confirm `ADMIN`, `TEACHER`, `STUDENT`
- define which role creates which records
- define self-service vs admin-managed actions
- define account status behavior

### C. Domain Model Lock

- finalize core entities
- finalize relationships
- finalize unique constraints
- finalize lifecycle/status enums
- finalize offering-centered workflow design

### D. API and Package Lock

- choose `/api/v1`
- choose plural resource naming
- choose feature-first package structure
- choose response and exception strategy

### E. Build Order Lock

- decide what belongs in Phase 1
- decide what is blocked on schema design
- decide which tables must exist in first migration wave

## Recommended Output From Phase 0

If we were presenting this phase as complete, the official output would be:

### Finalized MVP

- auth
- user and profile management
- academic master data
- course offerings
- enrollment
- attendance
- grading
- fees
- notifications

### Deferred Features

- chat
- analytics dashboards
- AI assistant
- caching and advanced infra

### First Schema Wave

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

## Risks Prevented By Phase 0

This phase helps us avoid:

- building enrollment on the wrong entity
- duplicating rule checks across modules
- mixing catalog data with transactional data
- unclear teacher ownership of attendance and marks
- inconsistent naming and package sprawl
- overbuilding chat and AI before core workflows work

## Ready-to-Start Condition For Phase 1

We can start Phase 1 when:

- the spec is accepted
- the entity list is stable
- the first migration tables are agreed
- the package structure is agreed
- the MVP cut is agreed

At that point, Phase 1 becomes implementation only, not architecture discovery.
