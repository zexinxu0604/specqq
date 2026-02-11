#!/bin/bash

# API测试脚本 - User Story 2 Web管理控制台
# 测试日期: 2026-02-09

BASE_URL="http://localhost:8080/api"
TOKEN=""
RULE_ID=""
GROUP_ID=""
LOG_ID=""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
test_api() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local expected_status="$5"
    local headers="$6"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -e "\n${YELLOW}[测试 $TOTAL_TESTS]${NC} $test_name"
    echo "  请求: $method $endpoint"

    if [ -n "$data" ]; then
        echo "  数据: $data"
    fi

    # 构建curl命令
    local curl_cmd="curl -s -w '\n%{http_code}' -X $method"

    if [ -n "$headers" ]; then
        curl_cmd="$curl_cmd $headers"
    fi

    if [ -n "$data" ]; then
        curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$data'"
    fi

    curl_cmd="$curl_cmd $BASE_URL$endpoint"

    # 执行请求
    response=$(eval $curl_cmd)
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    echo "  响应状态: $status_code"
    echo "  响应体: $body" | head -c 200
    if [ ${#body} -gt 200 ]; then
        echo "..."
    fi

    # 检查状态码
    if [ "$status_code" = "$expected_status" ]; then
        echo -e "  ${GREEN}✓ 通过${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo "$body"
    else
        echo -e "  ${RED}✗ 失败${NC} (期望: $expected_status, 实际: $status_code)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo ""
    fi
}

# 等待服务启动
wait_for_service() {
    echo -e "${YELLOW}等待后端服务启动...${NC}"
    for i in {1..30}; do
        if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/../actuator/health" | grep -q "200"; then
            echo -e "${GREEN}✓ 后端服务已就绪${NC}"
            return 0
        fi
        echo "  等待中... ($i/30)"
        sleep 2
    done
    echo -e "${RED}✗ 后端服务启动超时${NC}"
    return 1
}

echo "======================================"
echo "  聊天机器人路由系统 - API测试"
echo "======================================"
echo "测试环境: $BASE_URL"
echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "======================================"

# 等待服务
if ! wait_for_service; then
    echo -e "${RED}无法连接到后端服务,请先启动后端${NC}"
    echo "启动命令: mvn spring-boot:run"
    exit 1
fi

echo -e "\n${YELLOW}=== 1. 认证API测试 ===${NC}"

# 1.1 初始化管理员
test_api "初始化管理员账户" \
    "POST" \
    "/auth/init-admin" \
    "" \
    "200"

# 1.2 登录
login_response=$(test_api "管理员登录" \
    "POST" \
    "/auth/login" \
    '{"username":"admin","password":"admin123"}' \
    "200")

# 提取token
TOKEN=$(echo "$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
if [ -n "$TOKEN" ]; then
    echo -e "${GREEN}✓ Token获取成功${NC}"
    AUTH_HEADER="-H 'Authorization: Bearer $TOKEN'"
else
    echo -e "${RED}✗ Token获取失败${NC}"
    AUTH_HEADER=""
fi

# 1.3 获取用户信息
test_api "获取用户信息" \
    "GET" \
    "/auth/user-info" \
    "" \
    "200" \
    "$AUTH_HEADER"

# 1.4 登录失败测试
test_api "登录失败(错误密码)" \
    "POST" \
    "/auth/login" \
    '{"username":"admin","password":"wrong"}' \
    "401"

echo -e "\n${YELLOW}=== 2. 规则管理API测试 ===${NC}"

# 2.1 创建规则
create_rule_response=$(test_api "创建规则" \
    "POST" \
    "/rules" \
    '{"name":"测试规则","matchType":"KEYWORD","matchPattern":"你好","replyTemplate":"你好,我是机器人","priority":50,"description":"测试用规则"}' \
    "200" \
    "$AUTH_HEADER")

# 提取规则ID
RULE_ID=$(echo "$create_rule_response" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "规则ID: $RULE_ID"

# 2.2 查询规则列表
test_api "查询规则列表" \
    "GET" \
    "/rules?page=1&size=10" \
    "" \
    "200" \
    "$AUTH_HEADER"

# 2.3 搜索规则
test_api "搜索规则(关键词)" \
    "GET" \
    "/rules?keyword=测试&page=1&size=10" \
    "" \
    "200" \
    "$AUTH_HEADER"

# 2.4 按匹配类型过滤
test_api "过滤规则(匹配类型)" \
    "GET" \
    "/rules?matchType=KEYWORD&page=1&size=10" \
    "" \
    "200" \
    "$AUTH_HEADER"

if [ -n "$RULE_ID" ]; then
    # 2.5 获取规则详情
    test_api "获取规则详情" \
        "GET" \
        "/rules/$RULE_ID" \
        "" \
        "200" \
        "$AUTH_HEADER"

    # 2.6 更新规则
    test_api "更新规则" \
        "PUT" \
        "/rules/$RULE_ID" \
        '{"name":"测试规则(已更新)","matchType":"KEYWORD","matchPattern":"你好","replyTemplate":"你好,我是机器人(更新版)","priority":60,"description":"更新后的测试规则"}' \
        "200" \
        "$AUTH_HEADER"

    # 2.7 复制规则
    test_api "复制规则" \
        "POST" \
        "/rules/$RULE_ID/copy" \
        "" \
        "200" \
        "$AUTH_HEADER"

    # 2.8 启用/禁用规则
    test_api "禁用规则" \
        "PUT" \
        "/rules/$RULE_ID/toggle?enabled=false" \
        "" \
        "200" \
        "$AUTH_HEADER"

    test_api "启用规则" \
        "PUT" \
        "/rules/$RULE_ID/toggle?enabled=true" \
        "" \
        "200" \
        "$AUTH_HEADER"
fi

# 2.9 验证正则表达式
test_api "验证正则表达式(有效)" \
    "POST" \
    "/rules/validate-pattern" \
    '{"pattern":"^hello.*"}' \
    "200" \
    "$AUTH_HEADER"

test_api "验证正则表达式(无效)" \
    "POST" \
    "/rules/validate-pattern" \
    '{"pattern":"[invalid"}' \
    "200" \
    "$AUTH_HEADER"

# 2.10 测试规则匹配
test_api "测试规则匹配" \
    "POST" \
    "/rules/test" \
    '{"matchType":"KEYWORD","matchPattern":"你好","testMessage":"你好,世界"}' \
    "200" \
    "$AUTH_HEADER"

echo -e "\n${YELLOW}=== 3. 群聊管理API测试 ===${NC}"

# 3.1 查询群聊列表
test_api "查询群聊列表" \
    "GET" \
    "/groups?page=1&size=10" \
    "" \
    "200" \
    "$AUTH_HEADER"

# 3.2 搜索群聊
test_api "搜索群聊" \
    "GET" \
    "/groups?keyword=测试&page=1&size=10" \
    "" \
    "200" \
    "$AUTH_HEADER"

# 注意: 群聊数据通常由QQ消息触发自动创建,这里测试可能返回空列表

echo -e "\n${YELLOW}=== 4. 日志管理API测试 ===${NC}"

# 4.1 查询日志列表
test_api "查询日志列表" \
    "GET" \
    "/logs?page=1&size=10" \
    "" \
    "200" \
    "$AUTH_HEADER"

# 4.2 按状态过滤
test_api "过滤日志(状态)" \
    "GET" \
    "/logs?sendStatus=SUCCESS&page=1&size=10" \
    "" \
    "200" \
    "$AUTH_HEADER"

# 4.3 获取日志统计
test_api "获取日志统计" \
    "GET" \
    "/logs/stats" \
    "" \
    "200" \
    "$AUTH_HEADER"

# 4.4 获取热门规则
test_api "获取热门规则" \
    "GET" \
    "/logs/top-rules?limit=10" \
    "" \
    "200" \
    "$AUTH_HEADER"

# 4.5 获取活跃用户
test_api "获取活跃用户" \
    "GET" \
    "/logs/top-users?limit=10" \
    "" \
    "200" \
    "$AUTH_HEADER"

# 4.6 获取消息趋势
test_api "获取消息趋势" \
    "GET" \
    "/logs/trends?period=7d" \
    "" \
    "200" \
    "$AUTH_HEADER"

echo -e "\n${YELLOW}=== 5. 安全测试 ===${NC}"

# 5.1 无token访问
test_api "无token访问(应拒绝)" \
    "GET" \
    "/rules" \
    "" \
    "401"

# 5.2 错误token访问
test_api "错误token访问(应拒绝)" \
    "GET" \
    "/rules" \
    "" \
    "401" \
    "-H 'Authorization: Bearer invalid_token_12345'"

echo -e "\n${YELLOW}=== 6. 清理测试数据 ===${NC}"

if [ -n "$RULE_ID" ]; then
    # 删除创建的规则
    test_api "删除测试规则" \
        "DELETE" \
        "/rules/$RULE_ID" \
        "" \
        "200" \
        "$AUTH_HEADER"
fi

# 批量删除测试(如果有多个规则)
# test_api "批量删除规则" \
#     "DELETE" \
#     "/rules/batch" \
#     '{"ids":[1,2,3]}' \
#     "200" \
#     "$AUTH_HEADER"

echo -e "\n======================================"
echo "  测试完成"
echo "======================================"
echo -e "总测试数: ${YELLOW}$TOTAL_TESTS${NC}"
echo -e "通过: ${GREEN}$PASSED_TESTS${NC}"
echo -e "失败: ${RED}$FAILED_TESTS${NC}"
echo -e "通过率: $(awk "BEGIN {printf \"%.1f%%\", ($PASSED_TESTS/$TOTAL_TESTS)*100}")"
echo "======================================"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过!${NC}"
    exit 0
else
    echo -e "${RED}✗ 有测试失败,请检查日志${NC}"
    exit 1
fi
