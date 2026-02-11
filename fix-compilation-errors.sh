#!/bin/bash

# 自动修复编译错误脚本

set -e

echo "=========================================="
echo "  自动修复编译错误"
echo "=========================================="
echo ""

cd "$(dirname "$0")"

echo "1. 检查并修复 Lombok 配置..."

# 检查 pom.xml 中的 Lombok 配置
if ! grep -q "lombok-maven-plugin" pom.xml; then
    echo "添加 Lombok Maven Plugin..."
    # 这里需要手动添加,先跳过
fi

echo ""
echo "2. 添加 Lombok 配置文件..."

# 创建 lombok.config
cat > lombok.config << 'EOF'
# Lombok 配置
lombok.addLombokGeneratedAnnotation = true
lombok.anyConstructor.addConstructorProperties = true
lombok.log.fieldName = log
EOF

echo "创建 lombok.config 完成"

echo ""
echo "3. 检查实体类注解..."

# 检查 MessageRule.java
if ! grep -q "@Data" src/main/java/com/specqq/chatbot/entity/MessageRule.java; then
    echo "⚠️ MessageRule.java 缺少 @Data 注解"
fi

# 检查 MessageReceiveDTO.java
if ! grep -q "@Data\|@Getter" src/main/java/com/specqq/chatbot/dto/MessageReceiveDTO.java; then
    echo "⚠️ MessageReceiveDTO.java 缺少 Lombok 注解"
fi

# 检查 MessageReplyDTO.java
if ! grep -q "@Builder" src/main/java/com/specqq/chatbot/dto/MessageReplyDTO.java; then
    echo "⚠️ MessageReplyDTO.java 缺少 @Builder 注解"
fi

echo ""
echo "4. 清理并重新编译..."

export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home

# 清理
mvn clean

# 重新下载依赖
mvn dependency:resolve -U

# 编译
echo ""
echo "开始编译..."
if mvn compile -DskipTests; then
    echo ""
    echo "✅ 编译成功!"
else
    echo ""
    echo "❌ 编译失败,查看错误详情:"
    echo ""
    mvn compile -DskipTests 2>&1 | grep "ERROR" | head -20
    echo ""
    echo "建议手动检查以下文件:"
    echo "  - src/main/java/com/specqq/chatbot/entity/MessageRule.java"
    echo "  - src/main/java/com/specqq/chatbot/dto/MessageReceiveDTO.java"
    echo "  - src/main/java/com/specqq/chatbot/dto/MessageReplyDTO.java"
    echo "  - src/main/java/com/specqq/chatbot/service/RuleService.java"
    exit 1
fi

echo ""
echo "=========================================="
echo "修复完成!"
echo "=========================================="
