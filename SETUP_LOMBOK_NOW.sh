#!/bin/bash

# IntelliJ IDEA Lombok 快速设置脚本

echo "=========================================="
echo "  IntelliJ IDEA Lombok 设置检查"
echo "=========================================="
echo ""

# 检查 IntelliJ IDEA 是否运行
if pgrep -f "IntelliJIdea" > /dev/null; then
    echo "✅ IntelliJ IDEA 正在运行"
    echo ""
    echo "请按照以下步骤操作:"
    echo ""
    echo "1. 在 IntelliJ IDEA 中:"
    echo "   - 按 ⌘, (Mac) 或 Ctrl+Alt+S (Windows/Linux)"
    echo "   - 选择 'Plugins'"
    echo "   - 点击 'Marketplace'"
    echo "   - 搜索 'Lombok'"
    echo "   - 点击 'Install'"
    echo "   - 重启 IDE"
    echo ""
    echo "2. 重启后:"
    echo "   - Build → Rebuild Project"
    echo "   - 运行 ChatbotRouterApplication.main()"
    echo ""
else
    echo "⚠️ IntelliJ IDEA 未运行"
    echo ""
    echo "请先打开 IntelliJ IDEA:"
    echo "  1. 打开项目: /Users/zexinxu/IdeaProjects/specqq"
    echo "  2. 等待项目加载完成"
    echo "  3. 然后按照 IDEA_LOMBOK_SETUP.md 中的步骤操作"
    echo ""
fi

echo "=========================================="
echo "配置文件检查"
echo "=========================================="
echo ""

# 检查 compiler.xml
if [ -f ".idea/compiler.xml" ]; then
    if grep -q "enabled=\"true\"" .idea/compiler.xml; then
        echo "✅ 注解处理器已启用"
    else
        echo "⚠️ 注解处理器未启用"
    fi
    
    if grep -q "lombok" .idea/compiler.xml; then
        echo "✅ Lombok 处理器路径已配置"
    else
        echo "⚠️ Lombok 处理器路径未配置"
    fi
else
    echo "⚠️ .idea/compiler.xml 不存在"
    echo "   请在 IntelliJ IDEA 中打开项目"
fi

echo ""

# 检查 pom.xml
if grep -q "lombok" pom.xml; then
    echo "✅ pom.xml 包含 Lombok 依赖"
    LOMBOK_VERSION=$(grep -A 1 "<artifactId>lombok</artifactId>" pom.xml | grep "<version>" | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | head -1)
    if [ -n "$LOMBOK_VERSION" ]; then
        echo "   版本: $LOMBOK_VERSION"
    fi
else
    echo "⚠️ pom.xml 未包含 Lombok 依赖"
fi

echo ""
echo "=========================================="
echo "下一步"
echo "=========================================="
echo ""
echo "1. 阅读详细指南: cat IDEA_LOMBOK_SETUP.md"
echo "2. 在 IDE 中安装 Lombok 插件"
echo "3. 重新构建项目"
echo "4. 运行应用测试"
echo ""
echo "快速测试命令:"
echo "  # 在 IDE 中运行"
echo "  右键 ChatbotRouterApplication.java → Run"
echo ""

