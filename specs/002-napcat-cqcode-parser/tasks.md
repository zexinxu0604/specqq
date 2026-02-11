# Tasks: NapCat CQ Code Parser & Message Statistics

**Feature**: `002-napcat-cqcode-parser`
**Input**: Design documents from `/specs/002-napcat-cqcode-parser/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create feature documentation directory at `specs/002-napcat-cqcode-parser/`
- [X] T002 [P] Add CQ code type constants to `src/main/java/com/specqq/chatbot/common/CQCodeConstants.java`
- [X] T003 [P] Configure Caffeine cache for CQ code patterns in `src/main/java/com/specqq/chatbot/config/CQCodeCacheConfig.java`
- [X] T004 [P] Create test data fixtures in `src/test/resources/test-messages.json` (sample CQ code messages)

**Checkpoint**: Basic project structure ready for user story implementation

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Core Parsing Infrastructure

- [X] T005 Create `CQCode` record in `src/main/java/com/specqq/chatbot/parser/CQCode.java` (immutable record with type, params, rawText)
- [X] T006 Create `CQCodeType` enum in `src/main/java/com/specqq/chatbot/parser/CQCodeType.java` (face, image, at, reply, record, video, other)
- [X] T007 Create `CQCodeParser` class in `src/main/java/com/specqq/chatbot/parser/CQCodeParser.java` (regex-based parser with pattern cache)
- [X] T008 Implement `CQCodeParser.parse()` method to extract CQ codes from message string (returns `List<CQCode>`)
- [X] T009 Implement `CQCodeParser.stripCQCodes()` method to remove CQ codes from message (returns plain text)
- [X] T010 Implement `CQCodeParser.validate()` method to check CQ code syntax (returns boolean + error messages)

### Core Statistics Infrastructure

- [X] T011 Create `MessageStatistics` record in `src/main/java/com/specqq/chatbot/service/MessageStatistics.java` (characterCount, cqCodeCounts map)
- [X] T012 Create `MessageStatisticsService` class in `src/main/java/com/specqq/chatbot/service/MessageStatisticsService.java`
- [X] T013 Implement `MessageStatisticsService.calculate()` method (parse CQ codes, count characters, group by type)
- [X] T014 Implement `MessageStatisticsService.formatStatistics()` method (format as Chinese reply: "文字: X字, 表情: Y个")

### WebSocket API Integration (Shared for US3)

- [X] T015 Create `ApiCallRequest` record in `src/main/java/com/specqq/chatbot/dto/ApiCallRequestDTO.java` (JSON-RPC 2.0 format)
- [X] T016 Create `ApiCallResponse` record in `src/main/java/com/specqq/chatbot/dto/ApiCallResponseDTO.java` (status, retcode, data, executionTimeMs)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Message Statistics Display (Priority: P1) 🎯 MVP

**Goal**: Automatically calculate and reply with message statistics (text + CQ code counts) for every message sent to a group with the statistics rule enabled.

**Independent Test**: Send a mixed message "Hello😊[image]" to a test group with statistics rule enabled, verify bot replies with "文字: 5字, 表情: 1个, 图片: 1张" within 2 seconds.

### Tests for User Story 1 (TDD - Write FIRST)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T017 [P] [US1] Unit test `CQCodeParserTest.should_ParseMixedMessage_When_ContainsTextAndCQCodes()` in `src/test/java/com/specqq/chatbot/unit/CQCodeParserTest.java`
- [X] T018 [P] [US1] Unit test `CQCodeParserTest.should_StripCQCodes_When_MessageContainsCQCodes()` in `src/test/java/com/specqq/chatbot/unit/CQCodeParserTest.java`
- [X] T019 [P] [US1] Unit test `CQCodeParserTest.should_HandleMalformedCQCode_When_MissingClosingBracket()` in `src/test/java/com/specqq/chatbot/unit/CQCodeParserTest.java`
- [X] T020 [P] [US1] Unit test `MessageStatisticsServiceTest.should_CalculateCorrectCount_When_ChineseMixedWithEnglish()` in `src/test/java/com/specqq/chatbot/unit/MessageStatisticsServiceTest.java`
- [X] T021 [P] [US1] Unit test `MessageStatisticsServiceTest.should_FormatOnlyNonZeroCounts_When_StatisticsCalculated()` in `src/test/java/com/specqq/chatbot/unit/MessageStatisticsServiceTest.java`
- [X] T022 [P] [US1] Unit test `MessageStatisticsServiceTest.should_GroupCQCodesByType_When_MultipleCodesPresent()` in `src/test/java/com/specqq/chatbot/unit/MessageStatisticsServiceTest.java`
- [X] T023 [US1] Integration test `StatisticsRuleIntegrationTest.should_SendStatisticsReply_When_UserSendsMessage()` in `src/test/java/com/specqq/chatbot/integration/StatisticsRuleIntegrationTest.java`
- [X] T024 [US1] Integration test `StatisticsRuleIntegrationTest.should_IgnoreBotMessages_When_BotSendsReply()` in `src/test/java/com/specqq/chatbot/integration/StatisticsRuleIntegrationTest.java`
- [X] T025 [US1] Integration test `StatisticsRuleIntegrationTest.should_RespectRateLimit_When_MultipleMessagesRapid()` in `src/test/java/com/specqq/chatbot/integration/StatisticsRuleIntegrationTest.java`

**⚠️ TDD CHECKPOINT - MANDATORY USER APPROVAL REQUIRED**

Before proceeding to implementation tasks (T026-T056), you MUST:
1. Review all test tasks T017-T025 with the user
2. Get explicit user approval for the test plan
3. Ensure tests are written FIRST and FAIL before implementation
4. Follow Red-Green-Refactor cycle: Write test → See it fail → Implement → See it pass → Refactor

**This checkpoint enforces the constitution's TDD principle: "先编写测试 → 用户审批 → 测试失败 → 实现代码 → 测试通过"**

### Implementation for User Story 1

#### CQ Code Parsing Implementation

- [X] T026 [P] [US1] Implement regex pattern for CQ code matching in `CQCodeParser.java` (pattern: `\[CQ:([a-z]+)(?:,([^\]]+))?\]`)
- [X] T027 [P] [US1] Implement parameter parsing logic in `CQCodeParser.java` (split by comma, parse key=value pairs)
- [X] T028 [P] [US1] Implement compiled pattern caching in `CQCodeParser.java` (use Caffeine cache, 100 entries, 4h TTL)
- [X] T029 [US1] Add error handling for malformed CQ codes in `CQCodeParser.java` (log error, treat as plain text, continue processing)

#### Character Counting Implementation

- [X] T030 [US1] Implement Unicode character counting in `MessageStatisticsService.java` (use `String.codePointCount()`, not `length()`)
- [X] T031 [US1] Implement CQ code stripping before character count in `MessageStatisticsService.java` (call `CQCodeParser.stripCQCodes()`)
- [X] T032 [US1] Add validation for edge cases in `MessageStatisticsService.java` (empty message, CQ-only message, 1000+ characters)

#### Statistics Formatting Implementation

- [X] T033 [US1] Implement Chinese label mapping in `MessageStatisticsService.java` (face→"表情", image→"图片", at→"@提及", etc.)
- [X] T034 [US1] Implement unit suffix mapping in `MessageStatisticsService.java` (face→"个", image→"张", record→"条", etc.)
- [X] T035 [US1] Implement non-zero filtering in `MessageStatisticsService.formatStatistics()` (only include types with count > 0)
- [X] T036 [US1] Implement formatted message assembly in `MessageStatisticsService.formatStatistics()` (join with ", ", e.g. "文字: 10字, 表情: 2个")

#### Rule Engine Integration

- [X] T037 [US1] Add statistics rule type to `MessageRule` entity in `src/main/java/com/specqq/chatbot/entity/MessageRule.java` (new match_type: 'statistics')
- [X] T038 [US1] Create statistics rule matcher in `src/main/java/com/specqq/chatbot/engine/StatisticsMatcher.java` (always matches, but checks if enabled)
- [X] T039 [US1] Integrate statistics matcher into `RuleEngine.java` (add to matcher factory, priority 1000 - highest)
- [X] T040 [US1] Implement bot self-message filtering in `RuleEngine.java` (retrieve bot's QQ ID via `get_login_info` API on startup, cache in memory, compare sender ID with cached bot ID, skip processing if match)
- [X] T041 [US1] Implement Redis-based rate limiter in `MessageStatisticsService.java` (Redis key: `rate_limit:statistics:{group_id}`, INCR + EXPIRE 5s, check count ≤1 before sending reply)

#### Message Reply Implementation

- [X] T042 [US1] Modify `NapCatAdapter.java` to send statistics reply (reuse existing `sendReply()` method)
- [X] T043 [US1] Add statistics reply logging in `MessageLogService.java` (log as type: 'statistics_reply')
- [X] T044 [US1] Add error handling for reply failures in `MessageStatisticsService.java` (log error, do not crash pipeline)

#### API Endpoints for User Story 1

- [X] T045 [P] [US1] Create `CQCodeController.java` in `src/main/java/com/specqq/chatbot/controller/CQCodeController.java`
- [X] T046 [P] [US1] Implement `POST /api/cqcode/parse` endpoint (parse CQ codes from message string)
- [X] T047 [P] [US1] Implement `POST /api/cqcode/strip` endpoint (strip CQ codes, return character count)
- [X] T048 [P] [US1] Implement `POST /api/statistics/calculate` endpoint (calculate statistics)
- [X] T049 [P] [US1] Implement `POST /api/statistics/format` endpoint (format statistics as Chinese reply)
- [X] T050 [P] [US1] Implement `POST /api/statistics/calculate-and-format` endpoint (combined operation for convenience)
- [X] T051 [P] [US1] Implement `POST /api/statistics/test` endpoint (test statistics with debug info: execution time, cache hit)
- [X] T052 [US1] Add DTOs for API requests/responses in `src/main/java/com/specqq/chatbot/dto/` (ParseCQCodeRequest, MessageStatisticsDTO, etc.)
- [X] T053 [US1] Add VOs for API responses in `src/main/java/com/specqq/chatbot/vo/MessageStatisticsVO.java`

#### Validation and Error Handling

- [X] T054 [US1] Add input validation in `CQCodeController.java` (use `@Valid`, max message length 10000 chars)
- [X] T055 [US1] Add global exception handler for CQ code parsing errors in `src/main/java/com/specqq/chatbot/exception/GlobalExceptionHandler.java`
- [X] T056 [US1] Add structured logging for statistics calculation in `MessageStatisticsService.java` (log execution time, cache hit rate)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently. Send a message to a test group and verify auto-reply with statistics.

---

## Phase 4: User Story 2 - CQ Code Rule Configuration (Priority: P2)

**Goal**: Provide frontend UI for administrators to easily configure rules that match specific CQ code patterns (images, emojis, mentions) without writing regex manually.

**Independent Test**: Create a rule via frontend UI by selecting "Contains Image" from dropdown, send an image message to the group, verify the rule triggers correctly.

### Tests for User Story 2 (TDD - Write FIRST)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T057 [P] [US2] Unit test `CQCodeServiceTest.should_GenerateRegexPattern_When_CQCodeTypeSelected()` in `src/test/java/com/specqq/chatbot/unit/CQCodeServiceTest.java`
- [X] T058 [P] [US2] Unit test `CQCodeServiceTest.should_ValidatePattern_When_CustomRegexProvided()` in `src/test/java/com/specqq/chatbot/unit/CQCodeServiceTest.java`
- [X] T059 [P] [US2] Unit test `CQCodeServiceTest.should_CombinePatterns_When_LogicalOperatorProvided()` in `src/test/java/com/specqq/chatbot/unit/CQCodeServiceTest.java`
- [ ] T060 [P] [US2] Frontend test `CQCodeSelector.spec.ts.should_PopulateDropdown_When_ComponentMounted()` in `frontend/src/tests/unit/CQCodeSelector.spec.ts`
- [ ] T061 [P] [US2] Frontend test `CQCodeSelector.spec.ts.should_EmitPattern_When_OptionSelected()` in `frontend/src/tests/unit/CQCodeSelector.spec.ts`
- [X] T062 [US2] Integration test `CQCodeIntegrationTest.should_MatchImageMessage_When_ContainsImagePatternConfigured()` in `src/test/java/com/specqq/chatbot/integration/CQCodeIntegrationTest.java`

### Implementation for User Story 2

#### Backend - CQ Code Pattern Service

- [X] T063 [P] [US2] Create `CQCodeService.java` in `src/main/java/com/specqq/chatbot/service/CQCodeService.java`
- [X] T064 [P] [US2] Create `CQCodePatternDTO.java` in `src/main/java/com/specqq/chatbot/dto/CQCodePatternDTO.java` (id, label, type, regexPattern, paramFilters, logicalOperator)
- [X] T065 [P] [US2] Implement predefined pattern templates in `CQCodeService.java` (contains-image, contains-face, contains-at, etc.)
- [X] T066 [US2] Implement pattern generation logic in `CQCodeService.generateRegexPattern()` (convert CQ code type to regex)
- [X] T067 [US2] Implement parameter filter logic in `CQCodeService.applyParamFilters()` (generate regex for param matching, e.g., id=123)
- [X] T068 [US2] Implement logical operator handling in `CQCodeService.combinePatterns()` (AND = all patterns, OR = any pattern)

#### Backend - API Endpoints for User Story 2

- [X] T069 [P] [US2] Implement `GET /api/cqcode/types` endpoint in `CQCodeController.java` (return supported CQ code types with Chinese labels)
- [X] T070 [P] [US2] Implement `GET /api/cqcode/patterns` endpoint in `CQCodeController.java` (return predefined patterns for dropdown)
- [X] T071 [P] [US2] Implement `POST /api/cqcode/patterns/validate` endpoint in `CQCodeController.java` (validate custom CQ code pattern)
- [X] T072 [P] [US2] Implement `POST /api/cqcode/validate` endpoint in `CQCodeController.java` (validate single CQ code syntax)

#### Frontend - CQ Code Selector Component

- [ ] T073 [P] [US2] Create `CQCodeSelector.vue` in `frontend/src/components/CQCodeSelector.vue` (Element Plus Cascader component)
- [ ] T074 [P] [US2] Create `CQCodePreview.vue` in `frontend/src/components/CQCodePreview.vue` (display human-readable CQ code description)
- [ ] T075 [P] [US2] Create `cqcode.ts` API client in `frontend/src/api/cqcode.ts` (axios calls for CQ code endpoints)
- [ ] T076 [US2] Implement pattern selection logic in `CQCodeSelector.vue` (fetch patterns from backend, populate dropdown)
- [ ] T077 [US2] Implement pattern preview in `CQCodePreview.vue` (show Chinese label, example match, regex pattern)
- [ ] T078 [US2] Add parameter filter UI in `CQCodeSelector.vue` (optional filters for CQ code parameters, e.g., id=123)

#### Frontend - Rule Edit Page Integration

- [ ] T079 [US2] Modify `RuleEdit.vue` in `frontend/src/views/rules/RuleEdit.vue` (add CQ code selector to rule configuration form)
- [ ] T080 [US2] Add CQ code pattern field to rule form in `RuleEdit.vue` (store selected pattern as regex string)
- [ ] T081 [US2] Implement pattern validation on save in `RuleEdit.vue` (call `/api/cqcode/patterns/validate` before saving rule)

#### Frontend - State Management

- [ ] T082 [P] [US2] Create `cqcode.ts` Pinia store in `frontend/src/stores/cqcode.ts` (manage CQ code patterns state)
- [ ] T083 [P] [US2] Create TypeScript types in `frontend/src/types/cqcode.ts` (CQCodePattern, CQCodeType, ParamFilter interfaces)
- [ ] T084 [US2] Implement pattern caching in `cqcode.ts` store (cache predefined patterns, refresh on demand)

#### Frontend - Utilities

- [ ] T085 [P] [US2] Create `cqcode-formatter.ts` in `frontend/src/utils/cqcode-formatter.ts` (format CQ code for display)
- [ ] T086 [P] [US2] Implement Chinese label mapping in `cqcode-formatter.ts` (face→"表情", image→"图片", etc.)
- [ ] T087 [P] [US2] Implement unit tests for `cqcode-formatter.spec.ts` in `frontend/src/tests/unit/cqcode-formatter.spec.ts`

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently. Administrators can create CQ code rules via UI, and statistics still auto-reply for every message.

---

## Phase 5: User Story 3 - Extended NapCat API Integration (Priority: P3)

**Goal**: Use the existing WebSocket connection to call additional NapCat API endpoints (beyond `send_group_msg`) with automatic HTTP fallback, enabling future rich interactions like file uploads, voice messages, reactions.

**Independent Test**: Call a new API endpoint (e.g., `get_group_member_info`) via WebSocket, verify correct JSON-RPC format request, receive and parse response successfully.

### Tests for User Story 3 (TDD - Write FIRST)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T088 [P] [US3] Unit test `NapCatAdapterTest.should_SendJsonRpcRequest_When_ApiCalled()` in `src/test/java/com/specqq/chatbot/unit/NapCatAdapterTest.java`
- [X] T089 [P] [US3] Unit test `NapCatAdapterTest.should_ParseJsonRpcResponse_When_WebSocketReplies()` in `src/test/java/com/specqq/chatbot/unit/NapCatAdapterTest.java`
- [X] T090 [P] [US3] Unit test `NapCatAdapterTest.should_FallbackToHttp_When_WebSocketTimesOut()` in `src/test/java/com/specqq/chatbot/unit/NapCatAdapterTest.java`
- [X] T091 [P] [US3] Unit test `NapCatAdapterTest.should_HandleError_When_BothWebSocketAndHttpFail()` in `src/test/java/com/specqq/chatbot/unit/NapCatAdapterTest.java`
- [x] T092 [US3] Integration test `NapCatApiIntegrationTest.should_CallGetGroupInfo_When_WebSocketActive()` in `src/test/java/com/specqq/chatbot/integration/NapCatApiIntegrationTest.java`
- [x] T093 [US3] Integration test `NapCatApiIntegrationTest.should_HandleConcurrentCalls_When_MultipleRequests()` in `src/test/java/com/specqq/chatbot/integration/NapCatApiIntegrationTest.java`

### Implementation for User Story 3

#### WebSocket JSON-RPC Implementation

- [x] T094 [US3] Add JSON-RPC 2.0 support to `NapCatAdapter.java` (jsonrpc: "2.0", id: UUID, action: string, params: map)
- [x] T095 [US3] Implement `NapCatAdapter.callApi()` method (send JSON-RPC request via WebSocket, wait for response)
- [x] T096 [US3] Implement request-response correlation in `NapCatAdapter.java` (map request ID to CompletableFuture, 10s timeout)
- [x] T097 [US3] Implement response parsing in `NapCatAdapter.handleApiResponse()` (extract status, retcode, data from JSON-RPC response)
- [x] T098 [US3] Add timeout handling in `NapCatAdapter.callApi()` (10s timeout per WebSocket call, throw TimeoutException)

#### HTTP Fallback Implementation

- [x] T099 [US3] Implement HTTP fallback logic in `NapCatAdapter.callApiWithFallback()` (try WebSocket first, HTTP POST on timeout/error)
- [x] T100 [US3] Add HTTP POST client in `NapCatAdapter.java` (RestTemplate or WebClient for HTTP fallback)
- [x] T101 [US3] Implement request transformation for HTTP in `NapCatAdapter.toHttpRequest()` (convert JSON-RPC to HTTP POST format)
- [x] T102 [US3] Implement response transformation from HTTP in `NapCatAdapter.fromHttpResponse()` (convert HTTP response to ApiCallResponse)

#### Error Handling and Monitoring

- [x] T103 [US3] Add error handling for API call failures in `NapCatAdapter.java` (log error, return meaningful error message)
- [x] T104 [US3] Add metrics for API call performance in `NapCatAdapter.java` (execution time, success rate, fallback rate)
- [x] T105 [US3] Add structured logging for API calls in `NapCatAdapter.java` (log action, params, status, execution time)

#### Additional NapCat API Methods

- [x] T106 [P] [US3] Implement `getGroupInfo()` method in `NapCatAdapter.java` (call `get_group_info` API)
- [x] T107 [P] [US3] Implement `getGroupMemberInfo()` method in `NapCatAdapter.java` (call `get_group_member_info` API)
- [x] T108 [P] [US3] Implement `getGroupMemberList()` method in `NapCatAdapter.java` (call `get_group_member_list` API)
- [x] T109 [P] [US3] Implement `deleteMessage()` method in `NapCatAdapter.java` (call `delete_msg` API)
- [x] T110 [P] [US3] Implement `sendForwardMessage()` method in `NapCatAdapter.java` (call `send_forward_msg` API - for future use)

**Checkpoint**: All user stories should now be independently functional. WebSocket API calls work with transparent HTTP fallback.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T111 [P] Update API documentation in `specs/002-napcat-cqcode-parser/contracts/` (add examples, clarify response formats)
- [x] T112 [P] Update developer quickstart in `specs/002-napcat-cqcode-parser/quickstart.md` (add setup instructions, API examples)
- [x] T113 [P] Add SonarQube analysis for new code in `pom.xml` (ensure ≥80% coverage, no Critical issues)
- [x] T114 Code cleanup: Remove dead code, optimize imports, format code per Alibaba style guide
- [ ] T115 Performance optimization: Profile CQ code parsing, optimize regex cache hit rate to ≥95%
- [ ] T116 Security hardening: Add rate limiting for all new API endpoints (100 requests/minute/IP)
- [x] T117 [P] Add Prometheus metrics for CQ code parsing in `src/main/java/com/specqq/chatbot/config/MetricsConfig.java` (parse count, cache hit rate, execution time)
- [x] T118 [P] Add actuator health check for NapCat WebSocket connection in `src/main/java/com/specqq/chatbot/config/HealthCheckConfig.java`
- [ ] T119 Run quickstart.md validation: Verify all setup steps work on clean environment
- [ ] T120 Update main README.md with CQ code feature description and usage examples

### Validation Tasks (Constitution Compliance)

- [ ] T121 Performance benchmark validation: Run JMeter test with 50 CQ codes, validate CQ parsing P95 <10ms, statistics calculation P95 <50ms, end-to-end API response P95 <200ms (per constitution requirement)
- [ ] T122 Test coverage validation: Generate JaCoCo report for backend and Vitest coverage report for frontend, verify backend ≥80%, frontend ≥70%, core business logic ≥90% (per constitution requirement)

**Checkpoint**: Both T121 and T122 MUST pass before deployment. Constitution requires API P95 <200ms and test coverage ≥80%/70%.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User Story 1 (P1) can start immediately after Phase 2
  - User Story 2 (P2) depends on US1 completion (needs statistics functionality to test CQ code rules)
  - User Story 3 (P3) is independent and can run in parallel with US2
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after US1 completion - Needs statistics functionality to validate CQ code rule matching
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Independent of US1 and US2, can run in parallel

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD workflow)
- Unit tests for parser/service before integration tests
- Backend API endpoints before frontend components
- Core implementation before error handling and monitoring
- Story complete and independently testable before moving to next priority

### Parallel Opportunities

- All Setup tasks (T001-T004) can run in parallel
- All Foundational tasks within subsections can run in parallel:
  - T005-T010 (parsing), T011-T014 (statistics), T015-T016 (API integration)
- Once Foundational phase completes:
  - US1 tests (T017-T025) can run in parallel (different test files)
  - US1 implementation tasks within subsections can run in parallel:
    - T026-T029 (parsing), T030-T032 (counting), T033-T036 (formatting), T045-T051 (endpoints)
  - US2 tests (T057-T062) can run in parallel
  - US2 backend tasks (T069-T072) can run in parallel with frontend tasks (T073-T087)
  - US3 can start in parallel with US2 after Phase 2 completion
  - US3 tests (T088-T093) can run in parallel
  - US3 additional API methods (T106-T110) can run in parallel
- Polish tasks (T111-T113, T117-T118) can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all unit tests for User Story 1 together:
Task T017: "Unit test CQCodeParserTest.should_ParseMixedMessage_When_ContainsTextAndCQCodes()"
Task T018: "Unit test CQCodeParserTest.should_StripCQCodes_When_MessageContainsCQCodes()"
Task T019: "Unit test CQCodeParserTest.should_HandleMalformedCQCode_When_MissingClosingBracket()"
Task T020: "Unit test MessageStatisticsServiceTest.should_CalculateCorrectCount_When_ChineseMixedWithEnglish()"
Task T021: "Unit test MessageStatisticsServiceTest.should_FormatOnlyNonZeroCounts_When_StatisticsCalculated()"
Task T022: "Unit test MessageStatisticsServiceTest.should_GroupCQCodesByType_When_MultipleCodesPresent()"

# Launch all parsing implementation tasks together (after tests fail):
Task T026: "Implement regex pattern for CQ code matching in CQCodeParser.java"
Task T027: "Implement parameter parsing logic in CQCodeParser.java"
Task T028: "Implement compiled pattern caching in CQCodeParser.java"

# Launch all API endpoints for User Story 1 together:
Task T045: "Create CQCodeController.java"
Task T046: "Implement POST /api/cqcode/parse endpoint"
Task T047: "Implement POST /api/cqcode/strip endpoint"
Task T048: "Implement POST /api/statistics/calculate endpoint"
Task T049: "Implement POST /api/statistics/format endpoint"
Task T050: "Implement POST /api/statistics/calculate-and-format endpoint"
Task T051: "Implement POST /api/statistics/test endpoint"
```

---

## Parallel Example: User Story 2

```bash
# Launch backend API endpoints and frontend components in parallel:

# Backend team:
Task T069: "Implement GET /api/cqcode/types endpoint"
Task T070: "Implement GET /api/cqcode/patterns endpoint"
Task T071: "Implement POST /api/cqcode/patterns/validate endpoint"
Task T072: "Implement POST /api/cqcode/validate endpoint"

# Frontend team (can work simultaneously):
Task T073: "Create CQCodeSelector.vue component"
Task T074: "Create CQCodePreview.vue component"
Task T075: "Create cqcode.ts API client"
Task T082: "Create cqcode.ts Pinia store"
Task T083: "Create TypeScript types in cqcode.ts"
Task T085: "Create cqcode-formatter.ts utility"
```

---

## Parallel Example: User Story 3

```bash
# Launch all additional NapCat API methods together (after core implementation):
Task T106: "Implement getGroupInfo() method in NapCatAdapter.java"
Task T107: "Implement getGroupMemberInfo() method in NapCatAdapter.java"
Task T108: "Implement getGroupMemberList() method in NapCatAdapter.java"
Task T109: "Implement deleteMessage() method in NapCatAdapter.java"
Task T110: "Implement sendForwardMessage() method in NapCatAdapter.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: Foundational (T005-T016) - CRITICAL - blocks all stories
3. Complete Phase 3: User Story 1 (T017-T056)
4. **STOP and VALIDATE**: Test User Story 1 independently
   - Send "Hello😊[image]" to test group
   - Verify bot replies "文字: 5字, 表情: 1个, 图片: 1张" within 2 seconds
   - Test rate limiting (send 3 messages rapidly, verify only 1 reply per 5s)
   - Test bot self-message filtering (bot's own replies should not trigger statistics)
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (T001-T016)
2. Add User Story 1 → Test independently → Deploy/Demo (MVP! 🎯)
3. Add User Story 2 → Test independently → Deploy/Demo
   - Create rule via UI selecting "Contains Image"
   - Send image message, verify rule triggers
   - Verify statistics still auto-reply for all messages
4. Add User Story 3 → Test independently → Deploy/Demo
   - Call `getGroupInfo()` via WebSocket, verify response
   - Simulate timeout, verify HTTP fallback works
   - Verify US1 and US2 still work correctly

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (T001-T016)
2. Once Foundational is done:
   - **Developer A**: User Story 1 (T017-T056) - Backend focus
   - **Developer B**: User Story 2 Backend (T057-T072) - After US1 completes
   - **Developer C**: User Story 2 Frontend (T073-T087) - Can start parallel with B
   - **Developer D**: User Story 3 (T088-T110) - Can start immediately after Phase 2
3. Stories complete and integrate independently
4. All developers collaborate on Phase 6: Polish (T111-T120)

---

## Dependency Graph

```
Phase 1 (Setup)
   └─→ Phase 2 (Foundational - BLOCKS all stories)
         ├─→ Phase 3 (User Story 1 - P1 🎯 MVP)
         │     └─→ Phase 4 (User Story 2 - P2)
         │
         └─→ Phase 5 (User Story 3 - P3) [Can run parallel with US2]

All User Stories Complete
   └─→ Phase 6 (Polish)
```

**Critical Path**: Phase 1 → Phase 2 → US1 → US2 → Polish
**Parallel Path**: Phase 1 → Phase 2 → US3 (independent)

---

## Task Summary

**Total Tasks**: 122 tasks
- Phase 1 (Setup): 4 tasks
- Phase 2 (Foundational): 12 tasks (CRITICAL - blocks all stories)
- Phase 3 (User Story 1): 40 tasks (including 9 tests)
- Phase 4 (User Story 2): 32 tasks (including 6 tests)
- Phase 5 (User Story 3): 22 tasks (including 6 tests)
- Phase 6 (Polish): 10 tasks
- **Constitution Validation**: 2 tasks (T121: Performance, T122: Coverage)

**MVP Scope**: Phases 1-3 + Validation (58 tasks total)
**Parallelizable Tasks**: ~50% (marked with [P])
**Test Tasks**: 21 tasks (TDD workflow - write tests first)

---

## Notes

- [P] tasks = different files, no dependencies, can run in parallel
- [Story] label maps task to specific user story for traceability (US1, US2, US3)
- Each user story should be independently completable and testable
- **TDD Workflow**: Write tests → Get approval → Verify tests FAIL → Implement → Verify tests PASS → Refactor
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Performance targets: CQ code parsing P95 <10ms, statistics calculation P95 <50ms, API response P95 <200ms
- Test coverage targets: Backend ≥80%, Frontend ≥70%
