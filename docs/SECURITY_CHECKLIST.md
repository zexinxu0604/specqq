# SpecQQ Chatbot Router - Security Hardening Checklist

**Version**: 1.0.0 | **Last Updated**: 2026-02-11

This checklist ensures your SpecQQ Chatbot Router deployment follows security best practices for production environments.

---

## Table of Contents

1. [Pre-Deployment Security](#pre-deployment-security)
2. [Authentication & Authorization](#authentication--authorization)
3. [Network Security](#network-security)
4. [Database Security](#database-security)
5. [Redis Security](#redis-security)
6. [Application Security](#application-security)
7. [Infrastructure Security](#infrastructure-security)
8. [Monitoring & Logging](#monitoring--logging)
9. [Compliance & Auditing](#compliance--auditing)
10. [Security Maintenance](#security-maintenance)

---

## Pre-Deployment Security

### Critical First Steps

- [ ] **Change Default Credentials**
  ```bash
  # Default admin credentials MUST be changed
  # Current: admin/admin123
  # Update via API or database:
  curl -X POST http://localhost:8080/api/auth/change-password \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"oldPassword": "admin123", "newPassword": "NewSecurePassword123!"}'
  ```

- [ ] **Generate Strong JWT Secret**
  ```bash
  # Generate 256-bit random secret
  openssl rand -base64 32

  # Update application.yml or set environment variable
  export JWT_SECRET="your-generated-secret-here"
  ```

- [ ] **Review Default Configuration**
  - Check all `application-*.yml` files
  - Remove or secure any debug endpoints
  - Disable unnecessary actuator endpoints

- [ ] **Scan for Hardcoded Secrets**
  ```bash
  # Use git-secrets or similar tools
  git secrets --scan

  # Manual check
  grep -r "password\|secret\|token" src/ --include="*.java" --include="*.yml"
  ```

---

## Authentication & Authorization

### JWT Configuration

- [ ] **Use Strong JWT Secret**
  - Minimum 256 bits (32 bytes)
  - Randomly generated
  - Stored in environment variable, not in code
  - Rotated regularly (every 90 days)

  ```yaml
  # application-prod.yml
  jwt:
    secret: ${JWT_SECRET}  # NEVER hardcode
    expiration: 86400000   # 24 hours
  ```

- [ ] **Configure Appropriate Token Expiration**
  - Access token: 15-60 minutes (production)
  - Refresh token: 7-30 days
  - Consider implementing token refresh mechanism

  ```yaml
  jwt:
    expiration: 3600000        # 1 hour for production
    refresh-expiration: 604800000  # 7 days
  ```

- [ ] **Implement Token Blacklist**
  - Verify Redis-based token blacklist is enabled
  - Set appropriate TTL for blacklisted tokens

  ```yaml
  spring:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}  # Required for production
  ```

- [ ] **Enable HTTPS for Token Transmission**
  - Never send JWT over unencrypted HTTP in production
  - Configure TLS/SSL certificates

  ```yaml
  server:
    ssl:
      enabled: true
      key-store: classpath:keystore.p12
      key-store-password: ${KEYSTORE_PASSWORD}
      key-store-type: PKCS12
  ```

### Password Security

- [ ] **Enforce Strong Password Policy**
  ```java
  // Minimum requirements:
  // - At least 12 characters
  // - Mix of uppercase, lowercase, numbers, special characters
  // - Not in common password dictionaries

  @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$",
           message = "Password must be at least 12 characters with uppercase, lowercase, number, and special character")
  private String password;
  ```

- [ ] **Use BCrypt with Sufficient Rounds**
  ```java
  // SecurityConfig.java
  @Bean
  public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder(12);  // 12 rounds minimum
  }
  ```

- [ ] **Implement Account Lockout**
  - Lock account after 5 failed login attempts
  - Implement CAPTCHA after 3 failed attempts
  - Unlock after 30 minutes or admin intervention

### Authorization

- [ ] **Implement Role-Based Access Control (RBAC)**
  ```java
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> deleteRule(@PathVariable Long id) {
      // Only admins can delete rules
  }
  ```

- [ ] **Validate User Permissions on Every Request**
  - Never trust client-side authorization
  - Verify permissions in backend for every operation

- [ ] **Implement Principle of Least Privilege**
  - Grant minimum necessary permissions
  - Separate read and write permissions
  - Create role hierarchy (USER < MODERATOR < ADMIN)

---

## Network Security

### CORS Configuration

- [ ] **Restrict CORS Origins**
  ```java
  // SecurityConfig.java - DO NOT use "*" in production
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
      CorsConfiguration configuration = new CorsConfiguration();
      configuration.setAllowedOrigins(Arrays.asList(
          "https://yourdomain.com",
          "https://admin.yourdomain.com"
      ));
      configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
      configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
      configuration.setAllowCredentials(true);
      configuration.setMaxAge(3600L);

      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/api/**", configuration);
      return source;
  }
  ```

- [ ] **Validate Origin Headers**
  - Check `Origin` and `Referer` headers
  - Reject requests from unexpected origins

### HTTPS/TLS

- [ ] **Enable HTTPS for All Traffic**
  ```yaml
  server:
    ssl:
      enabled: true
      protocol: TLS
      enabled-protocols: TLSv1.3,TLSv1.2  # Disable TLSv1.0 and TLSv1.1
      ciphers: TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384  # Strong ciphers only
  ```

- [ ] **Use Valid SSL Certificates**
  - Obtain certificates from trusted CA (Let's Encrypt, DigiCert, etc.)
  - Never use self-signed certificates in production
  - Set up automatic certificate renewal

  ```bash
  # Using Let's Encrypt with Certbot
  sudo certbot certonly --standalone -d yourdomain.com
  ```

- [ ] **Implement HTTP Strict Transport Security (HSTS)**
  ```java
  // SecurityConfig.java
  http.headers()
      .httpStrictTransportSecurity()
      .includeSubDomains(true)
      .maxAgeInSeconds(31536000);  // 1 year
  ```

- [ ] **Redirect HTTP to HTTPS**
  ```yaml
  server:
    port: 443
  http:
    port: 80
    redirect-to-https: true
  ```

### Firewall Rules

- [ ] **Configure Firewall to Allow Only Necessary Ports**
  ```bash
  # Using UFW (Ubuntu)
  sudo ufw default deny incoming
  sudo ufw default allow outgoing
  sudo ufw allow 443/tcp    # HTTPS
  sudo ufw allow 22/tcp     # SSH (restrict to specific IPs)
  sudo ufw enable

  # Using iptables
  iptables -A INPUT -p tcp --dport 443 -j ACCEPT
  iptables -A INPUT -p tcp --dport 22 -j ACCEPT
  iptables -A INPUT -j DROP
  ```

- [ ] **Restrict SSH Access**
  ```bash
  # Edit /etc/ssh/sshd_config
  PermitRootLogin no
  PasswordAuthentication no
  PubkeyAuthentication yes
  AllowUsers admin@specific-ip
  ```

- [ ] **Use Network Segmentation**
  - Place database and Redis in private subnet
  - Only application server should access database
  - Use security groups or network ACLs

---

## Database Security

### MySQL Security

- [ ] **Change Default Root Password**
  ```sql
  ALTER USER 'root'@'localhost' IDENTIFIED BY 'StrongPassword123!';
  FLUSH PRIVILEGES;
  ```

- [ ] **Create Application-Specific User**
  ```sql
  CREATE USER 'specqq'@'localhost' IDENTIFIED BY 'StrongAppPassword123!';
  GRANT SELECT, INSERT, UPDATE, DELETE ON chatbot_router.* TO 'specqq'@'localhost';
  FLUSH PRIVILEGES;

  -- Revoke unnecessary privileges
  REVOKE CREATE, DROP, ALTER ON chatbot_router.* FROM 'specqq'@'localhost';
  ```

- [ ] **Disable Remote Root Access**
  ```sql
  DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');
  FLUSH PRIVILEGES;
  ```

- [ ] **Enable SSL/TLS for Database Connections**
  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/chatbot_router?useSSL=true&requireSSL=true
  ```

- [ ] **Configure Database Firewall**
  ```bash
  # Only allow connections from application server
  sudo ufw allow from 10.0.1.100 to any port 3306
  ```

- [ ] **Enable Audit Logging**
  ```sql
  -- Enable general query log (for audit purposes)
  SET GLOBAL general_log = 'ON';
  SET GLOBAL log_output = 'TABLE';

  -- Enable slow query log
  SET GLOBAL slow_query_log = 'ON';
  SET GLOBAL long_query_time = 2;  # Log queries taking >2 seconds
  ```

- [ ] **Encrypt Data at Rest**
  ```sql
  -- Enable InnoDB encryption
  SET GLOBAL innodb_encrypt_tables = ON;
  ```

- [ ] **Regular Backups**
  ```bash
  # Automated daily backups
  mysqldump -u root -p --all-databases --single-transaction > backup-$(date +%Y%m%d).sql

  # Encrypt backups
  gpg --encrypt backup-$(date +%Y%m%d).sql

  # Store backups offsite (S3, etc.)
  aws s3 cp backup-$(date +%Y%m%d).sql.gpg s3://your-backup-bucket/
  ```

---

## Redis Security

### Redis Hardening

- [ ] **Set Strong Password**
  ```bash
  # Edit /etc/redis/redis.conf
  requirepass YourStrongRedisPassword123!

  # Restart Redis
  sudo systemctl restart redis
  ```

- [ ] **Bind to Localhost Only**
  ```bash
  # Edit /etc/redis/redis.conf
  bind 127.0.0.1 ::1

  # Or bind to specific private IP
  bind 10.0.1.100
  ```

- [ ] **Disable Dangerous Commands**
  ```bash
  # Edit /etc/redis/redis.conf
  rename-command FLUSHDB ""
  rename-command FLUSHALL ""
  rename-command KEYS ""
  rename-command CONFIG "CONFIG-SECRET-NAME"
  ```

- [ ] **Enable Protected Mode**
  ```bash
  # Edit /etc/redis/redis.conf
  protected-mode yes
  ```

- [ ] **Configure Firewall for Redis**
  ```bash
  # Only allow connections from application server
  sudo ufw allow from 10.0.1.100 to any port 6379
  ```

- [ ] **Enable TLS/SSL**
  ```bash
  # Edit /etc/redis/redis.conf
  tls-port 6380
  port 0  # Disable non-TLS port
  tls-cert-file /path/to/redis.crt
  tls-key-file /path/to/redis.key
  tls-ca-cert-file /path/to/ca.crt
  ```

---

## Application Security

### Input Validation

- [ ] **Validate All User Input**
  ```java
  // Use @Valid and @Validated annotations
  @PostMapping("/rules")
  public ResponseEntity<?> createRule(@Valid @RequestBody RuleCreateDTO dto) {
      // Validation happens automatically
  }

  // Custom validators for complex rules
  @AssertTrue(message = "Invalid regex pattern")
  private boolean isValidRegex() {
      if (matchType == MatchType.REGEX) {
          try {
              Pattern.compile(matchPattern);
              return true;
          } catch (PatternSyntaxException e) {
              return false;
          }
      }
      return true;
  }
  ```

- [ ] **Sanitize Output (XSS Prevention)**
  ```java
  // Use OWASP Java Encoder
  import org.owasp.encoder.Encode;

  String safe = Encode.forHtml(userInput);
  String safeJs = Encode.forJavaScript(userInput);
  ```

- [ ] **Prevent SQL Injection**
  ```java
  // MyBatis uses parameterized queries by default
  // ALWAYS use #{} syntax, NEVER ${}

  // GOOD (parameterized)
  @Select("SELECT * FROM message_rule WHERE rule_name = #{ruleName}")
  MessageRule findByName(@Param("ruleName") String ruleName);

  // BAD (vulnerable to SQL injection)
  // @Select("SELECT * FROM message_rule WHERE rule_name = '${ruleName}'")
  ```

### Rate Limiting

- [ ] **Implement Rate Limiting on All Endpoints**
  ```java
  @RateLimit(limit = 100, windowSeconds = 60)
  @GetMapping("/rules")
  public ResponseEntity<?> listRules() {
      // Rate limited to 100 requests per minute per IP
  }
  ```

- [ ] **Configure Different Limits for Different Endpoints**
  ```java
  // Stricter limit for expensive operations
  @RateLimit(limit = 10, windowSeconds = 60)
  @PostMapping("/rules")
  public ResponseEntity<?> createRule() { }

  // More lenient for read operations
  @RateLimit(limit = 300, windowSeconds = 60)
  @GetMapping("/rules")
  public ResponseEntity<?> listRules() { }
  ```

### Security Headers

- [ ] **Configure Security Headers**
  ```java
  // SecurityConfig.java
  http.headers()
      .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
      .and()
      .xssProtection()
      .and()
      .contentTypeOptions()
      .and()
      .frameOptions().deny()
      .and()
      .referrerPolicy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
  ```

### Error Handling

- [ ] **Never Expose Stack Traces to Users**
  ```java
  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleException(Exception e) {
      // Log full error internally
      log.error("Internal error", e);

      // Return generic message to user
      return ResponseEntity.status(500)
          .body(Result.error(ResultCode.INTERNAL_ERROR, "An error occurred"));
  }
  ```

- [ ] **Implement Custom Error Pages**
  - Don't show default Spring Boot error pages in production
  - Create custom 404, 500 error pages

### Dependency Security

- [ ] **Keep Dependencies Up to Date**
  ```bash
  # Check for outdated dependencies
  mvn versions:display-dependency-updates

  # Check for security vulnerabilities
  mvn org.owasp:dependency-check-maven:check
  ```

- [ ] **Use Dependency Scanning Tools**
  - Snyk
  - OWASP Dependency-Check
  - GitHub Dependabot

- [ ] **Pin Dependency Versions**
  ```xml
  <!-- Don't use version ranges in production -->
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
      <version>3.1.8</version>  <!-- Exact version, not 3.1.+ -->
  </dependency>
  ```

---

## Infrastructure Security

### Server Hardening

- [ ] **Keep Operating System Updated**
  ```bash
  # Ubuntu/Debian
  sudo apt update && sudo apt upgrade -y

  # Enable automatic security updates
  sudo apt install unattended-upgrades
  sudo dpkg-reconfigure --priority=low unattended-upgrades
  ```

- [ ] **Disable Unnecessary Services**
  ```bash
  # List all services
  systemctl list-unit-files --type=service

  # Disable unnecessary services
  sudo systemctl disable telnet
  sudo systemctl disable ftp
  ```

- [ ] **Configure File Permissions**
  ```bash
  # Application files
  sudo chown -R specqq:specqq /opt/specqq
  sudo chmod 750 /opt/specqq

  # Configuration files (sensitive)
  sudo chmod 600 /opt/specqq/config/application-prod.yml

  # Log files
  sudo chmod 640 /var/log/specqq/*.log
  ```

### Docker Security (if using Docker)

- [ ] **Run Containers as Non-Root User**
  ```dockerfile
  FROM openjdk:17-jdk-slim

  # Create non-root user
  RUN useradd -m -u 1000 specqq
  USER specqq

  COPY --chown=specqq:specqq target/app.jar /app/app.jar
  CMD ["java", "-jar", "/app/app.jar"]
  ```

- [ ] **Scan Docker Images for Vulnerabilities**
  ```bash
  # Using Trivy
  trivy image specqq:latest

  # Using Docker Scan
  docker scan specqq:latest
  ```

- [ ] **Use Minimal Base Images**
  ```dockerfile
  # Use distroless or alpine images
  FROM gcr.io/distroless/java17-debian11
  # OR
  FROM eclipse-temurin:17-jre-alpine
  ```

- [ ] **Limit Container Resources**
  ```yaml
  # docker-compose.yml
  services:
    backend:
      image: specqq:latest
      deploy:
        resources:
          limits:
            cpus: '2'
            memory: 2G
          reservations:
            cpus: '1'
            memory: 1G
  ```

---

## Monitoring & Logging

### Audit Logging

- [ ] **Log All Security-Relevant Events**
  ```java
  // Login attempts
  log.info("Login attempt: user={}, ip={}, success={}", username, ip, success);

  // Permission changes
  log.warn("Permission change: user={}, target={}, action={}", admin, user, action);

  // Sensitive operations
  log.info("Rule deleted: id={}, deletedBy={}", ruleId, username);
  ```

- [ ] **Never Log Sensitive Data**
  ```java
  // BAD
  log.debug("User login: password={}", password);

  // GOOD
  log.debug("User login: username={}", username);
  ```

- [ ] **Implement Log Retention Policy**
  ```xml
  <!-- logback-spring.xml -->
  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
      <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
          <fileNamePattern>logs/application-%d{yyyy-MM-dd}.log</fileNamePattern>
          <maxHistory>90</maxHistory>  <!-- Keep 90 days -->
          <totalSizeCap>10GB</totalSizeCap>
      </rollingPolicy>
  </appender>
  ```

### Security Monitoring

- [ ] **Monitor Failed Login Attempts**
  ```bash
  # Alert on >10 failed logins from same IP in 5 minutes
  grep "Login failed" /var/log/specqq/application.log | \
    awk '{print $NF}' | sort | uniq -c | awk '$1 > 10 {print $2}'
  ```

- [ ] **Monitor for SQL Injection Attempts**
  ```bash
  # Look for suspicious patterns in logs
  grep -E "(\bOR\b.*=.*|UNION.*SELECT|DROP.*TABLE)" /var/log/specqq/access.log
  ```

- [ ] **Set Up Security Alerts**
  - Failed login attempts
  - Privilege escalation attempts
  - Unusual API usage patterns
  - Database connection failures
  - Unauthorized access attempts

---

## Compliance & Auditing

### Data Privacy

- [ ] **Implement Data Retention Policy**
  - Define how long message logs are kept (e.g., 90 days)
  - Implement automatic data deletion

  ```sql
  -- Delete logs older than 90 days
  DELETE FROM message_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);
  ```

- [ ] **Anonymize or Pseudonymize Personal Data**
  ```java
  // Hash user IDs before logging
  String hashedUserId = DigestUtils.sha256Hex(userId);
  log.info("Message received from user: {}", hashedUserId);
  ```

- [ ] **Implement Data Export/Deletion (GDPR)**
  - Allow users to export their data
  - Allow users to request data deletion
  - Respond to requests within required timeframe

### Security Audits

- [ ] **Conduct Regular Security Audits**
  - Quarterly internal audits
  - Annual external penetration testing
  - Code security reviews

- [ ] **Maintain Security Documentation**
  - Document security architecture
  - Keep this checklist updated
  - Document incident response procedures

- [ ] **Implement Incident Response Plan**
  1. Detection and analysis
  2. Containment
  3. Eradication
  4. Recovery
  5. Post-incident review

---

## Security Maintenance

### Regular Tasks

#### Daily
- [ ] Review security logs for anomalies
- [ ] Check for failed login attempts
- [ ] Verify backup completion

#### Weekly
- [ ] Review access logs
- [ ] Check for security updates
- [ ] Verify SSL certificate validity

#### Monthly
- [ ] Update dependencies
- [ ] Review user permissions
- [ ] Test backup restoration
- [ ] Review rate limiting effectiveness

#### Quarterly
- [ ] Rotate JWT secret
- [ ] Conduct security audit
- [ ] Review and update firewall rules
- [ ] Penetration testing

#### Annually
- [ ] Renew SSL certificates
- [ ] Comprehensive security assessment
- [ ] Update security policies
- [ ] Security awareness training

---

## Security Checklist Summary

### Critical (Must Do Before Production)

- [ ] Change default admin credentials
- [ ] Generate strong JWT secret
- [ ] Enable HTTPS/TLS
- [ ] Configure CORS properly
- [ ] Set database and Redis passwords
- [ ] Implement rate limiting
- [ ] Enable audit logging
- [ ] Configure firewall rules

### High Priority (Do Within First Week)

- [ ] Set up monitoring and alerting
- [ ] Implement automated backups
- [ ] Configure security headers
- [ ] Review and restrict actuator endpoints
- [ ] Set up log aggregation
- [ ] Document security procedures

### Medium Priority (Do Within First Month)

- [ ] Implement dependency scanning
- [ ] Set up automated security updates
- [ ] Conduct security audit
- [ ] Implement data retention policy
- [ ] Set up intrusion detection

### Ongoing

- [ ] Keep dependencies updated
- [ ] Monitor security advisories
- [ ] Review logs regularly
- [ ] Conduct periodic security assessments
- [ ] Update documentation

---

## Security Resources

### Tools

- **Dependency Scanning**: OWASP Dependency-Check, Snyk, GitHub Dependabot
- **Vulnerability Scanning**: Nessus, OpenVAS, Qualys
- **Penetration Testing**: Burp Suite, OWASP ZAP, Metasploit
- **Code Analysis**: SonarQube, Checkmarx, Veracode

### References

- OWASP Top 10: https://owasp.org/www-project-top-ten/
- CWE Top 25: https://cwe.mitre.org/top25/
- Spring Security Documentation: https://spring.io/projects/spring-security
- NIST Cybersecurity Framework: https://www.nist.gov/cyberframework

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-11
**Review Frequency**: Quarterly
**Next Review Date**: 2026-05-11
