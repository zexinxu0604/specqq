#!/bin/bash

# 快速启动脚本
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "使用 Java 版本:"
$JAVA_HOME/bin/java -version

echo ""
echo "启动应用..."
cd /Users/zexinxu/IdeaProjects/specqq
$JAVA_HOME/bin/java -jar target/chatbot-router.jar --spring.profiles.active=dev
