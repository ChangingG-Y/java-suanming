#!/usr/bin/env bash
# 后端发布脚本：拉代码 -> mvn 构建 -> 停服 -> 替换 jar -> 启服 -> 检查状态。
# 对应《算命项目部署指南.md》「日常运维：更新后端」那一节，在服务器上
# /opt/portal/source/java-suanming 目录里直接跑 ./deploy.sh 即可。
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_DEST="/opt/portal/app/suanming-srv/app.jar"
SERVICE_NAME="suanming-srv"

cd "$REPO_DIR"

echo "==> [1/6] 检查工作区是否干净"
if [ -n "$(git status --porcelain)" ]; then
  echo "工作区有未提交的改动，先处理干净再部署（不要在服务器上直接改代码）：" >&2
  git status --short
  exit 1
fi

echo "==> [2/6] 拉取最新代码"
git pull origin main

echo "==> [3/6] 构建后端"
if [ -x ./mvnw ]; then
  ./mvnw clean package -DskipTests
else
  mvn clean package -DskipTests
fi

JAR_PATH="$(ls -t sjfy-app/target/*.jar 2>/dev/null | head -n1 || true)"
if [ -z "$JAR_PATH" ]; then
  echo "找不到构建产物（sjfy-app/target/*.jar），终止部署" >&2
  exit 1
fi
echo "    构建产物：$JAR_PATH"

echo "==> [4/6] 备份上一版 jar 并替换"
if [ -f "$JAR_DEST" ]; then
  cp "$JAR_DEST" "${JAR_DEST}.bak"
  echo "    已备份旧版本到 ${JAR_DEST}.bak"
fi
cp "$JAR_PATH" "$JAR_DEST"

echo "==> [5/6] 重启服务"
sudo systemctl stop "$SERVICE_NAME"
sudo systemctl start "$SERVICE_NAME"
sleep 3

echo "==> [6/6] 检查服务状态"
if ! sudo systemctl is-active --quiet "$SERVICE_NAME"; then
  echo "服务没有正常启动，最近日志：" >&2
  sudo journalctl -u "$SERVICE_NAME" -n 50 --no-pager
  echo "如需回滚：cp ${JAR_DEST}.bak $JAR_DEST && sudo systemctl restart $SERVICE_NAME" >&2
  exit 1
fi

sudo systemctl status "$SERVICE_NAME" --no-pager
echo "==> 后端部署完成"
