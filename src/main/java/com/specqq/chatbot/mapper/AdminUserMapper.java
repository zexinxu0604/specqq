package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * AdminUser Mapper接口
 *
 * @author Chatbot Router System
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
    // BaseMapper provides standard CRUD operations
    // Username uniqueness enforced at database level
}
