# Development Milestones and Commit Steps

## Goal

Break the project into meaningful, incremental milestones so the Git history shows natural development progress.

Each milestone should:

- produce a visible project improvement
- keep scope small enough for 1 to 3 commits
- avoid mixing unrelated modules
- leave the codebase in a stable state

## Recommended Milestone Strategy

Use three layers:

1. phase
2. milestone
3. commit slice

That means:

- a phase is a broad project stage
- a milestone is a meaningful deliverable inside a phase
- a commit slice is a small unit of work that feels realistic in Git

## Phase 0: Planning and Design

### Milestone 0.1: Project vision and scope

Commit ideas:

- `docs: add project scope and module roadmap`
- `docs: define mvp and deferred features`

### Milestone 0.2: Domain modeling

Commit ideas:

- `docs: define core entities and role model`
- `docs: add subject vs course offering design decision`
- `docs: add er model and first schema direction`

## Phase 1: Foundation

### Milestone 1.1: Spring Boot bootstrap

Commit ideas:

- `build: initialize spring boot project with maven`
- `build: add core dependencies for web jpa security flyway and openapi`

### Milestone 1.2: Shared app infrastructure

Commit ideas:

- `feat: add base application config and startup class`
- `feat: add common api response and global exception handling`
- `feat: add health endpoint and openapi config`

### Milestone 1.3: Security foundation

Commit ideas:

- `feat: add baseline security configuration`
- `chore: allow health and swagger endpoints in security config`

### Milestone 1.4: Database foundation

Commit ideas:

- `feat: configure datasource and flyway`
- `feat: add initial foundation schema migration`

### Milestone 1.5: Local environment setup

Commit ideas:

- `chore: add gitignore for ide build and env files`
- `chore: move local credentials to env-based configuration`

## Phase 2: Auth and RBAC

### Milestone 2.1: User domain

Commit ideas:

- `feat: add user role and account status model`
- `feat: add user student and teacher entities`

### Milestone 2.2: Auth persistence

Commit ideas:

- `feat: add user and refresh token repositories`
- `feat: add auth dto and validation models`

### Milestone 2.3: JWT infrastructure

Commit ideas:

- `feat: add jwt token provider and auth utilities`
- `feat: add jwt authentication filter`

### Milestone 2.4: Auth APIs

Commit ideas:

- `feat: add login and token refresh endpoints`
- `feat: add current user profile endpoint`

### Milestone 2.5: Access control

Commit ideas:

- `feat: enforce role-based authorization rules`
- `test: add auth integration coverage for protected endpoints`

## Phase 3: Academic Master Data

### Milestone 3.1: Department and program management

Commit ideas:

- `feat: add department and program entities`
- `feat: add department and program management apis`

### Milestone 3.2: Academic term and section management

Commit ideas:

- `feat: add academic term and section entities`
- `feat: add term and section administration endpoints`

### Milestone 3.3: Subject catalog

Commit ideas:

- `feat: add subject and prerequisite models`
- `feat: add subject catalog management apis`

### Milestone 3.4: Student and teacher profile management

Commit ideas:

- `feat: add student profile management service`
- `feat: add teacher profile management service`
- `feat: add filtering pagination for master data listings`

## Phase 4: Course Offering

### Milestone 4.1: Course offering model

Commit ideas:

- `feat: add course offering entity and status rules`
- `feat: add course offering repository and service`

### Milestone 4.2: Course offering APIs

Commit ideas:

- `feat: add admin apis for managing course offerings`
- `feat: add teacher view for assigned offerings`

## Phase 5: Enrollment

### Milestone 5.1: Enrollment persistence

Commit ideas:

- `feat: add enrollment entity and repository`
- `feat: add enrollment status and request models`

### Milestone 5.2: Enrollment validation logic

Commit ideas:

- `feat: validate duplicate enrollment and capacity checks`
- `feat: validate enrollment window and prerequisites`

### Milestone 5.3: Enrollment workflows

Commit ideas:

- `feat: add enrollment and drop endpoints`
- `feat: add student enrollment history endpoint`
- `test: add enrollment workflow integration tests`

## Phase 6: Attendance

### Milestone 6.1: Attendance model

Commit ideas:

- `feat: add attendance session and attendance record entities`
- `feat: add attendance status model`

### Milestone 6.2: Teacher attendance flow

Commit ideas:

- `feat: add attendance marking service for teachers`
- `feat: add attendance marking and update endpoints`

### Milestone 6.3: Attendance reporting

Commit ideas:

- `feat: add attendance percentage calculations`
- `feat: add low attendance flag and reporting apis`

## Phase 7: Exams and Grading

### Milestone 7.1: Exam and mark model

Commit ideas:

- `feat: add exam and mark entry entities`
- `feat: add grade rule model`

### Milestone 7.2: Marks workflow

Commit ideas:

- `feat: add teacher mark entry workflow`
- `feat: restrict mark entry to assigned teachers`

### Milestone 7.3: Result publishing

Commit ideas:

- `feat: add result publication flow`
- `feat: add gpa and cgpa calculation service`
- `feat: add student result history endpoint`

## Phase 8: Fees

### Milestone 8.1: Fee structure foundation

Commit ideas:

- `feat: add fee structure and student fee models`
- `feat: add payment entity and repositories`

### Milestone 8.2: Payment workflow

Commit ideas:

- `feat: add payment recording service`
- `feat: support partial and full fee payments`

### Milestone 8.3: Fee policy

Commit ideas:

- `feat: add overdue and fine calculation logic`
- `feat: add fee due and payment history endpoints`
- `feat: add fee-based eligibility blocking`

## Phase 9: Notifications

### Milestone 9.1: Notification model

Commit ideas:

- `feat: add notification entity and repository`
- `feat: add notification type model`

### Milestone 9.2: Notification delivery

Commit ideas:

- `feat: add in-app notification service`
- `feat: add user notification listing and read endpoints`

### Milestone 9.3: Scheduled jobs

Commit ideas:

- `feat: add scheduled job for fee reminders`
- `feat: add scheduled job for attendance shortage alerts`

## Phase 10: Reporting and Audit

### Milestone 10.1: Audit logging

Commit ideas:

- `feat: add audit log model and persistence`
- `feat: track key admin and academic actions`

### Milestone 10.2: Reporting APIs

Commit ideas:

- `feat: add attendance shortage and fee defaulter reports`
- `feat: add student count and teacher workload reports`

## Phase 11: Chat

### Milestone 11.1: Chat data model

Commit ideas:

- `feat: add chat group membership and message entities`
- `feat: add chat moderation and pinned message support`

### Milestone 11.2: WebSocket foundation

Commit ideas:

- `feat: add websocket config for group chat`
- `feat: add jwt-based socket authentication`

### Milestone 11.3: Chat workflows

Commit ideas:

- `feat: add send message and history endpoints`
- `feat: add unread count and archive group support`

## Phase 12: AI Features

### Milestone 12.1: AI integration foundation

Commit ideas:

- `feat: add ai service abstraction and prompt templates`
- `feat: add safe backend data access layer for ai use cases`

### Milestone 12.2: AI workflows

Commit ideas:

- `feat: add exam eligibility assistant flow`
- `feat: add chat summarization workflow`
- `feat: add student performance insight generation`

## Phase 13: Production Readiness

### Milestone 13.1: Packaging and deployment

Commit ideas:

- `chore: add docker and docker compose setup`
- `docs: add local setup and deployment instructions`

### Milestone 13.2: Quality gates

Commit ideas:

- `test: expand integration coverage for core workflows`
- `ci: add github actions build pipeline`

### Milestone 13.3: Operational hardening

Commit ideas:

- `chore: improve logging and actuator configuration`
- `perf: add caching for high-read endpoints`

## Recommended Commit Rhythm

For normal progress, use:

- 1 commit for config or setup
- 1 commit for entity and repository layer
- 1 commit for service plus controller layer
- 1 commit for tests if added separately

That gives a natural history without making each commit too large.

## Good Commit Pattern Per Feature

For most modules, follow this order:

1. schema and entity model
2. repository and DTOs
3. service logic
4. controller endpoints
5. tests and cleanup

Example for enrollment:

1. `feat: add enrollment schema and entity model`
2. `feat: add enrollment validation and service workflow`
3. `feat: expose enrollment management apis`
4. `test: cover enrollment validations and workflows`

## Suggested Rule to Keep History Natural

Avoid these in a single commit:

- auth + enrollment together
- fees + notifications together
- schema + 6 modules together
- refactor + new feature + tests all mixed together

Instead, keep each commit answerable with one sentence:

- what was added
- why it matters

## Best Practical Starting Sequence From Current State

Since the project already has planning docs and a Phase 1 scaffold, the next natural commit sequence is:

1. `docs: add phase 0 planning and implementation spec`
2. `build: initialize spring boot foundation for sams backend`
3. `feat: add common infrastructure health endpoint and security baseline`
4. `feat: add initial flyway schema for core academic foundation`
5. `chore: move datasource configuration to env-based setup`

That will already make the repo feel like it grew step by step.
