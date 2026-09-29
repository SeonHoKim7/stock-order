#!/usr/bin/env bash
# 사용법: ./deploy.sh <이미지 태그(커밋 해시)>
# Docker Hub의 이미지를 받아 앱 컨테이너를 교체하고, 로그인 페이지 응답으로 기동을 확인한다.
# 빌드는 하지 않는다.
# t3.micro 1GB에서 Gradle 빌드는 메모리 부족 위험 → 빌드는 Codespaces에서
set -euo pipefail

cd "$(dirname "$0")"

TAG="${1:-}"
HEALTH_URL="http://localhost/login"
TIMEOUT_SEC=120

if [[ -z "$TAG" ]]; then
  echo "사용법: ./deploy.sh <이미지 태그>" >&2
  exit 1
fi
# 태그는 sed 치환에 그대로 들어가므로 허용 문자를 제한한다.
if [[ ! "$TAG" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "잘못된 태그 형식입니다: $TAG" >&2
  exit 1
fi
if [[ ! -f .env ]]; then
  echo ".env 파일이 없습니다." >&2
  exit 1
fi

PREV_TAG="$(grep '^APP_TAG=' .env | cut -d= -f2 || true)"
echo "이전 태그: ${PREV_TAG:-없음} -> 새 태그: $TAG"

# .env를 바꾸기 전에 먼저 받아 본다. 태그 오타면 여기서 멈추고 .env는 그대로 남는다.
APP_TAG="$TAG" docker compose pull app

# 나중에 누가 그냥 'docker compose up -d'를 쳐도 같은 버전이 뜨도록 .env에 기록한다.
if grep -q '^APP_TAG=' .env; then
  sed -i "s/^APP_TAG=.*/APP_TAG=$TAG/" .env
else
  echo "APP_TAG=$TAG" >> .env
fi

docker compose up -d

echo "기동 확인 중 (최대 ${TIMEOUT_SEC}초)..."
for ((waited = 0; waited < TIMEOUT_SEC; waited += 5)); do
  if curl -fsS -o /dev/null "$HEALTH_URL"; then
    echo "배포 성공: $TAG"
    docker image prune -f
    exit 0
  fi
  sleep 5
done

echo "기동 확인 실패. 최근 앱 로그:" >&2
docker compose logs --tail=100 app >&2
echo "되돌리려면: ./deploy.sh ${PREV_TAG}" >&2
exit 1