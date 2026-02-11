# Lombok Compilation Issue - Fix Guide

**Status**: ⚠️ BLOCKED - Lombok annotation processing not working
**Date**: 2026-02-09
**Error Count**: ~80 compilation errors

---

## Problem Summary

Lombok annotations (@Data, @Builder, @Slf4j, @Getter, @Setter) are not generating the required methods during Maven compilation, despite multiple configuration attempts.

### Symptoms

```
[ERROR] 找不到符号: 方法 getUserId()
[ERROR] 找不到符号: 方法 builder()
[ERROR] 找不到符号: 变量 log
```

### Root Cause Analysis

After extensive troubleshooting, the issue appears to be related to:

1. **Maven Annotation Processing**: The annotation processor is configured correctly in pom.xml but may not be executing
2. **Spring Boot Parent POM**: May be overriding our compiler configuration
3. **JDK 17 Compatibility**: Potential compatibility issue between Lombok 1.18.30 and JDK 17 in Maven context

---

## Solutions (In Order of Recommendation)

### Solution 1: Use IntelliJ IDEA Lombok Plugin (RECOMMENDED)

This is the fastest solution for development:

1. **Install Lombok Plugin**:
   - Open IntelliJ IDEA
   - Go to `Preferences` > `Plugins`
   - Search for "Lombok"
   - Install and restart IDE

2. **Enable Annotation Processing**:
   - Go to `Preferences` > `Build, Execution, Deployment` > `Compiler` > `Annotation Processors`
   - Check "Enable annotation processing"
   - Click Apply

3. **Rebuild Project**:
   - `Build` > `Rebuild Project`

4. **Verify**:
   - Open any DTO class (e.g., MessageReceiveDTO.java)
   - Try using `.getUserId()` - IDE should autocomplete
   - Run application from IDE: `ChatbotRouterApplication.main()`

**Pros**:
- Quick fix for development
- IDE will handle Lombok correctly
- Can run application and tests from IDE

**Cons**:
- Maven command-line builds will still fail
- CI/CD pipelines won't work

---

### Solution 2: Manual Delombok (TEMPORARY WORKAROUND)

Manually add getter/setter methods to critical classes:

#### Step 1: Add methods to MessageReceiveDTO.java

```java
// Add after the fields:
public String getMessageId() { return messageId; }
public void setMessageId(String messageId) { this.messageId = messageId; }
public String getGroupId() { return groupId; }
public void setGroupId(String groupId) { this.groupId = groupId; }
public String getUserId() { return userId; }
public void setUserId(String userId) { this.userId = userId; }
public String getUserNickname() { return userNickname; }
public void setUserNickname(String userNickname) { this.userNickname = userNickname; }
public String getMessageContent() { return messageContent; }
public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
public LocalDateTime getTimestamp() { return timestamp; }
public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
```

#### Step 2: Add builder to MessageReplyDTO.java

```java
public static class MessageReplyDTOBuilder {
    private String groupId;
    private String replyContent;
    private String messageId;

    public MessageReplyDTOBuilder groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public MessageReplyDTOBuilder replyContent(String replyContent) {
        this.replyContent = replyContent;
        return this;
    }

    public MessageReplyDTOBuilder messageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    public MessageReplyDTO build() {
        return new MessageReplyDTO(groupId, replyContent, messageId);
    }
}

public static MessageReplyDTOBuilder builder() {
    return new MessageReplyDTOBuilder();
}
```

#### Step 3: Add log to classes with @Slf4j

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Add to each class:
private static final Logger log = LoggerFactory.getLogger(ClassName.class);
```

**Files to modify** (in priority order):
1. `MessageReceiveDTO.java` - 6 getter methods
2. `MessageReplyDTO.java` - builder() method
3. `MessageRule.java` - 9 getter methods
4. `MessageRouter.java` - add log variable
5. `NapCatMessageDTO.Sender` - 3 getter methods
6. All Service classes - add log variable

**Pros**:
- Will fix compilation immediately
- Maven builds will work
- Can proceed with testing

**Cons**:
- Tedious manual work
- Code becomes verbose
- Loses Lombok benefits
- Hard to maintain

---

### Solution 3: Upgrade to Latest Lombok (EXPERIMENTAL)

Try the absolute latest Lombok version:

```xml
<properties>
    <lombok.version>1.18.32</lombok.version> <!-- Latest as of Feb 2026 -->
</properties>
```

Then run:
```bash
mvn clean install -U -DskipTests
```

---

### Solution 4: Use Lombok Maven Plugin with Delombok

Configure Maven to use delomboked sources:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok-maven-plugin</artifactId>
            <version>1.18.20.0</version>
            <executions>
                <execution>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>delombok</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <sourceDirectory>src/main/java</sourceDirectory>
                <outputDirectory>${project.build.directory}/generated-sources/delombok</outputDirectory>
                <addOutputDirectory>true</addOutputDirectory>
            </configuration>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <sourceDirectory>${project.build.directory}/generated-sources/delombok</sourceDirectory>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

### Solution 5: Remove Spring Boot Parent (LAST RESORT)

If nothing else works, remove Spring Boot parent POM and manage dependencies manually. **NOT RECOMMENDED** - this is a lot of work.

---

## Verification Commands

After applying any solution:

```bash
# 1. Clean and compile
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
mvn clean compile -DskipTests

# 2. Run tests
mvn test

# 3. Start application
mvn spring-boot:run

# Or use the startup script:
./start-dev.sh
```

---

## Next Steps

**Recommended Path**:

1. **Immediate** (5 minutes): Install IntelliJ IDEA Lombok plugin → Enable annotation processing → Rebuild project → Run from IDE
2. **Short-term** (1 hour): If Maven builds are needed, apply Solution 2 (Manual Delombok) to critical classes
3. **Long-term** (1 day): Investigate Solution 3 or 4 for proper fix

**Priority Files** (if doing manual delombok):
1. MessageReceiveDTO.java
2. MessageReplyDTO.java
3. MessageRule.java
4. MessageRouter.java (add log)
5. All Service classes (add log)

---

## Alternative: Continue Without Fixing

If the above solutions don't work, you can:

1. Document this as a known issue
2. Provide IntelliJ IDEA project files with Lombok configured
3. Note in README that Maven command-line builds require IDE setup
4. Use IDE for all development and testing
5. Configure CI/CD to use IDE build commands (e.g., `idea build`)

---

## Additional Resources

- Lombok Documentation: https://projectlombok.org/setup/maven
- Spring Boot + Lombok: https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.build-systems.maven
- IntelliJ IDEA Lombok Plugin: https://plugins.jetbrains.com/plugin/6317-lombok

---

**Last Updated**: 2026-02-09 18:35
**Status**: Awaiting user decision on fix approach
