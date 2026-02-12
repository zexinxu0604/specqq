# NapCat Mock Server

Mock server for testing NapCat API integration in development environment.

## Installation

```bash
npm install
```

## Usage

### Start Server

```bash
npm start
```

The server will run on `http://localhost:3100`.

### Available Endpoints

#### 1. Get Group List

```bash
curl -X POST http://localhost:3100/get_group_list \
  -H "Content-Type: application/json"
```

Response:
```json
{
  "status": "ok",
  "retcode": 0,
  "data": {
    "groups": [
      { "group_id": 123456789, "group_name": "测试群组1", "member_count": 50 },
      { "group_id": 987654321, "group_name": "测试群组2", "member_count": 100 },
      { "group_id": 111222333, "group_name": "测试群组3", "member_count": 200 }
    ]
  }
}
```

#### 2. Get Group Info

```bash
curl -X POST http://localhost:3100/get_group_info \
  -H "Content-Type: application/json" \
  -d '{"group_id": 123456789}'
```

Response (success):
```json
{
  "status": "ok",
  "retcode": 0,
  "data": {
    "group_id": 123456789,
    "group_name": "测试群组1",
    "member_count": 50
  }
}
```

Response (not found):
```json
{
  "status": "failed",
  "retcode": 1404,
  "message": "Group not found",
  "data": null
}
```

#### 3. Simulate Group Increase Event

```bash
curl -X POST http://localhost:3100/simulate_group_increase \
  -H "Content-Type: application/json" \
  -d '{
    "group_id": 444555666,
    "group_name": "新加入的群组",
    "member_count": 30
  }'
```

Response:
```json
{
  "status": "ok",
  "message": "Group increase event simulated"
}
```

## Configuration

Update `application-dev.yml` to use the mock server:

```yaml
napcat:
  api:
    base-url: http://localhost:3100
    timeout: 10000
  websocket:
    enabled: false
```

## Development

For auto-restart on file changes:

```bash
npm run dev
```

## Notes

- This mock server is for development and testing only
- Do not use in production
- Default port: 3100 (configurable in server.js)
- Mock data is stored in memory and resets on server restart
