# Tasks: Group Chat Auto-Sync & Rule Management

**Input**: Design documents from `/specs/004-group-sync/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/group-sync-api.yaml, quickstart.md

**Tests**: This feature follows TDD (Test-Driven Development) as required by project constitution. All tests must be written FIRST and FAIL before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

This project uses **Web application** structure:
- **Backend**: `src/main/java/com/specqq/chatbot/` (Spring Boot 3.1.8)
- **Frontend**: `frontend/src/` (Vue 3 + TypeScript)
- **Database**: `src/main/resources/db/migration/` (Flyway migrations)
- **Tests**: `src/test/java/com/specqq/chatbot/` (JUnit 5 + Mockito + TestContainers)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration and foundational configuration

- [ ] T001 Create database migration script V7__group_sync_feature.sql in src/main/resources/db/migration/
- [ ] T002 [P] Create SystemConfig entity in src/main/java/com/specqq/chatbot/entity/SystemConfig.java
- [ ] T003 [P] Create SyncStatus enum in src/main/java/com/specqq/chatbot/enums/SyncStatus.java
- [ ] T004 [P] Create SystemConfigKeys constants in src/main/java/com/specqq/chatbot/constant/SystemConfigKeys.java
- [ ] T005 Extend GroupChat entity with sync fields in src/main/java/com/specqq/chatbot/entity/GroupChat.java
- [ ] T006 [P] Create SystemConfigMapper interface in src/main/java/com/specqq/chatbot/mapper/SystemConfigMapper.java
- [ ] T007 [P] Create SystemConfigMapper XML in src/main/resources/mapper/SystemConfigMapper.xml
- [ ] T008 Extend GroupChatMapper with sync queries in src/main/java/com/specqq/chatbot/mapper/GroupChatMapper.java
- [ ] T009 Extend GroupChatMapper XML with sync queries in src/main/resources/mapper/GroupChatMapper.xml
- [ ] T010 [P] Configure Caffeine cache for system config in src/main/resources/application.yml
- [ ] T011 [P] Configure Spring Scheduler in src/main/resources/application.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core services and infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T012 Create DefaultRuleConfigDTO in src/main/java/com/specqq/chatbot/dto/DefaultRuleConfigDTO.java
- [ ] T013 [P] Create GroupSyncRequestDTO in src/main/java/com/specqq/chatbot/dto/GroupSyncRequestDTO.java
- [ ] T014 [P] Create GroupSyncResultDTO in src/main/java/com/specqq/chatbot/dto/GroupSyncResultDTO.java
- [ ] T015 [P] Create GroupSyncStatusVO in src/main/java/com/specqq/chatbot/vo/GroupSyncStatusVO.java
- [ ] T016 [P] Create SyncHistoryRecordVO in src/main/java/com/specqq/chatbot/vo/SyncHistoryRecordVO.java
- [ ] T017 [P] Create SyncStatisticsVO in src/main/java/com/specqq/chatbot/vo/SyncStatisticsVO.java
- [ ] T018 Extend NapCatApiService with group info methods in src/main/java/com/specqq/chatbot/service/NapCatApiService.java
- [ ] T019 Create DefaultRuleConfigService in src/main/java/com/specqq/chatbot/service/DefaultRuleConfigService.java
- [ ] T020 Configure Resilience4j retry policy in src/main/resources/application.yml

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Automatic Group Chat Discovery (Priority: P1) 🎯 MVP

**Goal**: Automatically detect and save new group information when bot joins a QQ group

**Independent Test**: Have bot join a test QQ group and verify group information (ID, name, member count) appears in database within 30 seconds without manual admin action

### Tests for User Story 1 (TDD - Write FIRST, Ensure FAIL)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T021 [P] [US1] Unit test for GroupJoinEventListener in src/test/java/com/specqq/chatbot/unit/GroupJoinEventListenerTest.java
- [ ] T022 [P] [US1] Integration test for group discovery in src/test/java/com/specqq/chatbot/integration/GroupDiscoveryIntegrationTest.java
- [ ] T023 [P] [US1] Contract test for NapCat group_increase event in src/test/java/com/specqq/chatbot/contract/NapCatGroupEventContractTest.java

### Implementation for User Story 1

- [ ] T024 [US1] Create GroupJoinEventListener in src/main/java/com/specqq/chatbot/event/GroupJoinEventListener.java
- [ ] T025 [US1] Implement handleGroupJoinEvent method with duplicate detection logic
- [ ] T026 [US1] Add group information fetch from NapCat API with 10s timeout
- [ ] T027 [US1] Implement database save with active=true and sync_status=SUCCESS
- [ ] T028 [US1] Add error handling and logging for join event failures
- [ ] T029 [US1] Implement rejoin detection (update existing group to active=true)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently. Bot joining a group automatically saves it to database.

---

## Phase 4: User Story 2 - Scheduled Group Information Refresh (Priority: P1)

**Goal**: Periodically sync all active groups' information from NapCat API (every 6 hours by default)

**Independent Test**: Schedule sync job, modify a group's name in QQ, wait for sync interval, verify database reflects updated group name

### Tests for User Story 2 (TDD - Write FIRST, Ensure FAIL)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T030 [P] [US2] Unit test for GroupSyncService in src/test/java/com/specqq/chatbot/unit/GroupSyncServiceTest.java
- [ ] T031 [P] [US2] Unit test for GroupSyncScheduler in src/test/java/com/specqq/chatbot/unit/GroupSyncSchedulerTest.java
- [ ] T032 [P] [US2] Integration test for scheduled sync in src/test/java/com/specqq/chatbot/integration/ScheduledSyncIntegrationTest.java
- [ ] T033 [P] [US2] Performance test for 100 groups sync in src/test/java/com/specqq/chatbot/performance/SyncPerformanceTest.java

### Implementation for User Story 2

- [ ] T034 [P] [US2] Create GroupSyncService interface in src/main/java/com/specqq/chatbot/service/GroupSyncService.java
- [ ] T035 [US2] Implement GroupSyncServiceImpl with core sync logic in src/main/java/com/specqq/chatbot/service/impl/GroupSyncServiceImpl.java
- [ ] T036 [US2] Implement selectActiveGroups query with pagination (batch size 50)
- [ ] T037 [US2] Implement NapCat API calls with 10s timeout per group
- [ ] T038 [US2] Implement retry mechanism with Resilience4j (30s, 2min, 5min backoff)
- [ ] T039 [US2] Implement batch update for sync success/failure status
- [ ] T040 [US2] Implement inactive group detection (mark active=false when bot removed)
- [ ] T041 [US2] Add comprehensive logging for sync operations (SLF4J)
- [ ] T042 [US2] Create GroupSyncScheduler in src/main/java/com/specqq/chatbot/scheduler/GroupSyncScheduler.java
- [ ] T043 [US2] Configure @Scheduled with cron expression (0 0 */6 * * ?)
- [ ] T044 [US2] Implement concurrency control with ReentrantLock (single instance)
- [ ] T045 [US2] Add Prometheus metrics for sync success rate and duration

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently. Groups are auto-discovered and periodically synced.

---

## Phase 5: User Story 3 - Default Rule Configuration for New Groups (Priority: P2)

**Goal**: Apply configurable default rules to newly discovered groups automatically

**Independent Test**: Configure default rules in admin settings, have bot join new group, verify new group has configured default rules enabled

### Tests for User Story 3 (TDD - Write FIRST, Ensure FAIL)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T046 [P] [US3] Unit test for DefaultRuleConfigService in src/test/java/com/specqq/chatbot/unit/DefaultRuleConfigServiceTest.java
- [ ] T047 [P] [US3] Integration test for default rule application in src/test/java/com/specqq/chatbot/integration/DefaultRuleApplicationIntegrationTest.java
- [ ] T048 [P] [US3] Contract test for default rule config API in src/test/java/com/specqq/chatbot/contract/DefaultRuleConfigApiContractTest.java

### Implementation for User Story 3

- [ ] T049 [US3] Implement DefaultRuleConfigService with cache in src/main/java/com/specqq/chatbot/service/impl/DefaultRuleConfigServiceImpl.java
- [ ] T050 [US3] Implement getDefaultRuleConfig with Caffeine cache
- [ ] T051 [US3] Implement updateDefaultRuleConfig with cache eviction
- [ ] T052 [US3] Implement applyDefaultRulesToGroup method
- [ ] T053 [US3] Integrate default rule application into GroupJoinEventListener
- [ ] T054 [US3] Add validation for rule IDs existence before applying
- [ ] T055 [US3] Implement batch insert for GroupRuleConfig records
- [ ] T056 [US3] Add logging for default rule application operations

**Checkpoint**: At this point, User Stories 1, 2, AND 3 should all work independently. New groups get default rules automatically.

---

## Phase 6: User Story 4 - Manual Group Sync Trigger (Priority: P3)

**Goal**: Allow administrators to manually trigger full sync from admin interface

**Independent Test**: Click "Sync Now" button in admin interface, verify all group information refreshes within 1 minute with success/failure notification

### Tests for User Story 4 (TDD - Write FIRST, Ensure FAIL)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T057 [P] [US4] Unit test for GroupSyncController in src/test/java/com/specqq/chatbot/unit/GroupSyncControllerTest.java
- [ ] T058 [P] [US4] Integration test for manual sync API in src/test/java/com/specqq/chatbot/integration/ManualSyncApiIntegrationTest.java
- [ ] T059 [P] [US4] Contract test for sync API endpoints in src/test/java/com/specqq/chatbot/contract/GroupSyncApiContractTest.java

### Implementation for User Story 4

#### Backend API Implementation

- [ ] T060 [P] [US4] Create GroupSyncController in src/main/java/com/specqq/chatbot/controller/GroupSyncController.java
- [ ] T061 [US4] Implement POST /api/groups/sync endpoint (manual trigger)
- [ ] T062 [US4] Add concurrency check (reject if sync running with 409 status)
- [ ] T063 [US4] Implement async execution with CompletableFuture
- [ ] T064 [US4] Implement GET /api/groups/{id}/sync-status endpoint
- [ ] T065 [US4] Implement GET /api/groups/sync-history endpoint with pagination
- [ ] T066 [US4] Implement GET /api/groups/sync-statistics endpoint
- [ ] T067 [US4] Implement GET /api/config/default-rules endpoint
- [ ] T068 [US4] Implement PUT /api/config/default-rules endpoint
- [ ] T069 [US4] Add @Valid validation for all request DTOs
- [ ] T070 [US4] Add JWT authentication check for all endpoints

#### Frontend UI Implementation

- [ ] T071 [P] [US4] Create groupSync API client in frontend/src/api/groupSync.ts
- [ ] T072 [P] [US4] Create GroupSyncStatus component in frontend/src/components/GroupSyncStatus.vue
- [ ] T073 [P] [US4] Create groupSync Pinia store in frontend/src/stores/groupSync.ts
- [ ] T074 [US4] Extend GroupManagement view with sync button in frontend/src/views/GroupManagement.vue
- [ ] T075 [US4] Add sync status display with last sync time and failure info
- [ ] T076 [US4] Implement "Sync Now" button with loading state
- [ ] T077 [US4] Add success/error notification using Element Plus
- [ ] T078 [US4] Implement sync history dialog with pagination
- [ ] T079 [US4] Add default rule configuration dialog
- [ ] T080 [US4] Implement rule selection with checkbox list

**Checkpoint**: All user stories should now be independently functional. Administrators can manually trigger sync and configure default rules.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T081 [P] Add comprehensive Javadoc to all public methods and classes
- [x] T082 [P] Add SLF4J debug logging for troubleshooting in all services
- [x] T083 [P] Configure Prometheus metrics for sync operations in src/main/resources/application.yml
- [x] T084 [P] Add health check indicator for sync task status in src/main/java/com/specqq/chatbot/health/SyncHealthIndicator.java
- [ ] T085 [P] Create NapCat mock server script in tools/napcat-mock/server.js (from quickstart.md)
- [ ] T086 [P] Add frontend unit tests for GroupSyncStatus component in frontend/tests/unit/GroupSyncStatus.spec.ts
- [ ] T087 Code cleanup: Extract magic numbers to constants
- [ ] T088 Code cleanup: Simplify complex methods (cyclomatic complexity ≤ 10)
- [ ] T089 Performance optimization: Add database indexes for sync queries
- [ ] T090 Security: Validate all API inputs with @Valid and custom validators
- [ ] T091 Run quickstart.md validation: Execute all test scenarios
- [ ] T092 Update CLAUDE.md with feature documentation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - User Story 1 (P1): Can start after Phase 2 - No dependencies on other stories
  - User Story 2 (P1): Can start after Phase 2 - No dependencies on other stories
  - User Story 3 (P2): Can start after Phase 2 - Integrates with US1 but independently testable
  - User Story 4 (P3): Can start after Phase 2 - Uses services from US2 but independently testable
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories (uses same GroupChat entity)
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Integrates with US1's GroupJoinEventListener but independently testable
- **User Story 4 (P3)**: Can start after Foundational (Phase 2) - Uses GroupSyncService from US2 but independently testable

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD required by constitution)
- DTOs/VOs before services
- Services before controllers
- Backend before frontend
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel (T002, T003, T004, T006, T007, T010, T011)
- All Foundational tasks marked [P] can run in parallel within Phase 2
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- DTOs/VOs within a story marked [P] can run in parallel
- Backend and frontend tasks within US4 marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (TDD):
Task T021: "Unit test for GroupJoinEventListener in src/test/java/com/specqq/chatbot/unit/GroupJoinEventListenerTest.java"
Task T022: "Integration test for group discovery in src/test/java/com/specqq/chatbot/integration/GroupDiscoveryIntegrationTest.java"
Task T023: "Contract test for NapCat group_increase event in src/test/java/com/specqq/chatbot/contract/NapCatGroupEventContractTest.java"

# After tests written and failing, implement in sequence:
# T024 → T025 → T026 → T027 → T028 → T029
```

## Parallel Example: User Story 2

```bash
# Launch all tests for User Story 2 together (TDD):
Task T030: "Unit test for GroupSyncService in src/test/java/com/specqq/chatbot/unit/GroupSyncServiceTest.java"
Task T031: "Unit test for GroupSyncScheduler in src/test/java/com/specqq/chatbot/unit/GroupSyncSchedulerTest.java"
Task T032: "Integration test for scheduled sync in src/test/java/com/specqq/chatbot/integration/ScheduledSyncIntegrationTest.java"
Task T033: "Performance test for 100 groups sync in src/test/java/com/specqq/chatbot/performance/SyncPerformanceTest.java"

# Launch parallel service implementations:
Task T034: "Create GroupSyncService interface"
Task T042: "Create GroupSyncScheduler"
```

## Parallel Example: User Story 4

```bash
# Backend and Frontend can work in parallel:

# Backend team:
Task T060: "Create GroupSyncController"
Task T061-T070: "Implement API endpoints"

# Frontend team (parallel):
Task T071: "Create groupSync API client"
Task T072: "Create GroupSyncStatus component"
Task T073: "Create groupSync Pinia store"
```

---

## Implementation Strategy

### MVP First (User Stories 1 & 2 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Auto-discovery)
4. **STOP and VALIDATE**: Test US1 independently
5. Complete Phase 4: User Story 2 (Scheduled sync)
6. **STOP and VALIDATE**: Test US1 + US2 together
7. Deploy/demo if ready (MVP with auto-discovery + scheduled sync)

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (Auto-discovery MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo (Complete sync automation!)
4. Add User Story 3 → Test independently → Deploy/Demo (Default rules automation!)
5. Add User Story 4 → Test independently → Deploy/Demo (Full feature complete!)
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (CRITICAL)
2. Once Foundational is done:
   - Developer A: User Story 1 (Auto-discovery)
   - Developer B: User Story 2 (Scheduled sync)
   - Developer C: User Story 3 (Default rules)
3. After US1, US2, US3 complete:
   - Developer D: User Story 4 (Manual trigger + UI)
4. Stories complete and integrate independently

---

## Task Summary

**Total Tasks**: 92 tasks
- **Phase 1 (Setup)**: 11 tasks
- **Phase 2 (Foundational)**: 9 tasks
- **Phase 3 (User Story 1)**: 9 tasks (3 tests + 6 implementation)
- **Phase 4 (User Story 2)**: 16 tasks (4 tests + 12 implementation)
- **Phase 5 (User Story 3)**: 11 tasks (3 tests + 8 implementation)
- **Phase 6 (User Story 4)**: 24 tasks (3 tests + 21 implementation: 11 backend + 10 frontend)
- **Phase 7 (Polish)**: 12 tasks

**Parallel Opportunities**: 34 tasks marked [P] can run in parallel within their phases

**Independent Test Criteria**:
- **US1**: Bot joins test group → Group appears in database within 30s
- **US2**: Modify group name in QQ → Sync job updates database
- **US3**: Configure default rules → New group gets rules automatically
- **US4**: Click "Sync Now" → All groups refresh within 1 minute

**Suggested MVP Scope**: User Stories 1 & 2 (Auto-discovery + Scheduled sync = 25 tasks)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- TDD required by project constitution: Write tests FIRST, ensure they FAIL before implementing
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- All public methods and classes MUST have Javadoc (project constitution requirement)
- Code complexity: Single method cyclomatic complexity ≤ 10
- Test coverage: Unit tests ≥ 80%, core sync logic ≥ 90%
