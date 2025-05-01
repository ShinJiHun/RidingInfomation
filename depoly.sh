#!/bin/bash

# === 설정 ===
PROJECT_ROOT="/Users/jihoon.shin/Desktop/development/project/RidingInfomation"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
LOCAL_JAR_PATH="$PROJECT_ROOT/build/libs/RidingInformation.jar"
REMOTE_USER="tho881"
REMOTE_HOST="34.172.162.148"
REMOTE_DIR="/home/tho881"
PRIVATE_KEY_PATH="/Users/jihoon.shin/Desktop/development/key/riding_key_nopass"
JAR_NAME=$(basename "$LOCAL_JAR_PATH")

# === 모드 선택 ===
MODE=$1

if [[ "$MODE" != "1" && "$MODE" != "2" && "$MODE" != "3" && "$MODE" != "4" ]]; then
  echo "❗ 사용법: ./deploy.sh [1|2|3|4]"
  echo "  1: 프론트 + 백엔드 빌드 후 배포"
  echo "  2: JAR만 전송 후 배포"
  echo "  3: 로컬 빌드 없이 서버에서 JAR 재시작"
  echo "  4: 컨테이너만 재시작 (JAR 교체 없음)"
  exit 1
fi

# === 1. 전체 빌드
if [[ "$MODE" == "1" ]]; then
  echo "🛠 프론트 + 백엔드 빌드 시작..."
  cd "$FRONTEND_DIR" || exit 1
  npm install
  npm run build

  cd "$PROJECT_ROOT" || exit 1
  ./gradlew clean build
fi

# === 2. JAR 및 docker-compose.yml 전송
if [[ "$MODE" == "1" || "$MODE" == "2" ]]; then
  echo "🚀 JAR 및 docker-compose.yml 전송 중..."
  scp -i "$PRIVATE_KEY_PATH" "$LOCAL_JAR_PATH" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR"
  scp -i "$PRIVATE_KEY_PATH" "$PROJECT_ROOT/docker-compose.yml" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR"
fi

# === 3. 컨테이너 재배포 (Spring Boot만 재시작, DB 유지)
if [[ "$MODE" == "1" || "$MODE" == "2" || "$MODE" == "3" ]]; then
  echo "🔄 서버에서 docker-compose 실행 및 용량 정리..."
  ssh -i "$PRIVATE_KEY_PATH" "$REMOTE_USER@$REMOTE_HOST" bash << EOF
    cd /home/tho881

    echo "📁 NAS 디렉토리 생성 확인"
    mkdir -p /home/tho881/NAS/fit /home/tho881/NAS/gpx /home/tho881/NAS/tcx

    echo "🧹 Docker 불필요 리소스 정리"
    docker container prune -f
    docker image prune -a -f
    docker volume prune -f
    docker builder prune -f

    echo "🧹 오래된 /tmp 파일 정리"
    find /tmp -type f -atime +1 -delete

    echo "📊 디스크 용량 사용 현황:"
    df -h

    echo "🛑 springboot 컨테이너만 중지 및 제거"
    docker-compose stop springboot
    docker-compose rm -f springboot

    echo "🚀 springboot 컨테이너만 재배포"
    docker-compose up -d --build springboot

    echo "📄 riding-springboot 로그 확인 중..."
    sleep 2
    docker logs -f \$(docker ps --filter "ancestor=riding-springboot-image" --format "{{.Names}}" | head -n 1)
EOF
fi

# === 4. 컨테이너만 재시작
if [[ "$MODE" == "4" ]]; then
  echo "🔁 컨테이너 재시작 중..."
  ssh -i "$PRIVATE_KEY_PATH" "$REMOTE_USER@$REMOTE_HOST" bash << EOF
    docker-compose restart springboot
    sleep 2
    docker logs -f \$(docker ps --filter "ancestor=riding-springboot-image" --format "{{.Names}}" | head -n 1)
EOF
fi
