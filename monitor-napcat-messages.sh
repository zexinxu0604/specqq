#!/bin/bash

# NapCat 消息监控脚本
# 用于实时监控和记录从 NapCat 接收到的消息格式

LOG_FILE="napcat-messages.log"
BACKEND_LOG="backend.log"

echo "========================================" | tee -a "$LOG_FILE"
echo "NapCat 消息监控 - 启动时间: $(date)" | tee -a "$LOG_FILE"
echo "========================================" | tee -a "$LOG_FILE"
echo ""

echo "监控后端日志: $BACKEND_LOG" | tee -a "$LOG_FILE"
echo "消息记录文件: $LOG_FILE" | tee -a "$LOG_FILE"
echo ""
echo "按 Ctrl+C 停止监控"
echo ""

# 监控 WebSocket 连接状态
echo "--- WebSocket 连接状态 ---" | tee -a "$LOG_FILE"
tail -f "$BACKEND_LOG" | grep --line-buffered -E "(WebSocket|Received WebSocket message|parseMessage)" | while read line; do
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $line" | tee -a "$LOG_FILE"
done
