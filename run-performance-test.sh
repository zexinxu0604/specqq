#!/bin/bash

# 聊天机器人路由系统性能测试脚本
# 使用方法: ./run-performance-test.sh [options]

set -e

# 默认配置
HOST="localhost"
PORT="8080"
CONCURRENT_USERS="100"
RAMP_UP_PERIOD="10"
LOOP_COUNT="10"
JMETER_HOME="${JMETER_HOME:-/usr/local/bin}"
TEST_PLAN="src/test/resources/jmeter/chatbot-performance-test.jmx"
RESULTS_DIR="test-results"
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

# 显示帮助信息
show_help() {
    cat << EOF
聊天机器人路由系统性能测试脚本

使用方法:
    ./run-performance-test.sh [options]

选项:
    -h, --host HOST                 目标主机 (默认: localhost)
    -p, --port PORT                 目标端口 (默认: 8080)
    -u, --users USERS               并发用户数 (默认: 100)
    -r, --ramp-up SECONDS           爬坡时间(秒) (默认: 10)
    -l, --loops COUNT               循环次数 (默认: 10)
    -j, --jmeter-home PATH          JMeter安装路径 (默认: /usr/local/bin)
    --skip-health-check             跳过健康检查
    --skip-cleanup                  跳过结果清理
    --help                          显示此帮助信息

示例:
    # 使用默认配置运行测试
    ./run-performance-test.sh

    # 自定义并发用户数和循环次数
    ./run-performance-test.sh -u 200 -l 20

    # 测试远程服务器
    ./run-performance-test.sh -h prod-server.example.com -p 8080

EOF
}

# 解析命令行参数
SKIP_HEALTH_CHECK=false
SKIP_CLEANUP=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--host)
            HOST="$2"
            shift 2
            ;;
        -p|--port)
            PORT="$2"
            shift 2
            ;;
        -u|--users)
            CONCURRENT_USERS="$2"
            shift 2
            ;;
        -r|--ramp-up)
            RAMP_UP_PERIOD="$2"
            shift 2
            ;;
        -l|--loops)
            LOOP_COUNT="$2"
            shift 2
            ;;
        -j|--jmeter-home)
            JMETER_HOME="$2"
            shift 2
            ;;
        --skip-health-check)
            SKIP_HEALTH_CHECK=true
            shift
            ;;
        --skip-cleanup)
            SKIP_CLEANUP=true
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            print_error "未知参数: $1"
            show_help
            exit 1
            ;;
    esac
done

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
    if [ "$SKIP_HEALTH_CHECK" = true ]; then
        print_warn "跳过健康检查"
        return 0
    fi

    print_info "检查应用健康状态: http://${HOST}:${PORT}/actuator/health"

    HEALTH_URL="http://${HOST}:${PORT}/actuator/health"

    if ! curl -f -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" | grep -q "200"; then
        print_error "应用健康检查失败，请确保应用正在运行"
        print_info "启动应用: java -jar target/chatbot-router.jar"
        exit 1
    fi

    print_info "应用健康检查通过 ✓"
}

# 创建结果目录
prepare_results_dir() {
    print_info "准备结果目录: ${RESULTS_DIR}"

    mkdir -p "${RESULTS_DIR}"

    # 清理旧结果（保留最近5次）
    if [ "$SKIP_CLEANUP" = false ]; then
        print_info "清理旧结果（保留最近5次）..."
        ls -t "${RESULTS_DIR}"/results_*.jtl 2>/dev/null | tail -n +6 | xargs -r rm -f
        ls -td "${RESULTS_DIR}"/html-report-* 2>/dev/null | tail -n +6 | xargs -r rm -rf
    fi
}

# 运行JMeter测试
run_test() {
    print_info "开始性能测试..."
    print_info "配置参数:"
    print_info "  目标地址: http://${HOST}:${PORT}"
    print_info "  并发用户: ${CONCURRENT_USERS}"
    print_info "  爬坡时间: ${RAMP_UP_PERIOD}秒"
    print_info "  循环次数: ${LOOP_COUNT}"
    print_info "  总请求数: $((CONCURRENT_USERS * LOOP_COUNT))"

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

    AVG_RESPONSE_TIME=$(awk -F',' 'NR>1 {sum+=$2; count++} END {printf "%.2f", sum/count}' "$RESULTS_FILE")

    # 计算P95延迟
    P95_LATENCY=$(awk -F',' 'NR>1 {print $2}' "$RESULTS_FILE" | sort -n | awk '{a[NR]=$1} END {printf "%.2f", a[int(NR*0.95)]}')

    # 计算吞吐量
    START_TIME=$(awk -F',' 'NR==2 {print $1}' "$RESULTS_FILE")
    END_TIME=$(awk -F',' 'END {print $1}' "$RESULTS_FILE")
    DURATION=$(awk "BEGIN {printf \"%.2f\", ($END_TIME - $START_TIME) / 1000}")
    THROUGHPUT=$(awk "BEGIN {printf \"%.2f\", $TOTAL_REQUESTS / $DURATION}")

    echo ""
    echo "=========================================="
    echo "          性能测试结果汇总"
    echo "=========================================="
    echo ""
    echo "请求统计:"
    echo "  总请求数:       $TOTAL_REQUESTS"
    echo "  成功请求:       $SUCCESS_REQUESTS"
    echo "  失败请求:       $FAILED_REQUESTS"
    echo "  成功率:         ${SUCCESS_RATE}%"
    echo ""
    echo "响应时间:"
    echo "  平均响应时间:   ${AVG_RESPONSE_TIME}ms"
    echo "  P95延迟:        ${P95_LATENCY}ms"
    echo ""
    echo "吞吐量:"
    echo "  测试时长:       ${DURATION}s"
    echo "  吞吐量:         ${THROUGHPUT} req/s"
    echo ""

    # 验证性能目标
    echo "性能目标验证:"

    # 检查P95延迟
    if (( $(echo "$P95_LATENCY < 3000" | bc -l) )); then
        echo -e "  P95延迟 < 3s:   ${GREEN}✓ 通过${NC} (${P95_LATENCY}ms)"
    else
        echo -e "  P95延迟 < 3s:   ${RED}✗ 失败${NC} (${P95_LATENCY}ms)"
    fi

    # 检查成功率
    if (( $(echo "$SUCCESS_RATE > 99" | bc -l) )); then
        echo -e "  成功率 > 99%:   ${GREEN}✓ 通过${NC} (${SUCCESS_RATE}%)"
    else
        echo -e "  成功率 > 99%:   ${RED}✗ 失败${NC} (${SUCCESS_RATE}%)"
    fi

    # 检查吞吐量
    if (( $(echo "$THROUGHPUT > 30" | bc -l) )); then
        echo -e "  吞吐量 > 30:    ${GREEN}✓ 通过${NC} (${THROUGHPUT} req/s)"
    else
        echo -e "  吞吐量 > 30:    ${RED}✗ 失败${NC} (${THROUGHPUT} req/s)"
    fi

    echo ""
    echo "详细报告:"
    echo "  JTL文件:        ${RESULTS_FILE}"
    echo "  HTML报告:       ${HTML_REPORT_DIR}/index.html"
    echo ""
    echo "查看HTML报告:"
    echo "  open ${HTML_REPORT_DIR}/index.html"
    echo ""
    echo "=========================================="
}

# 主函数
main() {
    print_info "聊天机器人路由系统性能测试"
    echo ""

    check_jmeter
    check_health
    prepare_results_dir
    run_test
    analyze_results

    print_info "测试完成！"
}

# 执行主函数
main
