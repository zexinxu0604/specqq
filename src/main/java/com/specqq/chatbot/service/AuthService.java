package com.specqq.chatbot.service;

import com.specqq.chatbot.dto.LoginResponse;
import com.specqq.chatbot.dto.UserInfo;
import com.specqq.chatbot.entity.User;
import com.specqq.chatbot.mapper.UserMapper;
import com.specqq.chatbot.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 *
 * @author Chatbot Router System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";
    private static final long TOKEN_EXPIRATION_SECONDS = 86400; // 24小时

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录响应
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(String username, String password) {
        // 查询用户
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            log.warn("User not found: username={}", username);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        log.debug("User found: username={}, password hash length={}", username, user.getPassword() != null ? user.getPassword().length() : 0);

        // 验证密码
        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
        log.debug("Password match result: {}", passwordMatches);

        if (!passwordMatches) {
            log.warn("Password mismatch for user: username={}", username);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 检查用户状态
        if (!user.getEnabled()) {
            throw new IllegalArgumentException("用户已被禁用");
        }

        // 生成JWT令牌
        String token = jwtUtil.generateToken(username);

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 构建用户信息
        UserInfo userInfo = buildUserInfo(user);

        log.info("用户登录成功: username={}", username);
        return new LoginResponse(token, TOKEN_EXPIRATION_SECONDS, userInfo);
    }

    /**
     * 用户登出
     *
     * @param token JWT令牌
     */
    public void logout(String token) {
        // 将令牌加入黑名单
        String key = TOKEN_BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", TOKEN_EXPIRATION_SECONDS, TimeUnit.SECONDS);
        log.info("用户登出，令牌已加入黑名单");
    }

    /**
     * 获取用户信息
     *
     * @param token JWT令牌
     * @return 用户信息
     */
    public UserInfo getUserInfo(String token) {
        // 验证令牌
        if (!jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("令牌无效或已过期");
        }

        // 检查黑名单
        if (isTokenBlacklisted(token)) {
            throw new IllegalArgumentException("令牌已失效");
        }

        // 获取用户名
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        return buildUserInfo(user);
    }

    /**
     * 刷新令牌
     *
     * @param token 当前令牌
     * @return 新的登录响应
     */
    public LoginResponse refreshToken(String token) {
        // 验证令牌
        if (!jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("令牌无效或已过期");
        }

        // 检查黑名单
        if (isTokenBlacklisted(token)) {
            throw new IllegalArgumentException("令牌已失效");
        }

        // 获取用户名并生成新令牌
        String username = jwtUtil.getUsernameFromToken(token);
        String newToken = jwtUtil.generateToken(username);

        // 将旧令牌加入黑名单
        logout(token);

        // 获取用户信息
        User user = userMapper.selectByUsername(username);
        UserInfo userInfo = buildUserInfo(user);

        log.info("令牌刷新成功: username={}", username);
        return new LoginResponse(newToken, TOKEN_EXPIRATION_SECONDS, userInfo);
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return jwtUtil.getUsernameFromToken(token);
    }

    /**
     * 初始化默认管理员账户
     */
    @Transactional(rollbackFor = Exception.class)
    public void initDefaultAdmin() {
        // 检查是否已存在管理员
        User existingAdmin = userMapper.selectByUsername("admin");
        if (existingAdmin != null) {
            throw new IllegalStateException("管理员账户已存在");
        }

        // 创建默认管理员
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@chatbot.local");
        admin.setEnabled(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(admin);
        log.info("默认管理员账户创建成功: username=admin, password=admin123");
    }

    /**
     * 修改密码
     *
     * @param username    用户名
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String username, String oldPassword, String newPassword) {
        // 查询用户
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("密码修改成功: username={}", username);
    }

    /**
     * 检查令牌是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        String key = TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 构建用户信息
     */
    private UserInfo buildUserInfo(User user) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setDisplayName(user.getUsername()); // Use username as display name
        userInfo.setEmail(user.getEmail());
        userInfo.setRoles(Arrays.asList(user.getRole())); // Use role from database
        userInfo.setPermissions(Arrays.asList("*")); // 简化实现，实际应从数据库查询
        userInfo.setCreatedAt(user.getCreatedAt());
        userInfo.setLastLoginAt(user.getLastLoginAt());
        return userInfo;
    }
}
