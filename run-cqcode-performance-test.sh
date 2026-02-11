#!/bin/bash

# CQ码解析性能测试脚本 (T121)
# 使用方法: ./run-cqcode-performance-test.sh

set -e

# 默认配置
HOST="localhost"
PORT="8080"
CONCURRENT_USERS="50"
RAMP_UP_PERIOD="5"
LOOP_COUNT="20"
JMETER_HOME="${JMETER_HOME:-/usr/local/bin}"
TEST_PLAN="src/test/resources/jmeter/cqcode-performance-test.jmx"
RESULTS_DIR="test-results/cqcode-performance"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_FILE="${RESULTS_DIR}/results_${TIMESTAMP}.jtl"
HTML_REPORT_DIR="${RESULTS_DIR}/html-report-${TIMESTAMP}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查JMeter是否安装
check_jmeter() {
    print_info "检查JMeter安装..."

    if command -v jmeter &> /dev/null; then
        JMETER_CMD="jmeter"
        print_info "找到JMeter: $(which jmeter)"
    elif [ -f "${JMETER_HOME}/jmeter" ]; then
        JMETER_CMD="${JMETER_HOME}/jmeter"
        print_info "找到JMeter: ${JMETER_CMD}"
    else
        print_error "未找到JMeter，请安装JMeter或设置JMETER_HOME环境变量"
        print_info "安装方法:"
        print_info "  macOS:  brew install jmeter"
        print_info "  Linux:  下载并解压 https://jmeter.apache.org/download_jmeter.cgi"
        exit 1
    fi

    # 显示JMeter版本
    JMETER_VERSION=$($JMETER_CMD --version 2>&1 | grep "Version" | head -1)
    print_info "JMeter版本: ${JMETER_VERSION}"
}

# 检查应用健康状态
check_health() {
    print_info "检查应用健康状态: http://${HOST}:${PORT}/actuator/health"

    HEALTH_URL="http://${HOST}:${PORT}/actuator/health"

    if ! curl -f -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" | grep -q "200"; then
        print_error "应用健康检查失败，请确保应用正在运行"
        print_info "启动应用: mvn spring-boot:run -Dspring-boot.run.profiles=dev"
        exit 1
    fi

    print_info "应用健康检查通过 ✓"
}

# 创建结果目录
prepare_results_dir() {
    print_info "准备结果目录: ${RESULTS_DIR}"
    mkdir -p "${RESULTS_DIR}"
}

# 运行JMeter测试
run_test() {
    print_info "开始CQ码解析性能测试..."
    print_info "配置参数:"
    print_info "  目标地址: http://${HOST}:${PORT}"
    print_info "  并发用户: ${CONCURRENT_USERS}"
    print_info "  爬坡时间: ${RAMP_UP_PERIOD}秒"
    print_info "  循环次数: ${LOOP_COUNT}"
    print_info "  总请求数: $((CONCURRENT_USERS * LOOP_COUNT * 3)) (3 API endpoints)"

    # 运行JMeter
    $JMETER_CMD -n \
        -t "$TEST_PLAN" \
        -l "$RESULTS_FILE" \
        -e -o "$HTML_REPORT_DIR" \
        -Jhost="$HOST" \
        -Jport="$PORT" \
        -JCONCURRENT_USERS="$CONCURRENT_USERS" \
        -JRAMP_UP_PERIOD="$RAMP_UP_PERIOD" \
        -JLOOP_COUNT="$LOOP_COUNT"

    if [ $? -eq 0 ]; then
        print_info "测试完成 ✓"
    else
        print_error "测试失败"
        exit 1
    fi
}

# 分析结果
analyze_results() {
    print_info "分析测试结果..."

    if [ ! -f "$RESULTS_FILE" ]; then
        print_error "结果文件不存在: ${RESULTS_FILE}"
        exit 1
    fi

    # 计算关键指标
    TOTAL_REQUESTS=$(awk -F',' 'NR>1 {count++} END {print count}' "$RESULTS_FILE")
    SUCCESS_REQUESTS=$(awk -F',' 'NR>1 && $8=="true" {count++} END {print count}' "$RESULTS_FILE")
    FAILED_REQUESTS=$((TOTAL_REQUESTS - SUCCESS_REQUESTS))
    SUCCESS_RATE=$(awk "BEGIN {printf \"%.2f\", ($SUCCESS_REQUESTS/$TOTAL_REQUESTS)*100}")

    # 分别计算每个API的P95延迟
    PARSE_P95=$(awk -F',' 'NR>1 && $3=="Parse 50 CQ Codes" {print $2}' "$RESULTS_FILE" | sort -n | awk '{a[NR]=$1} END {printf "%.2f", a[int(NR*0.95)]}')
    STATS_P95=$(awk -F',' 'NR>1 && $3=="Calculate Statistics" {print $2}' "$RESULTS_FILE" | sort -n | awk '{a[NR]=$1} END {printf "%.2f", a[int(NR*0.95)]}')
    API_P95=$(awk -F',' 'NR>1 && $3=="End-to-End API" {print $2}' "$RESULTS_FILE" | sort -n | awk '{a[NR]=$1} END {printf "%.2f", a[int(NR*0.95)]}')

    # 计算平均响应时间
    PARSE_AVG=$(awk -F',' 'NR>1 && $3=="Parse 50 CQ Codes" {sum+=$2; count++} END {printf "%.2f", sum/count}' "$RESULTS_FILE")
    STATS_AVG=$(awk -F',' 'NR>1 && $3=="Calculate Statistics" {sum+=$2; count++} END {printf "%.2f", sum/count}' "$RESULTS_FILE")
    API_AVG=$(awk -F',' 'NR>1 && $3=="End-to-End API" {sum+=$2; count++} END {printf "%.2f", sum/count}' "$RESULTS_FILE")

    echo ""
    echo "=========================================="
    echo "     CQ码解析性能测试结果汇总 (T121)"
    echo "=========================================="
    echo ""
    echo "请求统计:"
    echo "  总请求数:       $TOTAL_REQUESTS"
    echo "  成功请求:       $SUCCESS_REQUESTS"
    echo "  失败请求:       $FAILED_REQUESTS"
    echo "  成功率:         ${SUCCESS_RATE}%"
    echo ""
    echo "响应时间 (Parse 50 CQ Codes):"
    echo "  平均响应时间:   ${PARSE_AVG}ms"
    echo "  P95延迟:        ${PARSE_P95}ms"
    echo ""
    echo "响应时间 (Calculate Statistics):"
    echo "  平均响应时间:   ${STATS_AVG}ms"
    echo "  P95延迟:        ${STATS_P95}ms"
    echo ""
    echo "响应时间 (End-to-End API):"
    echo "  平均响应时间:   ${API_AVG}ms"
    echo "  P95延迟:        ${API_P95}ms"
    echo ""

    # 验证性能目标 (T121 requirements)
    echo "性能目标验证 (per constitution):"
    echo ""

    # 检查CQ码解析P95 < 10ms
    if (( $(echo "$PARSE_P95 < 10" | bc -l) )); then
        echo -e "  CQ码解析 P95 < 10ms:   ${GREEN}✓ 通过${NC} (${PARSE_P95}ms)"
        PARSE_PASS=1
    else
        echo -e "  CQ码解析 P95 < 10ms:   ${RED}✗ 失败${NC} (${PARSE_P95}ms)"
        PARSE_PASS=0
    fi

    # 检查统计计算P95 < 50ms
    if (( $(echo "$STATS_P95 < 50" | bc -l) )); then
        echo -e "  统计计算 P95 < 50ms:   ${GREEN}✓ 通过${NC} (${STATS_P95}ms)"
        STATS_PASS=1
    else
        echo -e "  统计计算 P95 < 50ms:   ${RED}✗ 失败${NC} (${STATS_P95}ms)"
        STATS_PASS=0
    fi

    # 检查API响应P95 < 200ms
    if (( $(echo "$API_P95 < 200" | bc -l) )); then
        echo -e "  API响应 P95 < 200ms:    ${GREEN}✓ 通过${NC} (${API_P95}ms)"
        API_PASS=1
    else
        echo -e "  API响应 P95 < 200ms:    ${RED}✗ 失败${NC} (${API_P95}ms)"
        API_PASS=0
    fi

    # 检查成功率
    if (( $(echo "$SUCCESS_RATE > 99" | bc -l) )); then
        echo -e "  成功率 > 99%:           ${GREEN}✓ 通过${NC} (${SUCCESS_RATE}%)"
        SUCCESS_PASS=1
    else
        echo -e "  成功率 > 99%:           ${RED}✗ 失败${NC} (${SUCCESS_RATE}%)"
        SUCCESS_PASS=0
    fi

    echo ""
    echo "详细报告:"
    echo "  JTL文件:        ${RESULTS_FILE}"
    echo "  HTML报告:       ${HTML_REPORT_DIR}/index.html"
    echo ""
    echo "查看HTML报告:"
    echo "  open ${HTML_REPORT_DIR}/index.html"
    echo ""

    # 总体结论
    if [ $PARSE_PASS -eq 1 ] && [ $STATS_PASS -eq 1 ] && [ $API_PASS -eq 1 ] && [ $SUCCESS_PASS -eq 1 ]; then
        echo -e "${GREEN}=========================================="
        echo "  ✓ T121 性能基准验证通过"
        echo "==========================================${NC}"
        echo ""
        return 0
    else
        echo -e "${RED}=========================================="
        echo "  ✗ T121 性能基准验证失败"
        echo "==========================================${NC}"
        echo ""
        return 1
    fi
}

# 主函数
main() {
    print_info "CQ码解析性能测试 (T121)"
    echo ""

    check_jmeter
    check_health
    prepare_results_dir
    run_test
    analyze_results

    EXIT_CODE=$?
    if [ $EXIT_CODE -eq 0 ]; then
        print_info "T121 测试完成并通过！"
    else
        print_error "T121 测试完成但未通过性能目标"
    fi
    exit $EXIT_CODE
}

# 执行主函数
main
