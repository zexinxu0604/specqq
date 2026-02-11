# Feature Specification: NapCat CQ Code Message Parser & Statistics

**Feature Branch**: `002-napcat-cqcode-parser`
**Created**: 2026-02-11
**Status**: Draft
**Input**: User description: "本次需求,我希望1. 接入napcat的websocket接口,具体的接口文档可以参考已有的mcp服务器napcat-apifox的内容和我传入的api网站,目前系统内已经实现了与napcat websocket服务器的连接,本次需要实现使用该链接调用napcat接口实现基础的消息功能,具体参考消息接口中的内容。2. 由于设置的napcat服务器消息格式以string类型交互,消息中的表情、图片等以CQ码的形式表现,本次需要添加对这部分特殊格式消息的解析。3. 在前端的规则页面,支持用户对这部分特殊消息进行快速设置。4. 需求的最终目标是,添加一个规则,解析出群聊中的每条消息,最终统计出其中的文字数量和不同格式的CQ码数量,并将结果发送回群聊。"

## Clarifications

### Session 2026-02-11

- Q: 统计规则应该如何触发?当前规格说明提到"statistics rule enabled",但没有明确是自动对所有消息生效,还是需要特定触发词。 → A: 每条消息自动触发 - 群里每发一条消息,机器人就自动回复该消息的统计信息
- Q: 统计回复消息应该如何格式化?当前示例显示"文字: X字, 表情: Y个, 图片: Z张",但没有明确所有CQ码类型都要显示还是只显示非零的。 → A: 仅显示非零项 - 只显示数量>0的类型(例如纯文本消息只显示"文字: 10字")
- Q: 当统计规则启用时,机器人应该如何处理自己发送的消息? → A: 忽略机器人自己的消息 - 不统计也不回复机器人自己发送的消息(避免无限循环)
- Q: 对于中文文本,字符计数应该如何处理?示例中"你好世界"显示为4字,但没有明确是按字符数还是字节数。 → A: 按字符数计数 - 一个汉字算1个字符(例如:"你好世界"=4字,"Hello"=5字)
- Q: User Story 3提到"使用现有WebSocket连接调用NapCat API",但没有明确是通过WebSocket发送API请求,还是继续使用HTTP。 → A: WebSocket优先降级 - 优先尝试WebSocket,失败时自动降级到HTTP

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Message Statistics Display (Priority: P1)

As a group chat administrator, I want to see statistics about message content (text count and different types of media elements) so that I can understand communication patterns in my group.

**Why this priority**: This is the core value proposition - providing actionable insights about group chat activity. It delivers immediate value by showing what types of content are being shared.

**Independent Test**: Can be fully tested by sending a mixed message (text + emoji + image) to the group and verifying that the bot replies with accurate counts: "文字: X字, 表情: Y个, 图片: Z张"

**Trigger Behavior**: Statistics rule automatically processes and replies to EVERY message sent in the group chat when enabled (no trigger word required).

**Acceptance Scenarios**:

1. **Given** a group chat with the statistics rule enabled, **When** a user sends "Hello 😊 [image]", **Then** the bot **immediately** replies with "文字: 5字, 表情: 1个, 图片: 1张" (all non-zero items shown)
2. **Given** a group chat with the statistics rule enabled, **When** a user sends only text "你好世界", **Then** the bot **immediately** replies with "文字: 4字" (zero-count items omitted)
3. **Given** a group chat with the statistics rule enabled, **When** a user sends multiple CQ codes "[emoji][emoji][image][image]", **Then** the bot **immediately** replies with correctly counted statistics for each type separately
4. **Given** a group chat with the statistics rule enabled, **When** multiple users send messages rapidly, **Then** the bot replies to each message individually with its own statistics
5. **Given** a group chat with the statistics rule enabled, **When** the bot itself sends a message (including statistics replies), **Then** the bot does NOT generate statistics for its own messages (preventing infinite loop)

---

### User Story 2 - CQ Code Rule Configuration (Priority: P2)

As a chatbot administrator, I want to easily configure rules that respond to specific CQ code patterns (like images or emojis) so that I can create rich interactive experiences without manually writing CQ code syntax.

**Why this priority**: This enables administrators to leverage the full power of QQ's rich media features without technical knowledge. It's a force multiplier for rule creation.

**Independent Test**: Can be fully tested by creating a rule via the UI that triggers on "any message with an image", then sending an image to verify the rule triggers correctly.

**Acceptance Scenarios**:

1. **Given** the rule configuration page, **When** admin selects "Contains Image" from CQ code filter dropdown, **Then** the rule pattern is automatically populated with the correct CQ code pattern
2. **Given** a rule configured to trigger on emoji, **When** a user sends a message with an emoji, **Then** the rule matches and sends the configured response
3. **Given** the rule configuration UI, **When** admin previews a CQ code pattern, **Then** the UI shows a human-readable description (e.g., "CQ:image" → "图片消息")

---

### User Story 3 - Extended NapCat API Integration (Priority: P3)

As a system developer, I want to use the existing WebSocket connection to call additional NapCat API endpoints (beyond just send_group_msg) so that the system can support richer interactions in the future.

**Why this priority**: This is infrastructure work that enables future features (file uploads, voice messages, reactions). It has lower immediate user value but provides technical foundation.

**Independent Test**: Can be fully tested by calling a new API endpoint (e.g., get_group_member_info) via the WebSocket connection and verifying the response is correctly parsed.

**API Call Strategy**: System prioritizes WebSocket for API calls (JSON-RPC format), automatically falling back to HTTP POST if WebSocket call fails or times out.

**Acceptance Scenarios**:

1. **Given** the NapCat adapter with active WebSocket connection, **When** a new API method is called (e.g., getGroupInfo), **Then** the request is sent via WebSocket first with correct JSON-RPC format and the response is parsed
2. **Given** the WebSocket connection is active, **When** multiple API calls are made in quick succession, **Then** all calls complete successfully without connection issues
3. **Given** an API call fails via WebSocket, **When** the error or timeout occurs, **Then** the system automatically retries via HTTP POST fallback
4. **Given** both WebSocket and HTTP fail, **When** the final error response is received, **Then** the system logs the error and returns a meaningful error message to the caller

---

### Edge Cases

- What happens when a message contains malformed CQ codes (e.g., `[CQ:face,id=` without closing bracket)?
- How does the system handle extremely long messages (>1000 characters) with hundreds of CQ codes?
- What happens when the same CQ code appears multiple times in one message (e.g., 50 identical emojis)?
- How does the parser distinguish between literal text "[CQ:face]" and an actual CQ code?
- What happens when a new, unknown CQ code type is encountered (e.g., `[CQ:future_type]`)?
- How does the system behave when NapCat API returns an error during message sending?
- How does the system identify and filter out messages sent by the bot itself to prevent infinite loops?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST parse CQ code format strings into structured data with type and parameters (e.g., `[CQ:face,id=123]` → type: "face", params: {id: 123})
- **FR-002**: System MUST support parsing at minimum these CQ code types: face (emoji), image (图片), at (mention), reply (回复), record (语音), video (视频)
- **FR-003**: System MUST calculate accurate character counts (by character, not bytes) excluding CQ codes, where each character (Chinese, English, digit, symbol) counts as 1 (e.g., "Hello[CQ:face,id=1]" → 5字, "你好世界" → 4字)
- **FR-004**: System MUST count CQ codes by type and report aggregate statistics, displaying ONLY non-zero counts (e.g., "图片: 3张, 表情: 2个, 语音: 1个" - omitting types with 0 count)
- **FR-005**: System MUST automatically send statistics as a formatted reply message to the same group chat within 2 seconds for EVERY user message received when the statistics rule is enabled, excluding messages sent by the bot itself (bot self-ID retrieved via `get_login_info` API on startup and cached for message filtering)
- **FR-006**: Frontend rule configuration page MUST provide a dropdown/selector for common CQ code patterns (contains image, contains emoji, contains mention, etc.)
- **FR-007**: Frontend MUST display human-readable labels for CQ code types (e.g., "CQ:image" shows as "图片", "CQ:face" shows as "表情")
- **FR-008**: System MUST use the existing WebSocket connection to call NapCat API endpoints beyond send_group_msg, with automatic HTTP fallback on WebSocket failure
- **FR-009**: System MUST handle API call responses asynchronously and provide timeout handling (10 second timeout per WebSocket call, automatic fallback to HTTP on timeout)
- **FR-010**: System MUST log all CQ code parsing errors without crashing the message processing pipeline
- **FR-011**: Statistics rule MUST be toggleable (can be enabled/disabled per group chat)
- **FR-012**: System MUST gracefully handle malformed CQ codes by treating them as plain text
- **FR-013**: Frontend CQ code selector MUST support combining multiple CQ code filters with AND/OR logic (e.g., "contains image AND contains text")

### Key Entities *(include if feature involves data)*

- **CQ Code**: Represents a parsed CQ code element with type (string), parameters (key-value map), and raw text
- **Message Statistics**: Contains counts for text characters, and counts per CQ code type (face, image, at, reply, record, video, other)
- **CQ Code Pattern**: Frontend configuration object that defines matching criteria for CQ codes (type, parameter filters, logical operators)
- **API Call Request**: Represents a NapCat API invocation with action name, parameters, and callback handlers
- **API Call Response**: Contains status code, data payload, error message, and execution time

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users receive accurate message statistics within 2 seconds of sending a message to a group with the statistics rule enabled
- **SC-002**: CQ code parser correctly identifies and categorizes at least 6 different CQ code types (face, image, at, reply, record, video) with 100% accuracy for well-formed codes
- **SC-003**: System processes messages containing up to 50 CQ codes without performance degradation (P95 response time remains <200ms)
- **SC-004**: Administrators can create a CQ code-based rule in under 1 minute using the UI selector (vs. 5+ minutes manually writing CQ code regex)
- **SC-005**: CQ code parsing errors occur in less than 0.1% of messages (excluding intentionally malformed input)
- **SC-006**: Extended NapCat API calls (beyond send_group_msg) complete successfully 99% of the time when NapCat service is healthy, with WebSocket-to-HTTP fallback occurring transparently when needed
- **SC-007**: Frontend displays CQ code types in localized Chinese labels with 100% coverage for all supported types

## Assumptions *(mandatory)*

- NapCat service is configured to use string-based CQ code format (not array format) for message content
- The existing WebSocket connection to NapCat is stable and supports bidirectional communication for API calls (JSON-RPC format), with HTTP available as fallback
- QQ platform rate limits (20-30 messages/minute/group) are sufficient for statistics replies
- Administrators understand basic rule configuration concepts (patterns, priorities, enable/disable)
- CQ code syntax follows OneBot 11 standard specification
- The system will initially support Chinese language UI labels; internationalization is out of scope
- Message statistics do not need to be persisted long-term (only sent as immediate replies)

## Dependencies *(mandatory)*

- Existing NapCat WebSocket connection (NapCatWebSocketHandler)
- Existing message parsing infrastructure (NapCatAdapter.parseMessage)
- Existing rule engine for triggering statistics calculation (RuleEngine)
- Frontend rule configuration page (Vue 3 + Element Plus)
- NapCat API documentation for extended endpoints (available via MCP server)

## Out of Scope *(mandatory)*

- Parsing CQ code array format (only string format is supported)
- Advanced CQ code generation (creating rich media messages beyond simple text replies)
- Historical message statistics (analyzing past messages)
- Real-time statistics dashboard (only per-message immediate replies)
- Custom CQ code type definitions (only standard OneBot 11 types)
- Multi-language support beyond Chinese (English labels are out of scope)
- CQ code validation before sending (system assumes NapCat will reject invalid codes)
- Message content filtering or censorship based on CQ codes
- Integration with external analytics platforms

## Technical Constraints *(optional)*

- Must use existing WebSocket connection (no new connections)
- CQ code parsing must complete in <10ms (P95), <15ms (P99) to meet overall API P95 <200ms response target (per plan.md performance goals)
- Frontend CQ code selector must work with existing Element Plus component library
- Must maintain backward compatibility with existing message parsing logic
- Cannot modify NapCat message format (must work with string-based CQ codes as-is)
- Statistics reply format must fit within QQ message length limits (approximately 5000 characters)

## Security & Privacy Considerations *(optional)*

- CQ code parsing must not execute arbitrary code (treat all input as data, not code)
- Statistics replies must not expose sensitive user information (e.g., user IDs, phone numbers)
- Malformed CQ codes must not cause denial-of-service by consuming excessive CPU/memory
- API call parameters must be validated to prevent injection attacks
- Error messages must not leak internal system details (e.g., file paths, stack traces)
- Rate limiting must prevent abuse of statistics rule (max 1 statistics reply per 5 seconds per group)

## Future Enhancements *(optional)*

- Support for CQ code array format in addition to string format
- Advanced statistics: most active users, peak activity times, media type trends over time
- CQ code composition UI: visual builder for creating rich media messages
- Integration with external image/video hosting services for media uploads
- Custom CQ code type plugins: allow administrators to define new CQ code types
- Batch message processing: analyze multiple messages at once for aggregate statistics
- Export statistics to CSV/Excel for offline analysis
- Webhook notifications when certain CQ code patterns are detected (e.g., spam detection)
