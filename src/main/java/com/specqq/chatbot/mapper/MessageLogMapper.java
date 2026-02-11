package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.specqq.chatbot.entity.MessageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * MessageLog Mapper接口
 *
 * @author Chatbot Router System
 */
@Mapper
public interface MessageLogMapper extends BaseMapper<MessageLog> {

    /**
     * 条件分页查询消息日志
     *
     * @param page      分页参数
     * @param groupId   群聊ID(可选)
     * @param userId    用户ID(可选)
     * @param startTime 开始时间(可选)
     * @param endTime   结束时间(可选)
     * @return 分页结果
     */
    IPage<MessageLog> selectByConditions(Page<MessageLog> page,
                                         @Param("groupId") Long groupId,
                                         @Param("userId") String userId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
}
