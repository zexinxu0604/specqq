package com.specqq.chatbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.specqq.chatbot.entity.HandlerChain;
import com.specqq.chatbot.mapper.HandlerChainMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handler 链服务
 *
 * <p>管理规则的 Handler 执行链配置</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandlerChainService extends ServiceImpl<HandlerChainMapper, HandlerChain> {

    private final HandlerChainMapper handlerChainMapper;

    /**
     * 查询规则的 Handler 链（按顺序）
     *
     * @param ruleId 规则 ID
     * @return Handler 链列表
     */
    @Cacheable(value = "handlerMetadata", key = "'chain:' + #ruleId", cacheManager = "caffeineCacheManager")
    public List<HandlerChain> getHandlerChainByRuleId(Long ruleId) {
        log.debug("查询 Handler 链: ruleId={}", ruleId);
        return handlerChainMapper.selectByRuleIdOrdered(ruleId);
    }

    /**
     * 创建 Handler 链项
     *
     * @param handlerChain Handler 链项
     * @return 创建的 Handler 链项
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "handlerMetadata", key = "'chain:' + #handlerChain.ruleId", cacheManager = "caffeineCacheManager")
    public HandlerChain createHandlerChain(HandlerChain handlerChain) {
        // 如果未指定顺序，自动分配
        if (handlerChain.getSequenceOrder() == null) {
            Integer maxOrder = getMaxSequenceOrder(handlerChain.getRuleId());
            handlerChain.setSequenceOrder(maxOrder + 1);
        }

        handlerChainMapper.insert(handlerChain);
        log.info("创建 Handler 链项: id={}, ruleId={}, handlerType={}, order={}",
                handlerChain.getId(), handlerChain.getRuleId(),
                handlerChain.getHandlerType(), handlerChain.getSequenceOrder());

        return handlerChain;
    }

    /**
     * 批量创建 Handler 链
     *
     * @param ruleId       规则 ID
     * @param handlerChains Handler 链列表
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "handlerMetadata", key = "'chain:' + #ruleId", cacheManager = "caffeineCacheManager")
    public void batchCreateHandlerChain(Long ruleId, List<HandlerChain> handlerChains) {
        // 先删除现有的 Handler 链
        deleteHandlerChainByRuleId(ruleId);

        // 批量插入新的 Handler 链
        for (int i = 0; i < handlerChains.size(); i++) {
            HandlerChain chain = handlerChains.get(i);
            chain.setRuleId(ruleId);
            chain.setSequenceOrder(i + 1);
            handlerChainMapper.insert(chain);
        }

        log.info("批量创建 Handler 链: ruleId={}, count={}", ruleId, handlerChains.size());
    }

    /**
     * 更新 Handler 链项
     *
     * @param handlerChain Handler 链项
     * @return 更新的 Handler 链项
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "handlerMetadata", key = "'chain:' + #handlerChain.ruleId", cacheManager = "caffeineCacheManager")
    public HandlerChain updateHandlerChain(HandlerChain handlerChain) {
        HandlerChain existing = handlerChainMapper.selectById(handlerChain.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Handler 链项不存在: id=" + handlerChain.getId());
        }

        handlerChainMapper.updateById(handlerChain);
        log.info("更新 Handler 链项: id={}, ruleId={}, handlerType={}",
                handlerChain.getId(), handlerChain.getRuleId(), handlerChain.getHandlerType());

        return handlerChain;
    }

    /**
     * 删除规则的所有 Handler 链
     *
     * @param ruleId 规则 ID
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "handlerMetadata", key = "'chain:' + #ruleId", cacheManager = "caffeineCacheManager")
    public void deleteHandlerChainByRuleId(Long ruleId) {
        LambdaQueryWrapper<HandlerChain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandlerChain::getRuleId, ruleId);

        handlerChainMapper.delete(wrapper);
        log.info("删除 Handler 链: ruleId={}", ruleId);
    }

    /**
     * 删除单个 Handler 链项
     *
     * @param chainId Handler 链项 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteHandlerChainItem(Long chainId) {
        HandlerChain chain = handlerChainMapper.selectById(chainId);
        if (chain == null) {
            throw new IllegalArgumentException("Handler 链项不存在: id=" + chainId);
        }

        handlerChainMapper.deleteById(chainId);
        log.info("删除 Handler 链项: id={}, ruleId={}", chainId, chain.getRuleId());

        // 清除缓存
        evictCache(chain.getRuleId());
    }

    /**
     * 调整 Handler 链顺序
     *
     * @param ruleId    规则 ID
     * @param chainId   Handler 链项 ID
     * @param newOrder  新的顺序
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "handlerMetadata", key = "'chain:' + #ruleId", cacheManager = "caffeineCacheManager")
    public void reorderHandlerChain(Long ruleId, Long chainId, Integer newOrder) {
        List<HandlerChain> chains = getHandlerChainByRuleId(ruleId);

        // 找到要移动的链项
        HandlerChain targetChain = null;
        for (HandlerChain chain : chains) {
            if (chain.getId().equals(chainId)) {
                targetChain = chain;
                break;
            }
        }

        if (targetChain == null) {
            throw new IllegalArgumentException("Handler 链项不存在: id=" + chainId);
        }

        // 移除目标链项
        chains.remove(targetChain);

        // 插入到新位置
        chains.add(newOrder - 1, targetChain);

        // 重新分配顺序
        for (int i = 0; i < chains.size(); i++) {
            HandlerChain chain = chains.get(i);
            chain.setSequenceOrder(i + 1);
            handlerChainMapper.updateById(chain);
        }

        log.info("调整 Handler 链顺序: ruleId={}, chainId={}, newOrder={}", ruleId, chainId, newOrder);
    }

    /**
     * 获取规则的最大顺序号
     *
     * @param ruleId 规则 ID
     * @return 最大顺序号
     */
    private Integer getMaxSequenceOrder(Long ruleId) {
        List<HandlerChain> chains = getHandlerChainByRuleId(ruleId);
        if (chains.isEmpty()) {
            return 0;
        }

        return chains.stream()
                .mapToInt(HandlerChain::getSequenceOrder)
                .max()
                .orElse(0);
    }

    /**
     * 清除缓存
     *
     * @param ruleId 规则 ID
     */
    @CacheEvict(value = "handlerMetadata", key = "'chain:' + #ruleId", cacheManager = "caffeineCacheManager")
    public void evictCache(Long ruleId) {
        log.debug("清除 Handler 链缓存: ruleId={}", ruleId);
    }
}
