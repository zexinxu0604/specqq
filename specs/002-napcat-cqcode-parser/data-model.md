# Data Model: NapCat CQ Code Parser & Message Statistics

**Feature**: `002-napcat-cqcode-parser` | **Date**: 2026-02-11

## Overview

This feature introduces CQ code parsing and message statistics calculation. Unlike typical CRUD features, this is primarily a **computational feature** with minimal persistent data requirements. Most data structures are runtime objects (DTOs/VOs) for processing and API communication.

**Key Design Decisions**:
- **No new database tables**: Reuse existing `message_rules`, `group_chat`, `message_log` tables
- **In-memory processing**: CQ codes are parsed on-the-fly, not persisted
- **Cache-first architecture**: Compiled regex patterns and statistics rules cached in Caffeine/Redis

---

## Core Entities

### 1. CQCode (Runtime Entity - Java Record)

**Purpose**: Represents a parsed CQ code element from a message string.

**Definition**:
```java
/**
 * Parsed CQ Code Element
 *
 * Immutable record representing a CQ code parsed from message text.
 * Example: [CQ:face,id=123] → CQCode("face", {"id": "123"}, "[CQ:face,id=123]")
 */
public record CQCode(
    String type,               // CQ code type (e.g., "face", "image", "at")
    Map<String, String> params, // Key-value parameters (e.g., {"id": "123", "url": "..."})
    String rawText             // Original CQ code text from message
) {
    // Standard CQ code types (OneBot 11 specification)
    public static final String TYPE_FACE = "face";      // 表情/emoji
    public static final String TYPE_IMAGE = "image";    // 图片
    public static final String TYPE_AT = "at";          // @mention
    public static final String TYPE_REPLY = "reply";    // 回复
    public static final String TYPE_RECORD = "record";  // 语音
    public static final String TYPE_VIDEO = "video";    // 视频
    public static final String TYPE_OTHER = "other";    // Unknown types

    /**
     * Get parameter value by key
     */
    public String getParam(String key) {
        return params.get(key);
    }

    /**
     * Check if this is a known CQ code type
     */
    public boolean isKnownType() {
        return Set.of(TYPE_FACE, TYPE_IMAGE, TYPE_AT, TYPE_REPLY, TYPE_RECORD, TYPE_VIDEO)
            .contains(type.toLowerCase());
    }
}
```

**Attributes**:
| Field | Type | Description | Example |
|-------|------|-------------|---------|
| type | String | CQ code type (lowercase) | "face", "image", "at" |
| params | Map<String,String> | Key-value parameters | {"id": "123", "url": "..."} |
| rawText | String | Original CQ code string | "[CQ:face,id=123]" |

**Validation Rules**:
- `type`: Non-null, non-empty, lowercase, alphanumeric
- `params`: Non-null (empty map if no parameters)
- `rawText`: Non-null, matches regex pattern `\[CQ:[a-z]+(?:,[^\]]+)?\]`

**Lifecycle**: Created during message parsing, discarded after statistics calculation (not persisted).

---

### 2. MessageStatistics (Runtime Entity - Java Record)

**Purpose**: Aggregated statistics for a single message.

**Definition**:
```java
/**
 * Message Statistics
 *
 * Aggregated counts of text characters and CQ codes by type.
 */
public record MessageStatistics(
    int characterCount,                 // Unicode character count (excluding CQ codes)
    Map<String, Integer> cqCodeCounts   // Count per CQ code type (e.g., {"face": 2, "image": 1})
) {
    /**
     * Check if message has any CQ codes
     */
    public boolean hasCQCodes() {
        return !cqCodeCounts.isEmpty();
    }

    /**
     * Get total CQ code count across all types
     */
    public int totalCQCodeCount() {
        return cqCodeCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Get count for specific CQ code type (0 if not present)
     */
    public int getCount(String type) {
        return cqCodeCounts.getOrDefault(type, 0);
    }
}
```

**Attributes**:
| Field | Type | Description | Example |
|-------|------|-------------|---------|
| characterCount | int | Number of Unicode characters (excluding CQ codes) | 15 |
| cqCodeCounts | Map<String,Integer> | Count per CQ code type | {"face": 2, "image": 1} |

**Validation Rules**:
- `characterCount`: ≥0 (can be 0 for CQ-only messages)
- `cqCodeCounts`: Non-null, all values >0 (zero-count types omitted)

**Lifecycle**: Created during statistics calculation, used to format reply message, then discarded.

---

### 3. CQCodePattern (Frontend Entity - TypeScript Interface)

**Purpose**: Defines a CQ code matching pattern for rule configuration (frontend only).

**Definition**:
```typescript
/**
 * CQ Code Pattern for Rule Configuration
 */
export interface CQCodePattern {
  id: string;                    // Unique pattern ID (e.g., "contains-image")
  label: string;                 // Display label (e.g., "包含图片")
  type: CQCodeType;              // CQ code type to match
  paramFilters?: ParamFilter[];  // Optional parameter filters
  logicalOperator?: 'AND' | 'OR'; // Combine with other patterns
}

export enum CQCodeType {
  FACE = 'face',       // 表情
  IMAGE = 'image',     // 图片
  AT = 'at',           // @提及
  REPLY = 'reply',     // 回复
  RECORD = 'record',   // 语音
  VIDEO = 'video',     // 视频
  ANY = 'any'          // 任意类型
}

export interface ParamFilter {
  key: string;         // Parameter key (e.g., "id", "qq")
  operator: '=' | '!=' | 'contains' | 'regex';
  value: string;       // Filter value
}
```

**Attributes**:
| Field | Type | Description | Example |
|-------|------|-------------|---------|
| id | string | Unique pattern identifier | "contains-image" |
| label | string | Human-readable label (Chinese) | "包含图片" |
| type | CQCodeType | CQ code type to match | "image" |
| paramFilters | ParamFilter[] | Optional parameter filters | [{"key": "id", "operator": "=", "value": "123"}] |
| logicalOperator | 'AND'\|'OR' | Combine with other patterns | "AND" |

**Validation Rules**:
- `id`: Non-empty, unique within rule
- `label`: Non-empty, max 50 characters
- `type`: Valid CQCodeType enum value
- `paramFilters`: Each filter must have valid key/operator/value

**Lifecycle**: Created in frontend, sent to backend as part of rule creation, converted to regex pattern.

---

### 4. ApiCallRequest (Runtime Entity - Java Record)

**Purpose**: Represents a WebSocket JSON-RPC API call request.

**Definition**:
```java
/**
 * NapCat API Call Request (JSON-RPC 2.0 format)
 */
public record ApiCallRequest(
    String jsonrpc,           // Protocol version ("2.0")
    String id,                // Unique request ID (UUID)
    String action,            // API action name (e.g., "get_group_info")
    Map<String, Object> params // API parameters
) {
    public static ApiCallRequest create(String action, Map<String, Object> params) {
        return new ApiCallRequest("2.0", UUID.randomUUID().toString(), action, params);
    }
}

/**
 * NapCat API Call Response (JSON-RPC 2.0 format)
 */
public record ApiCallResponse(
    String jsonrpc,           // Protocol version ("2.0")
    String id,                // Request ID (matches request)
    int status,               // HTTP status code (200 = success)
    int retcode,              // OneBot return code (0 = success)
    Object data,              // Response data
    String message,           // Error message (if retcode != 0)
    long executionTimeMs      // Execution time (for monitoring)
) {
    public boolean isSuccess() {
        return retcode == 0;
    }
}
```

**Attributes (Request)**:
| Field | Type | Description | Example |
|-------|------|-------------|---------|
| jsonrpc | String | Protocol version (always "2.0") | "2.0" |
| id | String | Unique request ID (UUID) | "a1b2c3d4-..." |
| action | String | API action name | "get_group_info" |
| params | Map<String,Object> | API parameters | {"group_id": 123456} |

**Attributes (Response)**:
| Field | Type | Description | Example |
|-------|------|-------------|---------|
| jsonrpc | String | Protocol version | "2.0" |
| id | String | Request ID (matches request) | "a1b2c3d4-..." |
| status | int | HTTP status code | 200 |
| retcode | int | OneBot return code (0=success) | 0 |
| data | Object | Response data | {"group_name": "测试群"} |
| message | String | Error message (if failed) | "Group not found" |
| executionTimeMs | long | Execution time (monitoring) | 45 |

**Lifecycle**: Created for each WebSocket API call, response received asynchronously, then discarded.

---

## Data Flow Diagrams

### 1. CQ Code Parsing Flow

```
┌─────────────┐
│ Raw Message │ "Hello[CQ:face,id=123]世界[CQ:image,file=abc.jpg]"
└──────┬──────┘
       │
       ▼
┌──────────────────┐
│ CQCodeParser     │
│ .parse(message)  │
└──────┬───────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ List<CQCode>                            │
│ [                                       │
│   CQCode("face", {"id":"123"}, "[...]"), │
│   CQCode("image", {"file":"abc.jpg"}, "[...]") │
│ ]                                       │
└─────────────────────────────────────────┘
```

### 2. Statistics Calculation Flow

```
┌─────────────┐
│ Raw Message │
└──────┬──────┘
       │
       ▼
┌──────────────────────┐
│ MessageStatisticsService │
│ .calculate(message)   │
└──────┬───────────────┘
       │
       ├─→ CQCodeParser.parse()  → List<CQCode>
       │
       ├─→ CQCodeParser.stripCQCodes() → "Hello世界"
       │
       ├─→ String.codePointCount() → 7 characters
       │
       └─→ Group by type → {"face": 1, "image": 1}
       │
       ▼
┌─────────────────────┐
│ MessageStatistics   │
│ (7, {"face": 1, "image": 1}) │
└─────────────────────┘
       │
       ▼
┌──────────────────────┐
│ Format Reply Message │
│ "文字: 7字, 表情: 1个, 图片: 1张" │
└──────────────────────┘
```

### 3. WebSocket API Call Flow

```
┌─────────────────┐
│ NapCatAdapter   │
│ .callApi(action, params) │
└────────┬────────┘
         │
         ▼
┌────────────────────┐
│ ApiCallRequest     │
│ {jsonrpc, id, action, params} │
└────────┬───────────┘
         │
         ▼
┌────────────────────┐
│ WebSocket Send     │
│ (JSON-RPC format)  │
└────────┬───────────┘
         │
         ▼ (wait 10s timeout)
         │
┌────────▼───────────┐
│ WebSocket Response │
│ {jsonrpc, id, status, retcode, data} │
└────────┬───────────┘
         │
         ├─→ Success (retcode=0) → Return data
         │
         └─→ Timeout/Error → HTTP Fallback
                  │
                  ▼
           ┌──────────────┐
           │ HTTP POST    │
           │ /api/action  │
           └──────┬───────┘
                  │
                  ▼
           ┌──────────────┐
           │ HTTP Response│
           └──────────────┘
```

---

## Database Schema (No Changes)

**Note**: This feature does **NOT** introduce new database tables. It reuses existing schema:

### Existing Tables Used

**1. message_rules** (REUSE):
- Add new rules with `match_type = 'cqcode'` (new match type)
- `pattern` field stores CQ code pattern (e.g., `[CQ:image.*]`)
- `response` field stores statistics reply template

**2. group_rule_config** (REUSE):
- Link statistics rules to specific groups
- `enabled` flag controls per-group activation

**3. message_log** (REUSE):
- Log statistics replies as normal messages
- `message_content` stores formatted statistics

**No schema migrations required** - existing tables accommodate new functionality.

---

## Cache Strategy

### Caffeine (Local Cache)

**1. Compiled Pattern Cache** (REUSE existing):
```java
Cache<String, Pattern> compiledPatternsCaffeine
```
- **Key**: CQ code regex pattern string
- **Value**: Compiled `java.util.regex.Pattern` object
- **Size**: 100 entries (sufficient for CQ code patterns)
- **TTL**: 4 hours
- **Hit Rate**: Expected >95% (patterns reused heavily)

**2. Statistics Rule Cache** (NEW):
```java
Cache<Long, MessageStatistics> statisticsRuleCache
```
- **Key**: Group ID
- **Value**: Statistics rule configuration
- **Size**: 1000 entries (max 1000 groups)
- **TTL**: 10 minutes
- **Eviction**: LRU (least recently used)

### Redis (Distributed Cache)

**Rate Limiting Keys**:
```
rate_limit:statistics:{group_id} → count (expires in 5 seconds)
```
- Prevent abuse: Max 1 statistics reply per 5 seconds per group
- Uses Redis `INCR` + `EXPIRE` commands

---

## API Contracts (Generated in Phase 1)

API contracts will be generated in `contracts/` directory:

1. **cqcode-api.yaml**: CQ code parsing and pattern validation endpoints
2. **statistics-api.yaml**: Message statistics calculation and formatting endpoints

See `contracts/` directory for OpenAPI 3.0 specifications.

---

## Summary

**New Entities**: 4 runtime entities (CQCode, MessageStatistics, CQCodePattern, ApiCallRequest/Response)
**Database Changes**: None (reuse existing tables)
**Cache Strategy**: 2 Caffeine caches (compiled patterns, statistics rules) + Redis rate limiting
**Performance**: <50ms total processing time (parsing + calculation + formatting)
**Scalability**: Stateless design, horizontally scalable, cache-first architecture
