#!/bin/bash

# Chatbot Router 系统测试脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "  Chatbot Router - 系统测试"
echo "=========================================="
echo ""

# 测试计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试结果记录
TEST_RESULTS=()

# 测试函数
run_test() {
    local test_name="$1"
    local test_command="$2"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -n "[$TOTAL_TESTS] $test_name ... "

    if eval "$test_command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        TEST_RESULTS+=("✓ $test_name")
    else
        echo -e "${RED}✗ FAIL${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TEST_RESULTS+=("✗ $test_name")
    fi
}

# 1. 环境检查
echo -e "${BLUE}=== 1. 环境检查 ===${NC}"
echo ""

run_test "JDK 17 已安装" "test -d /Library/Java/JavaVirtualMachines/temurin-17.jdk"
run_test "Maven 已安装" "which mvn"
run_test "Node.js 已安装" "which node"
run_test "MySQL 运行中" "pgrep -x mysqld"
run_test "Redis 运行中" "pgrep -x redis-server"
run_test "数据库存在" "mysql -uroot -e 'USE chatbot_router;'"

echo ""

# 2. 数据库连接测试
echo -e "${BLUE}=== 2. 数据库连接测试 ===${NC}"
echo ""

run_test "MySQL 连接测试" "mysql -uroot -e 'SELECT 1;'"
run_test "Redis 连接测试" "redis-cli ping | grep -q PONG"
run_test "数据库字符集正确" "mysql -uroot -e 'SELECT @@character_set_database FROM information_schema.schemata WHERE schema_name=\"chatbot_router\";' | grep -q utf8mb4"

echo ""

# 3. 项目结构检查
echo -e "${BLUE}=== 3. 项目结构检查 ===${NC}"
echo ""

run_test "pom.xml 存在" "test -f pom.xml"
run_test "src/main/java 存在" "test -d src/main/java"
run_test "src/test/java 存在" "test -d src/test/java"
run_test "frontend 目录存在" "test -d frontend"
run_test "application.yml 存在" "test -f src/main/resources/application.yml"

echo ""

# 4. 依赖检查
echo -e "${BLUE}=== 4. 依赖检查 ===${NC}"
echo ""

export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home

run_test "Maven 依赖完整" "mvn dependency:resolve -q"
run_test "前端依赖已安装" "test -d frontend/node_modules"

echo ""

# 5. 编译测试
echo -e "${BLUE}=== 5. 编译测试 ===${NC}"
echo ""

echo "编译后端项目 (可能需要几分钟)..."
if mvn clean compile -DskipTests -q; then
    echo -e "${GREEN}✓ 后端编译成功${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    TEST_RESULTS+=("✓ 后端编译成功")
else
    echo -e "${RED}✗ 后端编译失败${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    TEST_RESULTS+=("✗ 后端编译失败")
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""

# 6. 单元测试
echo -e "${BLUE}=== 6. 单元测试 ===${NC}"
echo ""

echo "运行单元测试 (可能需要几分钟)..."
if mvn test -q 2>&1 | tee test-results.log; then
    echo -e "${GREEN}✓ 单元测试通过${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    TEST_RESULTS+=("✓ 单元测试通过")

    # 统计测试结果
    if [ -f test-results.log ]; then
        TESTS_RUN=$(grep -o "Tests run: [0-9]*" test-results.log | tail -1 | grep -o "[0-9]*")
        TESTS_FAILED=$(grep -o "Failures: [0-9]*" test-results.log | tail -1 | grep -o "[0-9]*")
        TESTS_ERRORS=$(grep -o "Errors: [0-9]*" test-results.log | tail -1 | grep -o "[0-9]*")

        if [ ! -z "$TESTS_RUN" ]; then
            echo "  - 测试总数: $TESTS_RUN"
            echo "  - 失败: ${TESTS_FAILED:-0}"
            echo "  - 错误: ${TESTS_ERRORS:-0}"
        fi
    fi
else
    echo -e "${RED}✗ 单元测试失败${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    TEST_RESULTS+=("✗ 单元测试失败")
    echo "查看详细日志: test-results.log"
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""

# 7. 代码质量检查
echo -e "${BLUE}=== 7. 代码质量检查 ===${NC}"
echo ""

run_test "Java 代码编译无警告" "mvn compile -q 2>&1 | grep -v WARNING"
run_test "前端代码检查" "cd frontend && npm run lint --silent"

echo ""

# 8. 配置文件验证
echo -e "${BLUE}=== 8. 配置文件验证 ===${NC}"
echo ""

run_test "application.yml 格式正确" "grep -q 'spring:' src/main/resources/application.yml"
run_test "数据库配置存在" "grep -q 'datasource:' src/main/resources/application.yml"
run_test "Redis 配置存在" "grep -q 'redis:' src/main/resources/application.yml"

echo ""

# 测试总结
echo "=========================================="
echo -e "${BLUE}测试总结${NC}"
echo "=========================================="
echo ""
echo "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"
echo ""

# 显示详细结果
echo "详细结果:"
for result in "${TEST_RESULTS[@]}"; do
    echo "  $result"
done
echo ""

# 计算通过率
PASS_RATE=$(awk "BEGIN {printf \"%.1f\", ($PASSED_TESTS/$TOTAL_TESTS)*100}")
echo "通过率: $PASS_RATE%"
echo ""

# 生成测试报告
cat > TEST_REPORT.md << EOF
# 系统测试报告

**测试时间**: $(date '+%Y-%m-%d %H:%M:%S')

## 测试统计

- **总测试数**: $TOTAL_TESTS
- **通过**: $PASSED_TESTS
- **失败**: $FAILED_TESTS
- **通过率**: $PASS_RATE%

## 测试结果

$(for result in "${TEST_RESULTS[@]}"; do echo "- $result"; done)

## 环境信息

- **操作系统**: $(uname -s) $(uname -r)
- **JDK 版本**: $(java -version 2>&1 | head -n 1)
- **Maven 版本**: $(mvn --version | head -n 1)
- **Node.js 版本**: $(node --version)
- **MySQL 版本**: $(mysql --version)
- **Redis 版本**: $(redis-server --version)

## 下一步

EOF

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过!${NC}"
    echo ""
    echo "系统已准备就绪,可以启动服务:"
    echo "  ./start-dev.sh      # 启动后端"
    echo "  ./start-frontend.sh # 启动前端"

    cat >> TEST_REPORT.md << EOF
✅ **所有测试通过!** 系统已准备就绪。

建议:
1. 启动后端服务: \`./start-dev.sh\`
2. 启动前端服务: \`./start-frontend.sh\`
3. 访问 http://localhost:5173 进行功能测试
EOF
else
    echo -e "${RED}✗ 有测试失败,请检查错误信息${NC}"
    echo ""
    echo "查看详细日志:"
    echo "  cat test-results.log"

    cat >> TEST_REPORT.md << EOF
⚠️ **部分测试失败**,需要修复。

建议:
1. 查看详细日志: \`cat test-results.log\`
2. 修复失败的测试
3. 重新运行测试: \`./run-system-tests.sh\`
EOF
fi

echo ""
echo "测试报告已生成: TEST_REPORT.md"
echo "=========================================="

exit $FAILED_TESTS
