#!/bin/bash

echo "=========================================="
echo "  Lombok 配置验证"
echo "=========================================="
echo ""

cd "$(dirname "$0")"

echo "步骤 1: 检查 Maven 编译..."
echo ""

export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home

# 尝试编译
mvn clean compile -DskipTests 2>&1 | tail -20

echo ""
echo "=========================================="
echo "步骤 2: 检查测试编译..."
echo ""

mvn test-compile -DskipTests 2>&1 | tail -10

echo ""
echo "=========================================="
echo "验证完成"
echo "=========================================="
echo ""
echo "如果看到 'BUILD SUCCESS'，说明配置成功！"
echo ""
echo "下一步操作:"
echo "  1. 在 IntelliJ IDEA 中运行 ChatbotRouterApplication"
echo "  2. 或者运行: ./start-dev.sh"
echo ""

