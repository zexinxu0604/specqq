# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Chatbot Router System** - A QQ group chatbot management system with rule-based message routing, built with Spring Boot 3.x and Vue 3. The system receives messages from chat clients (primarily NapCatQQ), routes them through a rule engine, and sends automated replies.

**Current Status**: v1.0.0-SNAPSHOT | MVP Complete | 84.3% Development Progress (75/89 tasks)

## Architecture

### Three-Layer Architecture

```
┌─────────────────────────────────────────────────┐
│  Frontend (Vue 3 + TypeScript + Element Plus)  │
│  - Rule Management UI                           │
│  - Group Chat Configuration                     │
│  - Message Log Viewer                           │
└───────────────┬─────────────────────────────────┘
                │ HTTP/REST API
┌───────────────▼─────────────────────────────────┐
│  Backend (Spring Boot 3 + MyBatis-Plus)        │
│  ┌─────────────────────────────────────────┐   │
│  │ Rule Engine (Priority-based Matching)   │   │
│  │ - Keyword/Regex/Exact/Prefix/Suffix     │   │
│  │ - Rule Cache (Caffeine + Redis)         │   │
│  │ - Rate Limiter                           │   │
│  └─────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────┐   │
│  │ Protocol Adapters (Extensible)          │   │
│  │ - NapCatAdapter (WebSocket + HTTP)      │   │
│  │ - ClientAdapterFactory                   │   │
│  └─────────────────────────────────────────┘   │
└───────────────┬─────────────────────────────────┘
                │ WebSocket/HTTP
┌───────────────▼─────────────────────────────────┐
│  Chat Clients (NapCatQQ, extensible)           │
└─────────────────────────────────────────────────┘
```

### Key Design Patterns

1. **Strategy Pattern**: `RuleMatcher` interface with implementations (`ExactMatcher`, `ContainsMatcher`, `RegexMatcher`)
2. **Factory Pattern**: `ClientAdapterFactory` for protocol adapter creation
3. **Adapter Pattern**: `ClientAdapter` interface for different chat platforms
4. **Chain of Responsibility**: `RuleEngine` processes rules by priority until first match

### Core Components

**Rule Engine** (`src/main/java/com/specqq/chatbot/engine/`):
- `RuleEngine`: Orchestrates rule matching with priority-based scheduling
- `RuleMatcher`: Interface for different matching strategies
- `MessageRouter`: Routes messages to appropriate handlers
- `RateLimiter`: Prevents abuse (5 seconds, max 3 triggers per user)

**Protocol Adapters** (`src/main/java/com/specqq/chatbot/adapter/`):
- `NapCatAdapter`: Handles NapCatQQ protocol (OneBot 11) via WebSocket/HTTP
- `ClientAdapterFactory`: Creates appropriate adapters based on protocol type

**Data Layer** (`src/main/java/com/specqq/chatbot/`):
- Uses MyBatis-Plus 3.5.7 with custom XML mappers
- Entities: `MessageRule`, `GroupChat`, `MessageLog`, `ChatClient`, `AdminUser`
- Mappers in `src/main/resources/mapper/`

## Development Commands

### Backend (Java 17 + Spring Boot 3.1.8)

```bash
# Build and run (recommended - handles Java 17)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Alternative startup scripts
./start-backend.sh     # Uses JAVA_HOME for Java 17
./quick-start.sh       # Quick start script

# Run tests
mvn test                           # All tests
mvn test -Dtest=RuleEngineTest     # Single test class
mvn test -Dtest=RuleEngineTest#testKeywordMatch  # Single test method

# Build JAR
mvn clean package -DskipTests

# Code coverage
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html

# Check dependencies
mvn dependency:tree
mvn versions:display-dependency-updates
```

### Frontend (Vue 3 + TypeScript + Vite)

```bash
# Start development server
cd frontend
npm install
npm run dev              # Starts on http://localhost:5173

# Alternative: use project script
./start-frontend.sh      # From project root

# Build for production
npm run build
npm run preview          # Preview production build

# Type checking and linting
npm run type-check       # TypeScript validation
npm run lint             # ESLint + auto-fix

# Testing
npm run test             # Vitest unit tests
npm run test:e2e         # Playwright E2E tests
```

### Database Setup

```bash
# Start MySQL (Homebrew)
brew services start mysql@8.4

# Create database
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS chatbot_router CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Start Redis
brew services start redis
redis-cli ping  # Should return PONG

# Database migrations are handled by Flyway automatically on startup
# Migration scripts: src/main/resources/db/migration/V*.sql
```

### Docker Deployment

```bash
# Start all services (MySQL + Redis + Backend + Frontend)
docker-compose up -d

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Stop services
docker-compose down
```

## Project Structure

```
specqq/
├── src/main/java/com/specqq/chatbot/
│   ├── controller/          # REST API endpoints (41 APIs)
│   ├── service/             # Business logic layer
│   ├── mapper/              # MyBatis data access interfaces
│   ├── entity/              # Database entities (JPA-annotated)
│   ├── dto/                 # Data transfer objects
│   ├── vo/                  # View objects (API responses)
│   ├── engine/              # Rule engine core (RuleEngine, Matchers)
│   ├── adapter/             # Protocol adapters (NapCatAdapter)
│   ├── websocket/           # WebSocket handlers
│   ├── config/              # Spring configuration (Security, Cache, etc.)
│   ├── security/            # JWT authentication filter
│   ├── common/              # Common utilities (Result, ResultCode)
│   └── exception/           # Global exception handler
├── src/main/resources/
│   ├── mapper/              # MyBatis XML mappers
│   ├── application.yml      # Base configuration
│   ├── application-dev.yml  # Development profile
│   └── db/migration/        # Flyway migration scripts
├── src/test/java/com/specqq/chatbot/
│   ├── unit/                # Unit tests (Mockito)
│   ├── integration/         # Integration tests (TestContainers)
│   └── resources/           # Test resources (JMeter scripts)
├── frontend/
│   ├── src/
│   │   ├── api/             # API client modules (auth, rule, group, log)
│   │   ├── components/      # Reusable Vue components
│   │   ├── layouts/         # Layout components
│   │   ├── router/          # Vue Router configuration
│   │   ├── stores/          # Pinia state management (auth, rules)
│   │   ├── types/           # TypeScript type definitions
│   │   ├── utils/           # Utility functions
│   │   └── views/           # Page components
│   └── package.json
├── specs/001-chatbot-router/  # Feature specifications
│   ├── spec.md              # Functional requirements
│   ├── plan.md              # Implementation plan
│   ├── tasks.md             # Task breakdown
│   └── data-model.md        # Database schema
├── .specify/                # SpecKit configuration
│   ├── templates/           # Document templates
│   └── memory/constitution.md  # Project constitution
├── pom.xml                  # Maven dependencies
└── docker-compose.yml       # Docker orchestration
```

## Configuration

### Environment Profiles

**Development** (`application-dev.yml`):
- Database: `jdbc:mysql://localhost:3306/chatbot_router`
- Redis: `localhost:6379`
- JWT expiration: 24 hours
- Log level: DEBUG for `com.specqq.chatbot`

**Production** (`application-prod.yml`):
- Use environment variables for sensitive data
- JWT secret must be changed
- Log level: INFO

### Key Configuration Files

- `src/main/resources/application.yml`: Base configuration
- `src/main/resources/logback-spring.xml`: Logging configuration
- `frontend/vite.config.ts`: Vite build configuration
- `frontend/tsconfig.json`: TypeScript compiler options

## Testing Strategy

### Test Coverage Requirements

- **Backend**: ≥80% statement coverage (core business logic ≥90%)
- **Frontend**: ≥70% coverage (core components ≥85%)

### Test Types

1. **Unit Tests** (`src/test/java/.../unit/`):
   - Test individual classes/methods with mocked dependencies
   - Use JUnit 5 + Mockito + AssertJ
   - Example: `RuleMatcherTest`, `RuleEngineTest`

2. **Integration Tests** (`src/test/java/.../integration/`):
   - Test component interactions with real dependencies
   - Use `@SpringBootTest` + TestContainers (MySQL)
   - Example: `RuleEngineIntegrationTest`

3. **Frontend Tests** (`frontend/src/tests/`):
   - Component tests: Vitest + Vue Test Utils
   - E2E tests: Playwright

### Running Tests

```bash
# Backend: Run specific test suites
mvn test -Dtest="**/*Test"           # Unit tests
mvn test -Dtest="**/*IntegrationTest" # Integration tests

# Frontend
cd frontend
npm run test              # Vitest unit tests
npm run test:e2e          # Playwright E2E tests
```

## API Endpoints

**Base URL**: `http://localhost:8080/api`

**Authentication** (`/api/auth`):
- POST `/login` - User login (returns JWT token)
- POST `/logout` - User logout (invalidates token)
- GET `/user-info` - Get current user info
- POST `/refresh` - Refresh JWT token
- POST `/init-admin` - Initialize admin user (first-time setup)

**Rules** (`/api/rules`):
- GET `/` - List rules (paginated, filterable)
- POST `/` - Create new rule
- GET `/{id}` - Get rule details
- PUT `/{id}` - Update rule
- DELETE `/{id}` - Delete rule
- PUT `/{id}/toggle` - Enable/disable rule
- POST `/test` - Test rule matching

**Groups** (`/api/groups`):
- GET `/` - List group chats
- GET `/{id}` - Get group details
- PUT `/{id}/toggle` - Enable/disable group
- GET `/{id}/rules` - Get group's rules
- POST `/{id}/rules` - Bind rule to group
- DELETE `/{id}/rules/{ruleId}` - Unbind rule

**Logs** (`/api/logs`):
- GET `/` - List message logs (paginated, filterable)
- GET `/{id}` - Get log details
- GET `/export` - Export logs as CSV
- GET `/stats` - Get statistics
- GET `/trends` - Get message trends

**API Documentation**: http://localhost:8080/swagger-ui.html

## Security

### Authentication & Authorization

- **JWT-based authentication**: 24-hour expiration, stored in HttpOnly cookies
- **Token blacklist**: Redis-based, invalidates tokens on logout
- **Password encryption**: BCrypt with 12 rounds
- **CORS**: Configured in `SecurityConfig`, adjust for production

### Security Best Practices

1. **Input Validation**: Use `@Valid` on all DTOs
2. **SQL Injection Prevention**: MyBatis uses `#{}` (parameterized queries)
3. **XSS Prevention**: Vue 3 auto-escapes by default, sanitize v-html content
4. **Sensitive Data**: Never log passwords, tokens, or PII in plain text
5. **Rate Limiting**: Built-in rate limiter (5s window, 3 triggers/user)

## Important Constraints

### From Project Constitution (`.specify/memory/constitution.md`)

1. **Java 17 Required**: Use modern features (records, sealed classes, pattern matching)
2. **Spring Boot 3.x Only**: Jakarta EE 9+ (not javax.*)
3. **MyBatis-Plus 3.5+**: Primary ORM, avoid Spring Data JPA
4. **Test-Driven Development**: Write tests first, get approval, then implement
5. **Code Quality**: ≥80% coverage, no Critical/Blocker issues in SonarQube
6. **Performance**: API P95 <200ms, P99 <500ms; Frontend first load <2s

### Prohibited Technologies

- Spring Boot 2.x or earlier
- JDK 11 or lower
- Spring Data JPA + Hibernate (use MyBatis-Plus)
- PostgreSQL (use MySQL 8.0+)
- Vue 2.x (use Vue 3 Composition API)
- Vuex (use Pinia)

## NapCat Integration

### Message Flow

1. **Receive**: NapCatQQ sends group messages via WebSocket/HTTP (OneBot 11 protocol)
2. **Parse**: `NapCatAdapter` extracts message content, sender, group info
3. **Route**: `MessageRouter` passes to `RuleEngine`
4. **Match**: `RuleEngine` finds matching rule by priority
5. **Reply**: `NapCatAdapter` sends reply back to QQ group
6. **Log**: `MessageLogService` records transaction asynchronously

### Message Format

NapCat uses OneBot 11 protocol. Key fields:
- `message_type`: "group" (group messages only for MVP)
- `group_id`: QQ group identifier
- `user_id`: Sender's QQ number
- `message`: Message content (text/CQ codes)
- `raw_message`: Plain text message

See `NAPCAT_MESSAGE_FORMAT.md` for detailed format specification.

## Common Issues

### Compilation Errors

If you encounter compilation errors:
```bash
# Verify Java 17 is active
java -version  # Should show "17.x.x"

# Clean and rebuild
mvn clean install -DskipTests

# If Lombok issues persist
./diagnose-lombok-idea.sh
```

### Port Conflicts

```bash
# Check port usage
lsof -i :8080  # Backend
lsof -i :5173  # Frontend

# Kill process
kill -9 <PID>
```

### Database Connection Issues

```bash
# Verify MySQL is running
brew services list | grep mysql
mysql -u root -p -e "SELECT 1"

# Check Redis
redis-cli ping  # Should return PONG
```

## Documentation

- **README.md**: Quick start guide and project overview
- **DEPLOYMENT_GUIDE.md**: Comprehensive deployment instructions
- **specs/001-chatbot-router/**: Feature specifications and design docs
  - `spec.md`: User stories and requirements
  - `plan.md`: Implementation plan and architecture
  - `tasks.md`: Task breakdown and progress tracking
  - `data-model.md`: Database schema and relationships
- **README_TESTING.md**: Testing documentation and test plans

## SpecKit Workflow

This project uses SpecKit for feature development workflow:

```bash
# Create feature specification
/specify <feature-description>

# Generate implementation plan
/plan

# Generate task breakdown
/tasks

# Analyze cross-artifact consistency
/analyze

# Execute implementation
/implement
```

SpecKit commands are defined in `.claude/commands/` and use templates from `.specify/templates/`.

## Quick Reference

### Default Credentials

- **Admin User**: `admin` / `admin123` (change in production!)
- **MySQL**: `root` / `root` (local dev)
- **Redis**: No password (local dev)

### Service URLs

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus

### Useful Scripts

- `./start-backend.sh`: Start backend with Java 17
- `./start-frontend.sh`: Start frontend dev server
- `./quick-start.sh`: Quick backend startup
- `./test-api.sh`: Run API integration tests
- `./run-performance-test.sh`: Run JMeter performance tests

## Development Workflow

1. **Before Starting**: Read the feature spec in `specs/001-chatbot-router/spec.md`
2. **Write Tests First**: Follow TDD (Red-Green-Refactor)
3. **Code Standards**: Follow Alibaba Java Coding Guidelines (backend) and Vue 3 Style Guide (frontend)
4. **Commit Messages**: Use Conventional Commits format (`feat:`, `fix:`, `docs:`, etc.)
5. **Code Review**: All changes require PR review before merging to `main`

## Performance Targets

- **Backend API**: P95 <200ms, P99 <500ms
- **Frontend Load**: First contentful paint <2s (4G network)
- **Database**: Query execution <100ms (slow queries logged)
- **Cache Hit Rate**: ≥90% for hot data (rule cache)
- **Concurrent Users**: Support ≥100 simultaneous users

---

**Version**: 1.0.0-SNAPSHOT | **Last Updated**: 2026-02-11
