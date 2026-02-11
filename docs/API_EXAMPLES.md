# SpecQQ Chatbot Router - API Usage Examples

**Version**: 1.0.0 | **Last Updated**: 2026-02-11

This document provides ready-to-use code examples for integrating with the SpecQQ Chatbot Router API in multiple programming languages.

---

## Table of Contents

1. [Authentication](#authentication)
2. [CQ Code Parsing](#cq-code-parsing)
3. [Statistics API](#statistics-api)
4. [Rule Management](#rule-management)
5. [Group Management](#group-management)
6. [Message Logs](#message-logs)
7. [Error Handling](#error-handling)
8. [Rate Limiting](#rate-limiting)
9. [Best Practices](#best-practices)

---

## Authentication

All API requests require JWT authentication. First, obtain a token by logging in.

### cURL

```bash
# Login to get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# Response:
# {
#   "code": 200,
#   "message": "success",
#   "data": {
#     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#     "tokenType": "Bearer",
#     "expiresIn": 86400
#   }
# }

# Store token in variable
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Use token in subsequent requests
curl -X GET http://localhost:8080/api/auth/user-info \
  -H "Authorization: Bearer $TOKEN"
```

### Node.js (JavaScript)

```javascript
const axios = require('axios');

const BASE_URL = 'http://localhost:8080/api';

// Login and get token
async function login(username, password) {
  try {
    const response = await axios.post(`${BASE_URL}/auth/login`, {
      username,
      password
    });

    const { accessToken, expiresIn } = response.data.data;
    console.log(`Token obtained, expires in ${expiresIn} seconds`);
    return accessToken;
  } catch (error) {
    console.error('Login failed:', error.response?.data || error.message);
    throw error;
  }
}

// Create axios instance with token
function createAuthenticatedClient(token) {
  return axios.create({
    baseURL: BASE_URL,
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
}

// Usage example
(async () => {
  const token = await login('admin', 'admin123');
  const client = createAuthenticatedClient(token);

  // Now use client for all API calls
  const userInfo = await client.get('/auth/user-info');
  console.log('User info:', userInfo.data);
})();
```

### Python

```python
import requests
from typing import Optional

BASE_URL = 'http://localhost:8080/api'

class SpecQQClient:
    def __init__(self, base_url: str = BASE_URL):
        self.base_url = base_url
        self.token: Optional[str] = None
        self.session = requests.Session()

    def login(self, username: str, password: str) -> str:
        """Login and store JWT token"""
        response = self.session.post(
            f'{self.base_url}/auth/login',
            json={'username': username, 'password': password}
        )
        response.raise_for_status()

        data = response.json()
        self.token = data['data']['accessToken']
        self.session.headers.update({
            'Authorization': f'Bearer {self.token}'
        })

        print(f"Token obtained, expires in {data['data']['expiresIn']} seconds")
        return self.token

    def get_user_info(self) -> dict:
        """Get current user information"""
        response = self.session.get(f'{self.base_url}/auth/user-info')
        response.raise_for_status()
        return response.json()['data']

# Usage example
if __name__ == '__main__':
    client = SpecQQClient()
    client.login('admin', 'admin123')

    user_info = client.get_user_info()
    print(f"Logged in as: {user_info['username']}")
```

### Java

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;

public class SpecQQClient {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private String token;

    public SpecQQClient() {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String login(String username, String password) throws IOException {
        String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
            .url(BASE_URL + "/auth/login")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Login failed: " + response);
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            this.token = root.get("data").get("accessToken").asText();

            System.out.println("Token obtained, expires in " +
                root.get("data").get("expiresIn").asInt() + " seconds");

            return this.token;
        }
    }

    public JsonNode getUserInfo() throws IOException {
        Request request = new Request.Builder()
            .url(BASE_URL + "/auth/user-info")
            .header("Authorization", "Bearer " + token)
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response);
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            return root.get("data");
        }
    }

    public static void main(String[] args) throws IOException {
        SpecQQClient client = new SpecQQClient();
        client.login("admin", "admin123");

        JsonNode userInfo = client.getUserInfo();
        System.out.println("Logged in as: " + userInfo.get("username").asText());
    }
}
```

---

## CQ Code Parsing

Parse CQ codes from QQ messages to extract structured data.

### cURL

```bash
# Parse CQ codes from message
curl -X POST http://localhost:8080/api/cqcode/parse \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "message": "Hello[CQ:face,id=123]World[CQ:image,file=test.jpg]"
  }'

# Response:
# {
#   "code": 200,
#   "message": "success",
#   "data": [
#     {
#       "type": "face",
#       "params": {"id": "123"},
#       "rawText": "[CQ:face,id=123]"
#     },
#     {
#       "type": "image",
#       "params": {"file": "test.jpg"},
#       "rawText": "[CQ:image,file=test.jpg]"
#     }
#   ]
# }

# Strip CQ codes from message
curl -X POST http://localhost:8080/api/cqcode/strip \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "message": "Hello[CQ:face,id=123]World"
  }'

# Response:
# {
#   "code": 200,
#   "message": "success",
#   "data": {
#     "strippedMessage": "HelloWorld"
#   }
# }

# Validate CQ code syntax
curl -X POST http://localhost:8080/api/cqcode/validate \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "cqcode": "[CQ:face,id=123]"
  }'

# Response:
# {
#   "code": 200,
#   "message": "success",
#   "data": {
#     "valid": true,
#     "parsed": {
#       "type": "face",
#       "params": {"id": "123"},
#       "rawText": "[CQ:face,id=123]"
#     }
#   }
# }
```

### Node.js

```javascript
// Parse CQ codes
async function parseCQCodes(client, message) {
  try {
    const response = await client.post('/cqcode/parse', { message });
    return response.data.data;
  } catch (error) {
    console.error('Parse failed:', error.response?.data || error.message);
    throw error;
  }
}

// Strip CQ codes
async function stripCQCodes(client, message) {
  try {
    const response = await client.post('/cqcode/strip', { message });
    return response.data.data.strippedMessage;
  } catch (error) {
    console.error('Strip failed:', error.response?.data || error.message);
    throw error;
  }
}

// Validate CQ code
async function validateCQCode(client, cqcode) {
  try {
    const response = await client.post('/cqcode/validate', { cqcode });
    return response.data.data;
  } catch (error) {
    console.error('Validation failed:', error.response?.data || error.message);
    throw error;
  }
}

// Usage example
(async () => {
  const token = await login('admin', 'admin123');
  const client = createAuthenticatedClient(token);

  const message = 'Hello[CQ:face,id=123]World[CQ:image,file=test.jpg]';

  // Parse CQ codes
  const cqCodes = await parseCQCodes(client, message);
  console.log('Parsed CQ codes:', cqCodes);
  // Output: [
  //   { type: 'face', params: { id: '123' }, rawText: '[CQ:face,id=123]' },
  //   { type: 'image', params: { file: 'test.jpg' }, rawText: '[CQ:image,file=test.jpg]' }
  // ]

  // Strip CQ codes
  const stripped = await stripCQCodes(client, message);
  console.log('Stripped message:', stripped);
  // Output: "HelloWorld"

  // Validate CQ code
  const validation = await validateCQCode(client, '[CQ:face,id=123]');
  console.log('Validation result:', validation);
  // Output: { valid: true, parsed: { type: 'face', params: { id: '123' }, ... } }
})();
```

### Python

```python
from typing import List, Dict, Any

class SpecQQClient:
    # ... (previous login code) ...

    def parse_cq_codes(self, message: str) -> List[Dict[str, Any]]:
        """Parse CQ codes from message"""
        response = self.session.post(
            f'{self.base_url}/cqcode/parse',
            json={'message': message}
        )
        response.raise_for_status()
        return response.json()['data']

    def strip_cq_codes(self, message: str) -> str:
        """Remove CQ codes from message"""
        response = self.session.post(
            f'{self.base_url}/cqcode/strip',
            json={'message': message}
        )
        response.raise_for_status()
        return response.json()['data']['strippedMessage']

    def validate_cq_code(self, cqcode: str) -> Dict[str, Any]:
        """Validate CQ code syntax"""
        response = self.session.post(
            f'{self.base_url}/cqcode/validate',
            json={'cqcode': cqcode}
        )
        response.raise_for_status()
        return response.json()['data']

# Usage example
if __name__ == '__main__':
    client = SpecQQClient()
    client.login('admin', 'admin123')

    message = 'Hello[CQ:face,id=123]World[CQ:image,file=test.jpg]'

    # Parse CQ codes
    cq_codes = client.parse_cq_codes(message)
    print(f"Parsed {len(cq_codes)} CQ codes:")
    for cq in cq_codes:
        print(f"  - Type: {cq['type']}, Params: {cq['params']}")

    # Strip CQ codes
    stripped = client.strip_cq_codes(message)
    print(f"Stripped message: {stripped}")

    # Validate CQ code
    validation = client.validate_cq_code('[CQ:face,id=123]')
    print(f"Valid: {validation['valid']}")
```

### Java

```java
import java.util.List;
import java.util.Map;

public class SpecQQClient {
    // ... (previous login code) ...

    public List<Map<String, Object>> parseCQCodes(String message) throws IOException {
        String json = String.format("{\"message\":\"%s\"}", escapeJson(message));
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
            .url(BASE_URL + "/cqcode/parse")
            .header("Authorization", "Bearer " + token)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Parse failed: " + response);
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            return objectMapper.convertValue(
                root.get("data"),
                new TypeReference<List<Map<String, Object>>>() {}
            );
        }
    }

    public String stripCQCodes(String message) throws IOException {
        String json = String.format("{\"message\":\"%s\"}", escapeJson(message));
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
            .url(BASE_URL + "/cqcode/strip")
            .header("Authorization", "Bearer " + token)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Strip failed: " + response);
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            return root.get("data").get("strippedMessage").asText();
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n");
    }

    public static void main(String[] args) throws IOException {
        SpecQQClient client = new SpecQQClient();
        client.login("admin", "admin123");

        String message = "Hello[CQ:face,id=123]World[CQ:image,file=test.jpg]";

        // Parse CQ codes
        List<Map<String, Object>> cqCodes = client.parseCQCodes(message);
        System.out.println("Parsed " + cqCodes.size() + " CQ codes:");
        cqCodes.forEach(cq -> System.out.println("  - " + cq));

        // Strip CQ codes
        String stripped = client.stripCQCodes(message);
        System.out.println("Stripped message: " + stripped);
    }
}
```

---

## Statistics API

Calculate message statistics and CQ code distribution.

### cURL

```bash
# Calculate statistics
curl -X POST http://localhost:8080/api/statistics/calculate \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "message": "Hello[CQ:face,id=123]World[CQ:image,file=test.jpg][CQ:face,id=456]"
  }'

# Response:
# {
#   "code": 200,
#   "message": "success",
#   "data": {
#     "totalCount": 3,
#     "countByType": {
#       "face": 2,
#       "image": 1
#     },
#     "uniqueTypes": ["face", "image"]
#   }
# }

# Calculate and format with Chinese labels
curl -X POST http://localhost:8080/api/statistics/calculate-and-format \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "message": "Hello[CQ:face,id=123]World[CQ:image,file=test.jpg]"
  }'

# Response:
# {
#   "code": 200,
#   "message": "success",
#   "data": {
#     "formatted": "表情×2, 图片×1",
#     "totalCount": 3,
#     "countByType": {
#       "face": 2,
#       "image": 1
#     }
#   }
# }
```

### Node.js

```javascript
// Calculate statistics
async function calculateStatistics(client, message) {
  try {
    const response = await client.post('/statistics/calculate', { message });
    return response.data.data;
  } catch (error) {
    console.error('Statistics calculation failed:', error.response?.data || error.message);
    throw error;
  }
}

// Calculate and format with Chinese labels
async function calculateAndFormatStatistics(client, message) {
  try {
    const response = await client.post('/statistics/calculate-and-format', { message });
    return response.data.data;
  } catch (error) {
    console.error('Statistics formatting failed:', error.response?.data || error.message);
    throw error;
  }
}

// Usage example
(async () => {
  const token = await login('admin', 'admin123');
  const client = createAuthenticatedClient(token);

  const message = 'Hello[CQ:face,id=123]World[CQ:image,file=test.jpg][CQ:face,id=456]';

  // Calculate statistics
  const stats = await calculateStatistics(client, message);
  console.log('Statistics:', stats);
  // Output: {
  //   totalCount: 3,
  //   countByType: { face: 2, image: 1 },
  //   uniqueTypes: ['face', 'image']
  // }

  // Formatted statistics
  const formatted = await calculateAndFormatStatistics(client, message);
  console.log('Formatted:', formatted.formatted);
  // Output: "表情×2, 图片×1"
})();
```

### Python

```python
class SpecQQClient:
    # ... (previous code) ...

    def calculate_statistics(self, message: str) -> Dict[str, Any]:
        """Calculate CQ code statistics"""
        response = self.session.post(
            f'{self.base_url}/statistics/calculate',
            json={'message': message}
        )
        response.raise_for_status()
        return response.json()['data']

    def calculate_and_format_statistics(self, message: str) -> Dict[str, Any]:
        """Calculate and format statistics with Chinese labels"""
        response = self.session.post(
            f'{self.base_url}/statistics/calculate-and-format',
            json={'message': message}
        )
        response.raise_for_status()
        return response.json()['data']

# Usage example
if __name__ == '__main__':
    client = SpecQQClient()
    client.login('admin', 'admin123')

    message = 'Hello[CQ:face,id=123]World[CQ:image,file=test.jpg][CQ:face,id=456]'

    # Calculate statistics
    stats = client.calculate_statistics(message)
    print(f"Total CQ codes: {stats['totalCount']}")
    print(f"By type: {stats['countByType']}")

    # Formatted statistics
    formatted = client.calculate_and_format_statistics(message)
    print(f"Formatted: {formatted['formatted']}")
```

---

## Rule Management

Create, update, and manage message routing rules.

### cURL

```bash
# List all rules (paginated)
curl -X GET "http://localhost:8080/api/rules?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Create new rule
curl -X POST http://localhost:8080/api/rules \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "ruleName": "Welcome Message",
    "description": "Greet new members",
    "matchType": "KEYWORD",
    "matchPattern": "hello",
    "replyType": "TEXT",
    "replyContent": "Hello {user}! Welcome to {group}!",
    "priority": 10,
    "enabled": true
  }'

# Update existing rule
curl -X PUT http://localhost:8080/api/rules/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "ruleName": "Welcome Message (Updated)",
    "description": "Greet new members with emoji",
    "matchType": "KEYWORD",
    "matchPattern": "hello",
    "replyType": "TEXT",
    "replyContent": "Hello {user}! 👋 Welcome to {group}!",
    "priority": 10,
    "enabled": true
  }'

# Delete rule
curl -X DELETE http://localhost:8080/api/rules/1 \
  -H "Authorization: Bearer $TOKEN"

# Toggle rule status
curl -X PUT http://localhost:8080/api/rules/1/toggle \
  -H "Authorization: Bearer $TOKEN"

# Test rule matching
curl -X POST http://localhost:8080/api/rules/test \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "ruleId": 1,
    "testMessage": "hello world"
  }'
```

### Node.js

```javascript
// List rules with pagination
async function listRules(client, page = 1, size = 10, filters = {}) {
  try {
    const params = new URLSearchParams({ page, size, ...filters });
    const response = await client.get(`/rules?${params}`);
    return response.data.data;
  } catch (error) {
    console.error('List rules failed:', error.response?.data || error.message);
    throw error;
  }
}

// Create new rule
async function createRule(client, ruleData) {
  try {
    const response = await client.post('/rules', ruleData);
    return response.data.data;
  } catch (error) {
    console.error('Create rule failed:', error.response?.data || error.message);
    throw error;
  }
}

// Update rule
async function updateRule(client, ruleId, ruleData) {
  try {
    const response = await client.put(`/rules/${ruleId}`, ruleData);
    return response.data.data;
  } catch (error) {
    console.error('Update rule failed:', error.response?.data || error.message);
    throw error;
  }
}

// Delete rule
async function deleteRule(client, ruleId) {
  try {
    const response = await client.delete(`/rules/${ruleId}`);
    return response.data.message;
  } catch (error) {
    console.error('Delete rule failed:', error.response?.data || error.message);
    throw error;
  }
}

// Test rule
async function testRule(client, ruleId, testMessage) {
  try {
    const response = await client.post('/rules/test', { ruleId, testMessage });
    return response.data.data;
  } catch (error) {
    console.error('Test rule failed:', error.response?.data || error.message);
    throw error;
  }
}

// Usage example
(async () => {
  const token = await login('admin', 'admin123');
  const client = createAuthenticatedClient(token);

  // Create rule
  const newRule = await createRule(client, {
    ruleName: 'Welcome Message',
    description: 'Greet new members',
    matchType: 'KEYWORD',
    matchPattern: 'hello',
    replyType: 'TEXT',
    replyContent: 'Hello {user}! Welcome to {group}!',
    priority: 10,
    enabled: true
  });
  console.log('Created rule:', newRule);

  // Test rule
  const testResult = await testRule(client, newRule.id, 'hello world');
  console.log('Test result:', testResult);
  // Output: { matched: true, reply: 'Hello user! Welcome to group!' }
})();
```

### Python

```python
from typing import Optional

class SpecQQClient:
    # ... (previous code) ...

    def list_rules(self, page: int = 1, size: int = 10, **filters) -> Dict[str, Any]:
        """List rules with pagination and filters"""
        params = {'page': page, 'size': size, **filters}
        response = self.session.get(
            f'{self.base_url}/rules',
            params=params
        )
        response.raise_for_status()
        return response.json()['data']

    def create_rule(self, rule_data: Dict[str, Any]) -> Dict[str, Any]:
        """Create new rule"""
        response = self.session.post(
            f'{self.base_url}/rules',
            json=rule_data
        )
        response.raise_for_status()
        return response.json()['data']

    def update_rule(self, rule_id: int, rule_data: Dict[str, Any]) -> Dict[str, Any]:
        """Update existing rule"""
        response = self.session.put(
            f'{self.base_url}/rules/{rule_id}',
            json=rule_data
        )
        response.raise_for_status()
        return response.json()['data']

    def delete_rule(self, rule_id: int) -> str:
        """Delete rule"""
        response = self.session.delete(f'{self.base_url}/rules/{rule_id}')
        response.raise_for_status()
        return response.json()['message']

    def test_rule(self, rule_id: int, test_message: str) -> Dict[str, Any]:
        """Test rule matching"""
        response = self.session.post(
            f'{self.base_url}/rules/test',
            json={'ruleId': rule_id, 'testMessage': test_message}
        )
        response.raise_for_status()
        return response.json()['data']

# Usage example
if __name__ == '__main__':
    client = SpecQQClient()
    client.login('admin', 'admin123')

    # Create rule
    new_rule = client.create_rule({
        'ruleName': 'Welcome Message',
        'description': 'Greet new members',
        'matchType': 'KEYWORD',
        'matchPattern': 'hello',
        'replyType': 'TEXT',
        'replyContent': 'Hello {user}! Welcome to {group}!',
        'priority': 10,
        'enabled': True
    })
    print(f"Created rule: {new_rule['ruleName']} (ID: {new_rule['id']})")

    # Test rule
    test_result = client.test_rule(new_rule['id'], 'hello world')
    print(f"Test matched: {test_result['matched']}")
```

---

## Group Management

Manage QQ groups and rule bindings.

### cURL

```bash
# List all groups
curl -X GET http://localhost:8080/api/groups \
  -H "Authorization: Bearer $TOKEN"

# Get group details
curl -X GET http://localhost:8080/api/groups/1 \
  -H "Authorization: Bearer $TOKEN"

# Toggle group status
curl -X PUT http://localhost:8080/api/groups/1/toggle \
  -H "Authorization: Bearer $TOKEN"

# Get group's rules
curl -X GET http://localhost:8080/api/groups/1/rules \
  -H "Authorization: Bearer $TOKEN"

# Bind rule to group
curl -X POST http://localhost:8080/api/groups/1/rules \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "ruleId": 5
  }'

# Unbind rule from group
curl -X DELETE http://localhost:8080/api/groups/1/rules/5 \
  -H "Authorization: Bearer $TOKEN"
```

### Node.js

```javascript
// List all groups
async function listGroups(client) {
  const response = await client.get('/groups');
  return response.data.data;
}

// Get group details
async function getGroup(client, groupId) {
  const response = await client.get(`/groups/${groupId}`);
  return response.data.data;
}

// Bind rule to group
async function bindRuleToGroup(client, groupId, ruleId) {
  const response = await client.post(`/groups/${groupId}/rules`, { ruleId });
  return response.data.message;
}

// Unbind rule from group
async function unbindRuleFromGroup(client, groupId, ruleId) {
  const response = await client.delete(`/groups/${groupId}/rules/${ruleId}`);
  return response.data.message;
}

// Usage example
(async () => {
  const token = await login('admin', 'admin123');
  const client = createAuthenticatedClient(token);

  // List groups
  const groups = await listGroups(client);
  console.log(`Found ${groups.length} groups`);

  // Bind rule to group
  await bindRuleToGroup(client, 1, 5);
  console.log('Rule bound successfully');
})();
```

---

## Message Logs

Query and export message logs.

### cURL

```bash
# List message logs (paginated)
curl -X GET "http://localhost:8080/api/logs?page=1&size=20&groupId=1" \
  -H "Authorization: Bearer $TOKEN"

# Get log details
curl -X GET http://localhost:8080/api/logs/123 \
  -H "Authorization: Bearer $TOKEN"

# Export logs as CSV
curl -X GET "http://localhost:8080/api/logs/export?startDate=2026-02-01&endDate=2026-02-11" \
  -H "Authorization: Bearer $TOKEN" \
  -o logs.csv

# Get statistics
curl -X GET "http://localhost:8080/api/logs/stats?startDate=2026-02-01&endDate=2026-02-11" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Error Handling

All APIs return consistent error responses.

### Error Response Format

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": null,
  "errors": [
    {
      "field": "matchPattern",
      "message": "Match pattern cannot be empty"
    }
  ]
}
```

### Common Error Codes

- **400**: Bad Request (invalid input)
- **401**: Unauthorized (missing or invalid token)
- **403**: Forbidden (insufficient permissions)
- **404**: Not Found (resource doesn't exist)
- **429**: Too Many Requests (rate limit exceeded)
- **500**: Internal Server Error

### Node.js Error Handling

```javascript
async function safeApiCall(apiFunction, ...args) {
  try {
    return await apiFunction(...args);
  } catch (error) {
    if (error.response) {
      // Server responded with error status
      const { code, message, errors } = error.response.data;
      console.error(`API Error ${code}: ${message}`);
      if (errors) {
        errors.forEach(err => console.error(`  - ${err.field}: ${err.message}`));
      }
    } else if (error.request) {
      // Request made but no response
      console.error('No response from server');
    } else {
      // Other errors
      console.error('Request error:', error.message);
    }
    throw error;
  }
}
```

### Python Error Handling

```python
from requests.exceptions import HTTPError, ConnectionError, Timeout

def safe_api_call(func, *args, **kwargs):
    try:
        return func(*args, **kwargs)
    except HTTPError as e:
        response = e.response
        data = response.json()
        print(f"API Error {data['code']}: {data['message']}")
        if 'errors' in data:
            for err in data['errors']:
                print(f"  - {err['field']}: {err['message']}")
        raise
    except ConnectionError:
        print("Connection error: Unable to reach server")
        raise
    except Timeout:
        print("Request timeout: Server did not respond in time")
        raise
```

---

## Rate Limiting

The API implements rate limiting to prevent abuse.

### Rate Limit Headers

Response headers include rate limit information:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1707648000
```

### Default Limits

- **100 requests per 60 seconds** per IP address
- **429 Too Many Requests** response when limit exceeded
- **Retry-After** header indicates when to retry (in seconds)

### Handling Rate Limits

```javascript
async function apiCallWithRetry(apiFunction, ...args) {
  const MAX_RETRIES = 3;
  let retries = 0;

  while (retries < MAX_RETRIES) {
    try {
      return await apiFunction(...args);
    } catch (error) {
      if (error.response?.status === 429) {
        const retryAfter = parseInt(error.response.headers['retry-after'] || '60');
        console.log(`Rate limited. Retrying after ${retryAfter} seconds...`);
        await new Promise(resolve => setTimeout(resolve, retryAfter * 1000));
        retries++;
      } else {
        throw error;
      }
    }
  }

  throw new Error('Max retries exceeded');
}
```

---

## Best Practices

### 1. Token Management

- Store tokens securely (never in source code)
- Refresh tokens before expiration (24-hour lifetime)
- Handle 401 errors by re-authenticating
- Use environment variables for credentials

### 2. Connection Pooling

```javascript
// Node.js: Reuse axios instance
const client = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  maxRedirects: 5,
  httpAgent: new http.Agent({ keepAlive: true }),
  httpsAgent: new https.Agent({ keepAlive: true })
});
```

```python
# Python: Reuse session
session = requests.Session()
adapter = HTTPAdapter(
    pool_connections=10,
    pool_maxsize=20,
    max_retries=3
)
session.mount('http://', adapter)
session.mount('https://', adapter)
```

### 3. Pagination

Always paginate large result sets:

```javascript
async function getAllRules(client) {
  let page = 1;
  let allRules = [];
  let hasMore = true;

  while (hasMore) {
    const response = await client.get(`/rules?page=${page}&size=100`);
    const { items, totalPages } = response.data.data;

    allRules = allRules.concat(items);
    hasMore = page < totalPages;
    page++;
  }

  return allRules;
}
```

### 4. Timeout Configuration

Set appropriate timeouts:

```javascript
const client = axios.create({
  baseURL: BASE_URL,
  timeout: 30000  // 30 seconds
});
```

### 5. Logging

Log API calls for debugging:

```javascript
client.interceptors.request.use(request => {
  console.log('Request:', request.method.toUpperCase(), request.url);
  return request;
});

client.interceptors.response.use(
  response => {
    console.log('Response:', response.status, response.config.url);
    return response;
  },
  error => {
    console.error('Error:', error.response?.status, error.config?.url);
    return Promise.reject(error);
  }
);
```

---

## Complete Example: Rule Creation Workflow

```javascript
const axios = require('axios');

const BASE_URL = 'http://localhost:8080/api';

async function completeRuleWorkflow() {
  try {
    // 1. Login
    const loginResponse = await axios.post(`${BASE_URL}/auth/login`, {
      username: 'admin',
      password: 'admin123'
    });
    const token = loginResponse.data.data.accessToken;

    // 2. Create authenticated client
    const client = axios.create({
      baseURL: BASE_URL,
      headers: { 'Authorization': `Bearer ${token}` }
    });

    // 3. Create rule
    const ruleResponse = await client.post('/rules', {
      ruleName: 'Face Emoji Detector',
      description: 'Detect and respond to face emojis',
      matchType: 'REGEX',
      matchPattern: '\\[CQ:face,id=(\\d+)\\]',
      replyType: 'TEXT',
      replyContent: 'I see you sent emoji {$1}!',
      priority: 50,
      enabled: true
    });
    const ruleId = ruleResponse.data.data.id;
    console.log(`Created rule: ${ruleId}`);

    // 4. Test rule
    const testResponse = await client.post('/rules/test', {
      ruleId,
      testMessage: 'Hello [CQ:face,id=123] there!'
    });
    console.log('Test result:', testResponse.data.data);

    // 5. Bind to group
    await client.post('/groups/1/rules', { ruleId });
    console.log('Rule bound to group 1');

    // 6. Verify binding
    const groupRulesResponse = await client.get('/groups/1/rules');
    const boundRules = groupRulesResponse.data.data;
    console.log(`Group has ${boundRules.length} rules`);

    console.log('Workflow completed successfully!');

  } catch (error) {
    console.error('Workflow failed:', error.response?.data || error.message);
  }
}

completeRuleWorkflow();
```

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-11
**API Version**: v1
**Base URL (Development)**: http://localhost:8080/api
**Base URL (Production)**: Update with your production URL
