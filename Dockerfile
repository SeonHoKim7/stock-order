# 1단계: 빌드 (JDK 17로 실행 가능한 jar 생성)
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Gradle 파일 먼저 복사 → 소스 변경 시에도 의존성 레이어 캐시 재사용
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# 소스 복사 후 패키징 (테스트 제외)
COPY src src
RUN ./gradlew bootJar --no-daemon

# 2단계: 실행 (JRE만 담아 경량화)
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# 빌드 결과물 jar만 가져옴
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]