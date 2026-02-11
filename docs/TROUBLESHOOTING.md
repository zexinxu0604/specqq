# SpecQQ Chatbot Router - Troubleshooting Guide

**Version**: 1.0.0 | **Last Updated**: 2026-02-11

This guide provides solutions to common issues encountered when running the SpecQQ Chatbot Router system.

---

## Table of Contents

1. [Backend Startup Issues](#backend-startup-issues)
2. [Frontend Issues](#frontend-issues)
3. [Database Connection Problems](#database-connection-problems)
4. [Redis Connectivity Issues](#redis-connectivity-issues)
5. [Authentication & JWT Errors](#authentication--jwt-errors)
6. [API Request Failures](#api-request-failures)
7. [Performance Problems](#performance-problems)
8. [NapCat Integration Issues](#napcat-integration-issues)
9. [Rate Limiting (429 Errors)](#rate-limiting-429-errors)
10. [Logging and Debugging](#logging-and-debugging)

---

## Backend Startup Issues

### Issue 1: Java Version Mismatch

**Symptoms**:
```
Error: A JNI error has occurred, please check your installation and try again
Exception in thread "main" java.lang.UnsupportedClassVersionError
```

**Cause**: Wrong Java version (requires Java 17)

**Solution**:
```bash
# Check Java version
java -version
# Should show: openjdk version "17.x.x"

# If wrong version, set JAVA_HOME
export JAVA_HOME=/path/to/java-17
export PATH=$JAVA_HOME/bin:$PATH

# Or use the startup script (handles Java 17 automatically)
./start-backend.sh
```

**Verification**:
```bash
# Verify Java 17 is active
java -version | grep "17"
```

---

### Issue 2: Port 8080 Already in Use

**Symptoms**:
```
Web server failed to start. Port 8080 was already in use.
```

**Cause**: Another application is using port 8080

**Solution 1 - Kill the process**:
```bash
# Find process using port 8080
lsof -i :8080
# Output: java    12345 user   123u  IPv6 0x123456      0t0  TCP *:http-alt (LISTEN)

# Kill the process
kill -9 12345
```

**Solution 2 - Change port**:
```bash
# Edit application-dev.yml
nano src/main/resources/application-dev.yml

# Change server.port
server:
  port: 8081  # Use different port

# Restart application
./start-backend.sh
```

**Verification**:
```bash
# Check if port is now available
lsof -i :8080
# Should return empty if port is free
```

---

### Issue 3: Maven Build Fails

**Symptoms**:
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile
```

**Cause**: Missing dependencies, network issues, or corrupted Maven cache

**Solution**:
```bash
# Clean Maven cache and rebuild
rm -rf ~/.m2/repository
mvn clean install -U

# If network issues, try with verbose output
mvn clean install -X

# Skip tests if needed (not recommended)
mvn clean package -DskipTests
```

**Verification**:
```bash
# Check if JAR was created
ls -lh target/chatbot-router-*.jar
```

---

### Issue 4: Lombok Compilation Errors

**Symptoms**:
```
cannot find symbol: method builder()
cannot find symbol: method getId()
```

**Cause**: Lombok annotation processor not configured

**Solution**:
```bash
# Run Lombok diagnostic script
./diagnose-lombok-idea.sh

# Rebuild with Lombok processing
mvn clean compile -Dmaven.compiler.annotationProcessorPaths=lombok

# For IntelliJ IDEA:
# 1. Enable annotation processing: Preferences → Build → Compiler → Annotation Processors
# 2. Install Lombok plugin: Preferences → Plugins → Search "Lombok"
# 3. Rebuild project: Build → Rebuild Project
```

---

### Issue 5: Application Starts but Health Check Fails

**Symptoms**:
```bash
curl http://localhost:8080/actuator/health
# Returns: {"status":"DOWN"}
```

**Cause**: Database or Redis connection failure

**Solution**:
```bash
# Check application logs
tail -f logs/application.log

# Look for connection errors
grep -i "connection" logs/application.log

# Check database connectivity
mysql -u root -p -h localhost -e "SELECT 1"

# Check Redis connectivity
redis-cli ping
# Should return: PONG

# Verify configuration
cat src/main/resources/application-dev.yml | grep -A 5 "datasource"
```

---

## Frontend Issues

### Issue 1: npm install Fails

**Symptoms**:
```
npm ERR! code ERESOLVE
npm ERR! ERESOLVE unable to resolve dependency tree
```

**Cause**: Dependency conflicts or Node version mismatch

**Solution**:
```bash
# Check Node version (requires Node 16+)
node --version

# Clear npm cache
npm cache clean --force

# Delete node_modules and package-lock.json
cd frontend
rm -rf node_modules package-lock.json

# Reinstall with legacy peer deps
npm install --legacy-peer-deps

# Or use force flag
npm install --force
```

---

### Issue 2: Frontend Build Fails

**Symptoms**:
```
ERROR in ./src/main.ts
Module not found: Error: Can't resolve '@/components/...'
```

**Cause**: TypeScript path resolution issues

**Solution**:
```bash
# Check tsconfig.json paths configuration
cat frontend/tsconfig.json | grep -A 5 "paths"

# Rebuild TypeScript
npm run type-check

# Clean build and rebuild
rm -rf frontend/dist
npm run build
```

---

### Issue 3: CORS Errors in Browser

**Symptoms**:
```
Access to XMLHttpRequest at 'http://localhost:8080/api/...' from origin 'http://localhost:5173'
has been blocked by CORS policy
```

**Cause**: CORS not configured for frontend origin

**Solution**:
```java
// Edit SecurityConfig.java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:5173",  // Add frontend URL
        "http://localhost:3000"
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    // ... rest of configuration
}
```

**Restart backend** after making changes.

---

### Issue 4: API Calls Return 401 Unauthorized

**Symptoms**: All API requests fail with 401 status

**Cause**: JWT token expired or not sent

**Solution**:
```javascript
// Check if token exists in localStorage
console.log(localStorage.getItem('jwt_token'));

// If missing, login again
// If present but expired, refresh token or re-login

// Verify axios interceptor is configured
// In src/api/axios.ts
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

---

## Database Connection Problems

### Issue 1: MySQL Connection Refused

**Symptoms**:
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**Cause**: MySQL server not running or wrong connection settings

**Solution**:
```bash
# Check if MySQL is running
brew services list | grep mysql
# OR
sudo systemctl status mysql

# Start MySQL if not running
brew services start mysql@8.4
# OR
sudo systemctl start mysql

# Test connection
mysql -u root -p -h localhost -e "SELECT 1"

# Check connection settings in application-dev.yml
cat src/main/resources/application-dev.yml | grep -A 3 "datasource"
```

---

### Issue 2: Access Denied for User

**Symptoms**:
```
java.sql.SQLException: Access denied for user 'root'@'localhost' (using password: YES)
```

**Cause**: Wrong database credentials

**Solution**:
```bash
# Reset MySQL root password
mysql -u root
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';
FLUSH PRIVILEGES;

# Or create new user
CREATE USER 'specqq'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON chatbot_router.* TO 'specqq'@'localhost';
FLUSH PRIVILEGES;

# Update application-dev.yml with correct credentials
```

---

### Issue 3: Database Does Not Exist

**Symptoms**:
```
java.sql.SQLSyntaxErrorException: Unknown database 'chatbot_router'
```

**Cause**: Database not created

**Solution**:
```bash
# Create database
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS chatbot_router CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Verify database exists
mysql -u root -p -e "SHOW DATABASES LIKE 'chatbot_router';"

# Restart application (Flyway will create tables automatically)
./start-backend.sh
```

---

### Issue 4: Flyway Migration Fails

**Symptoms**:
```
FlywayException: Validate failed: Migration checksum mismatch
```

**Cause**: Migration script was modified after being applied

**Solution**:
```bash
# Option 1: Repair Flyway schema history
mysql -u root -p chatbot_router -e "DELETE FROM flyway_schema_history WHERE success = 0;"

# Option 2: Clean and re-migrate (DESTRUCTIVE - deletes all data)
mysql -u root -p -e "DROP DATABASE chatbot_router; CREATE DATABASE chatbot_router;"

# Restart application to re-run migrations
./start-backend.sh
```

---

## Redis Connectivity Issues

### Issue 1: Redis Connection Refused

**Symptoms**:
```
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

**Cause**: Redis server not running

**Solution**:
```bash
# Check if Redis is running
brew services list | grep redis
# OR
sudo systemctl status redis

# Start Redis
brew services start redis
# OR
sudo systemctl start redis

# Test connection
redis-cli ping
# Should return: PONG
```

---

### Issue 2: Redis Authentication Failed

**Symptoms**:
```
io.lettuce.core.RedisCommandExecutionException: NOAUTH Authentication required
```

**Cause**: Redis requires password but none provided

**Solution**:
```bash
# Check if Redis has password
redis-cli
> CONFIG GET requirepass
# If returns password, update application-dev.yml

# Update application-dev.yml
spring:
  redis:
    host: localhost
    port: 6379
    password: your_redis_password  # Add this line
```

---

## Authentication & JWT Errors

### Issue 1: JWT Token Expired

**Symptoms**:
```json
{
  "code": 401,
  "message": "Token expired",
  "data": null
}
```

**Cause**: Token lifetime exceeded (24 hours)

**Solution**:
```bash
# Frontend: Re-login to get new token
# Or implement token refresh logic

# Backend: Increase token expiration (not recommended for production)
# Edit application.yml
jwt:
  expiration: 86400000  # 24 hours in milliseconds
```

---

### Issue 2: Invalid JWT Signature

**Symptoms**:
```json
{
  "code": 401,
  "message": "Invalid JWT signature",
  "data": null
}
```

**Cause**: JWT secret changed or token tampered with

**Solution**:
```bash
# Ensure JWT secret is consistent across restarts
# Set in application.yml or environment variable

# For production, use strong secret
jwt:
  secret: ${JWT_SECRET:your-very-long-and-secure-secret-key-here}

# Clear all tokens and re-login
# Frontend: localStorage.clear()
```

---

### Issue 3: CORS Preflight Request Fails for Auth Endpoints

**Symptoms**: OPTIONS request returns 403

**Cause**: CORS not configured for authentication endpoints

**Solution**:
```java
// In SecurityConfig.java, ensure OPTIONS requests are permitted
http.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    .requestMatchers("/api/auth/**").permitAll()
    // ... rest of configuration
);
```

---

## API Request Failures

### Issue 1: 404 Not Found for API Endpoints

**Symptoms**:
```
GET http://localhost:8080/api/rules 404 (Not Found)
```

**Cause**: Wrong URL or endpoint not registered

**Solution**:
```bash
# Check if application is running
curl http://localhost:8080/actuator/health

# List all endpoints
curl http://localhost:8080/actuator/mappings | jq '.contexts.application.mappings.dispatcherServlets'

# Verify controller is registered
grep -r "@RequestMapping" src/main/java/com/specqq/chatbot/controller/
```

---

### Issue 2: 400 Bad Request with Validation Errors

**Symptoms**:
```json
{
  "code": 400,
  "message": "Validation failed",
  "errors": [
    {"field": "matchPattern", "message": "Match pattern cannot be empty"}
  ]
}
```

**Cause**: Request body validation failed

**Solution**:
- Check request body matches DTO requirements
- Ensure all required fields are provided
- Verify data types are correct
- Check field constraints (min/max length, pattern, etc.)

**Example**:
```javascript
// Correct request body for creating rule
{
  "ruleName": "Test Rule",        // Required, non-empty
  "matchType": "KEYWORD",         // Required, valid enum
  "matchPattern": "hello",        // Required, non-empty
  "replyType": "TEXT",            // Required, valid enum
  "replyContent": "Hi there!",    // Required, non-empty
  "priority": 10,                 // Required, positive integer
  "enabled": true                 // Required, boolean
}
```

---

### Issue 3: 500 Internal Server Error

**Symptoms**: API returns 500 status code

**Cause**: Unhandled exception in backend

**Solution**:
```bash
# Check application logs for stack trace
tail -f logs/application.log | grep -A 20 "ERROR"

# Common causes:
# 1. NullPointerException - Check for null values
# 2. Database constraint violation - Check foreign keys
# 3. Serialization error - Check JSON format

# Enable debug logging
# Edit application-dev.yml
logging:
  level:
    com.specqq.chatbot: DEBUG
```

---

## Performance Problems

### Issue 1: Slow API Response Times

**Symptoms**: API requests take >1 second

**Cause**: Database queries, network latency, or resource exhaustion

**Solution**:
```bash
# Check database connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# Check slow queries (if enabled)
mysql -u root -p -e "SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 10;"

# Check JVM memory
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Enable query logging
# Edit application-dev.yml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: DEBUG
```

---

### Issue 2: High Memory Usage

**Symptoms**: Application uses >2GB memory

**Cause**: Memory leaks or large object allocations

**Solution**:
```bash
# Take heap dump
jmap -dump:live,format=b,file=heap.hprof <PID>

# Analyze with Eclipse MAT or VisualVM

# Increase heap size if needed
# Edit start script or add to JAVA_OPTS
export JAVA_OPTS="-Xmx2g -Xms1g"

# Enable GC logging
export JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:file=gc.log:time,uptime:filecount=10,filesize=100M"
```

---

### Issue 3: Database Connection Pool Exhausted

**Symptoms**:
```
java.sql.SQLException: Connection is not available, request timed out after 30000ms
```

**Cause**: Too many concurrent requests or connection leaks

**Solution**:
```yaml
# Increase connection pool size
# Edit application-dev.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # Increase from default 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**Check for connection leaks**:
```bash
# Monitor active connections
watch -n 1 'curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq ".measurements[0].value"'
```

---

## NapCat Integration Issues

### Issue 1: WebSocket Connection Fails

**Symptoms**: No messages received from NapCat

**Cause**: WebSocket connection not established

**Solution**:
```bash
# Check WebSocket endpoint
curl -i -N -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" \
  -H "Sec-WebSocket-Key: test" \
  http://localhost:8080/ws/napcat

# Check NapCat configuration
# In NapCat config.json:
{
  "ws": {
    "enable": true,
    "host": "0.0.0.0",
    "port": 8080
  }
}

# Check application logs for WebSocket events
grep -i "websocket" logs/application.log
```

---

### Issue 2: Messages Not Matching Rules

**Symptoms**: Bot receives messages but doesn't reply

**Cause**: No matching rules or rules disabled

**Solution**:
```bash
# Check if rules exist
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/rules

# Check if group is enabled
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/groups

# Test rule matching
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"ruleId": 1, "testMessage": "hello"}' \
  http://localhost:8080/api/rules/test

# Check message logs
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/logs?page=1&size=10
```

---

### Issue 3: CQ Code Parsing Errors

**Symptoms**: CQ codes not recognized or parsed incorrectly

**Cause**: Invalid CQ code format or unsupported type

**Solution**:
```bash
# Test CQ code parsing
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "[CQ:face,id=123]"}' \
  http://localhost:8080/api/cqcode/parse

# Validate CQ code syntax
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cqcode": "[CQ:face,id=123]"}' \
  http://localhost:8080/api/cqcode/validate

# Check supported CQ code types
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/cqcode/types
```

---

## Rate Limiting (429 Errors)

### Issue: Too Many Requests

**Symptoms**:
```json
{
  "code": 429,
  "message": "Too many requests",
  "data": null
}
```

**Cause**: Rate limit exceeded (100 requests per 60 seconds per IP)

**Solution**:
```bash
# Wait for rate limit window to reset (60 seconds)

# Check Retry-After header
curl -I -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/rules

# For testing, temporarily increase rate limit
# Edit RateLimitAspect.java
@RateLimit(limit = 1000, windowSeconds = 60)  # Increase limit

# For production, consider:
# 1. Implementing exponential backoff
# 2. Caching responses on client side
# 3. Batching requests
# 4. Using WebSocket for real-time updates
```

---

## Logging and Debugging

### Enable Debug Logging

```yaml
# Edit application-dev.yml
logging:
  level:
    root: INFO
    com.specqq.chatbot: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

### View Logs in Real-Time

```bash
# Application logs
tail -f logs/application.log

# Error logs only
tail -f logs/application.log | grep ERROR

# Filter by component
tail -f logs/application.log | grep "RuleEngine"

# With timestamps
tail -f logs/application.log | awk '{print strftime("%Y-%m-%d %H:%M:%S"), $0}'
```

### Remote Debugging

```bash
# Start application with debug port
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar target/chatbot-router-1.0.0-SNAPSHOT.jar

# Connect from IDE (IntelliJ IDEA):
# Run → Edit Configurations → + → Remote JVM Debug
# Host: localhost, Port: 5005
```

---

## Quick Diagnostic Commands

```bash
# Check all services status
./check-services.sh

# Test database connection
mysql -u root -p -h localhost chatbot_router -e "SELECT COUNT(*) FROM message_rule;"

# Test Redis connection
redis-cli ping

# Check application health
curl http://localhost:8080/actuator/health | jq

# View recent logs
tail -n 100 logs/application.log

# Check port usage
lsof -i :8080
lsof -i :5173
lsof -i :3306
lsof -i :6379

# Monitor system resources
top -o cpu
top -o mem

# Check disk space
df -h
```

---

## Getting Further Help

If issues persist after trying these solutions:

1. **Check Documentation**:
   - README.md
   - DEPLOYMENT_GUIDE.md
   - API_EXAMPLES.md
   - USER_GUIDE.md

2. **Review Logs**:
   - Application logs: `logs/application.log`
   - Error logs: `logs/application-error.log`
   - Access logs: `logs/access.log`

3. **Report Issues**:
   - GitHub Issues: https://github.com/your-org/specqq/issues
   - Include: Error message, logs, steps to reproduce, environment details

4. **Contact Support**:
   - Email: support@example.com
   - Slack: #specqq-support

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-11
**Support**: For urgent production issues, contact on-call engineer
