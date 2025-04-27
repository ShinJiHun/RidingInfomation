#!/bin/bash

# === 설정 ===
LOCAL_JAR_PATH="/Users/jihoon.shin/Desktop/development/project/RidingInfomation/build/libs/RidingInformation.jar"
REMOTE_USER="tho881"
REMOTE_HOST="34.172.162.148"
REMOTE_DIR="/home/tho881"
PRIVATE_KEY_PATH="/Users/jihoon.shin/Desktop/development/key/riding_key_nopass"

# JAR 파일 이름과 전체 경로 변수 지정
JAR_NAME=$(basename "$LOCAL_JAR_PATH")
REMOTE_FULL_PATH="$REMOTE_DIR/$JAR_NAME"

# === JAR 파일 GCP로 전송 ===
echo "🚀 GCP로 JAR 파일 전송 중..."
scp -i "$PRIVATE_KEY_PATH" "$LOCAL_JAR_PATH" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR"

# ======== 원격 실행 ========
echo "🔄 서버에서 기존 프로세스 종료 중..."
ssh -i "$PRIVATE_KEY_PATH" "$REMOTE_USER@$REMOTE_HOST" "pkill -f '$JAR_NAME' || true"

echo "🚦 새 JAR 실행 중..."
ssh -i "$PRIVATE_KEY_PATH" "$REMOTE_USER@$REMOTE_HOST" "
  nohup java -jar '$REMOTE_FULL_PATH' > '$REMOTE_DIR/app.log' 2>&1 &
  sleep 2
  tail -f '$REMOTE_DIR/app.log'
  "