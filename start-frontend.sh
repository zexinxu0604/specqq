#!/bin/bash

# Chatbot Router 前端开发服务器启动脚本

set -e

echo "=========================================="
echo "  Chatbot Router - 前端开发服务器"
echo "=========================================="
echo ""

cd "$(dirname "$0")/frontend"

echo "检查依赖..."
if [ ! -d "node_modules" ]; then
    echo "安装依赖..."
    npm install
fi

echo ""
echo "启动开发服务器..."
echo "访问地址: http://localhost:5173"
echo ""

npm run dev
