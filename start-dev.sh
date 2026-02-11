#!/bin/bash

# Chatbot Router 开发环境启动脚本

set -e

echo "=========================================="
echo "  Chatbot Router - 开发环境启动"
echo "=========================================="
echo ""

# 检查服务状态
echo "1. 检查依赖服务..."
echo ""

# 检查 MySQL
if ! pgrep -x "mysqld" > /dev/null; then
    echo "❌ MySQL 未运行,正在启动..."
    brew services start mysql@8.4
    sleep 3
else
    echo "✅ MySQL 运行中"
fi

# 检查 Redis
if ! pgrep -x "redis-server" > /dev/null; then
    echo "❌ Redis 未运行,正在启动..."
    brew services start redis
    sleep 2
else
    echo "✅ Redis 运行中"
fi

echo ""
echo "2. 配置环境变量..."
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
echo "✅ JAVA_HOME=$JAVA_HOME"

echo ""
echo "3. 启动后端服务..."
cd "$(dirname "$0")"

# 如果已经编译过,直接运行;否则先编译
if [ -f "target/chatbot-router.jar" ]; then
    echo "使用已编译的jar包..."
    $JAVA_HOME/bin/java -jar target/chatbot-router.jar --spring.profiles.active=dev
else
    echo "首次启动,正在编译..."
    mvn clean package -DskipTests
    echo ""
    echo "编译完成,启动应用..."
    $JAVA_HOME/bin/java -jar target/chatbot-router.jar --spring.profiles.active=dev
fi
