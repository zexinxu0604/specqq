#!/bin/bash

echo "=========================================="
echo "  IntelliJ IDEA Lombok 诊断"
echo "=========================================="
echo ""

cd "$(dirname "$0")"

echo "1. 检查 Lombok 插件配置文件..."
echo ""

# 检查 IntelliJ IDEA 插件目录
IDEA_PLUGINS_DIR="$HOME/Library/Application Support/JetBrains"

if [ -d "$IDEA_PLUGINS_DIR" ]; then
    echo "✅ IntelliJ IDEA 配置目录存在"

    # 查找最新的 IntelliJ IDEA 版本
    LATEST_IDEA=$(ls -t "$IDEA_PLUGINS_DIR" | grep "IntelliJIdea" | head -1)

    if [ -n "$LATEST_IDEA" ]; then
        echo "   版本: $LATEST_IDEA"

        # 检查 Lombok 插件
        PLUGINS_DIR="$IDEA_PLUGINS_DIR/$LATEST_IDEA/plugins"
        if [ -d "$PLUGINS_DIR" ]; then
            if ls "$PLUGINS_DIR" | grep -i lombok > /dev/null 2>&1; then
                echo "✅ Lombok 插件已安装"
                ls -la "$PLUGINS_DIR" | grep -i lombok
            else
                echo "❌ Lombok 插件未找到"
                echo "   请在 IDE 中安装: Preferences → Plugins → Lombok"
            fi
        fi
    fi
else
    echo "⚠️ IntelliJ IDEA 配置目录未找到"
fi

echo ""
echo "2. 检查项目 .idea 配置..."
echo ""

if [ -f ".idea/compiler.xml" ]; then
    echo "✅ compiler.xml 存在"

    if grep -q 'enabled="true"' .idea/compiler.xml; then
        echo "✅ 注解处理已启用"
    else
        echo "❌ 注解处理未启用"
    fi

    if grep -q "lombok" .idea/compiler.xml; then
        echo "✅ Lombok 处理器已配置"
    else
        echo "❌ Lombok 处理器未配置"
    fi
else
    echo "❌ compiler.xml 不存在"
fi

echo ""
echo "3. 检查 Maven 依赖..."
echo ""

if [ -d "$HOME/.m2/repository/org/projectlombok/lombok/1.18.30" ]; then
    echo "✅ Lombok 1.18.30 已下载"
    ls -lh "$HOME/.m2/repository/org/projectlombok/lombok/1.18.30/lombok-1.18.30.jar"
else
    echo "❌ Lombok 1.18.30 未下载"
    echo "   运行: mvn dependency:resolve"
fi

echo ""
echo "4. 检查常见问题..."
echo ""

# 检查是否有 .iml 文件
if ls *.iml > /dev/null 2>&1; then
    echo "✅ IntelliJ 项目文件存在"
else
    echo "⚠️ .iml 文件不存在，可能需要重新导入项目"
fi

# 检查 target 目录
if [ -d "target/classes" ]; then
    echo "✅ target/classes 目录存在"

    # 检查是否有编译的类
    CLASS_COUNT=$(find target/classes -name "*.class" 2>/dev/null | wc -l)
    echo "   编译的类文件数: $CLASS_COUNT"
else
    echo "⚠️ target/classes 目录不存在，项目未编译"
fi

echo ""
echo "=========================================="
echo "建议的修复步骤"
echo "=========================================="
echo ""

echo "如果 Lombok 插件已安装但仍有错误："
echo ""
echo "步骤 1: 清理 IntelliJ IDEA 缓存"
echo "  - File → Invalidate Caches / Restart"
echo "  - 选择 'Invalidate and Restart'"
echo ""
echo "步骤 2: 重新导入 Maven 项目"
echo "  - 右键 pom.xml → Maven → Reload Project"
echo ""
echo "步骤 3: 重新构建项目"
echo "  - Build → Rebuild Project"
echo ""
echo "步骤 4: 检查注解处理设置"
echo "  - Preferences → Build, Execution, Deployment"
echo "  - → Compiler → Annotation Processors"
echo "  - 确保 'Enable annotation processing' 已勾选"
echo ""
echo "步骤 5: 如果仍然失败，尝试手动配置"
echo "  - 查看 MANUAL_FIX_LOMBOK.md"
echo ""

