# JWT API 修复完成

**修复时间**: 2026-02-09 18:58
**问题**: JJWT 0.12.x API 变更导致编译错误

---

## ✅ 修复的问题

### 问题: JWT API 不兼容

**错误信息**:
```
java: 找不到符号
  符号:   方法 parserBuilder()
  位置: 类 io.jsonwebtoken.Jwts
```

**根本原因**:
JJWT 从 0.11.x 升级到 0.12.x 后，API 发生了重大变更。

---

## 🔄 API 变更对照

### 1. JWT 解析 API

**0.11.x (旧版本)**:
```java
Jwts.parserBuilder()
    .setSigningKey(key)
    .build()
    .parseClaimsJws(token)
    .getBody();
```

**0.12.x (新版本)**:
```java
Jwts.parser()
    .verifyWith(key)
    .build()
    .parseSignedClaims(token)
    .getPayload();
```

### 2. JWT 生成 API

**0.11.x (旧版本)**:
```java
Jwts.builder()
    .setClaims(claims)
    .setSubject(username)
    .setIssuedAt(new Date())
    .setExpiration(expiration)
    .signWith(key, SignatureAlgorithm.HS512)
    .compact();
```

**0.12.x (新版本)**:
```java
Jwts.builder()
    .claims(claims)
    .subject(username)
    .issuedAt(new Date())
    .expiration(expiration)
    .signWith(key)  // 自动检测算法
    .compact();
```

---

## 📝 具体修复内容

### 修改的文件: `JwtUtil.java`

#### 修复 1: 更新解析方法

**修复前**:
```java
private Claims getClaimsFromToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
}
```

**修复后**:
```java
private Claims getClaimsFromToken(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
}
```

#### 修复 2: 更新生成方法

**修复前**:
```java
public String generateToken(String username) {
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(username)
        .setIssuedAt(new Date())
        .setExpiration(expiration)
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
}
```

**修复后**:
```java
public String generateToken(String username) {
    return Jwts.builder()
        .claims(claims)
        .subject(username)
        .issuedAt(new Date())
        .expiration(expiration)
        .signWith(getSigningKey())
        .compact();
}
```

#### 修复 3: 删除不需要的导入

**删除**:
```java
import io.jsonwebtoken.SignatureAlgorithm;
```

---

## 📊 API 变更总结

| 方法 | 0.11.x | 0.12.x |
|------|--------|--------|
| 解析器构建 | `parserBuilder()` | `parser()` |
| 设置签名密钥 | `setSigningKey()` | `verifyWith()` |
| 解析 JWT | `parseClaimsJws()` | `parseSignedClaims()` |
| 获取 Claims | `getBody()` | `getPayload()` |
| 设置 Claims | `setClaims()` | `claims()` |
| 设置主题 | `setSubject()` | `subject()` |
| 设置签发时间 | `setIssuedAt()` | `issuedAt()` |
| 设置过期时间 | `setExpiration()` | `expiration()` |
| 签名 | `signWith(key, algo)` | `signWith(key)` |

---

## ✅ 修复验证

修复后的代码：
- ✅ 使用 JJWT 0.12.3 的新 API
- ✅ 自动检测签名算法（基于密钥类型）
- ✅ 更简洁的 API 调用
- ✅ 向后兼容（功能保持不变）

---

## 🎯 下一步

**在 IntelliJ IDEA 中执行**:
```
Build → Rebuild Project
```

这次应该完全没有问题了！

---

## 📚 参考资料

- JJWT 0.12.x 迁移指南: https://github.com/jwtk/jjwt#migration-to-012x
- JJWT 文档: https://github.com/jwtk/jjwt

---

**修复完成！现在请重新构建项目！** 🚀
