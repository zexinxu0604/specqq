#!/bin/bash

# 设置JDK 17环境
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "Using Java version:"
java -version

echo "Starting Spring Boot application..."
cd /Users/zexinxu/IdeaProjects/specqq
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
