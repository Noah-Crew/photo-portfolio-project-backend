FROM azul/zulu-openjdk-alpine:17-latest

# libwebp-tools 설치하여 `cwebp` 바이너리 추가
RUN apk add --no-cache libwebp-tools
RUN which cwebp

# 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper 및 설정 파일 복사 (의존성 캐싱을 위해 먼저 복사)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Gradle Wrapper 권한 수정
RUN chmod +x ./gradlew

# 의존성 캐싱을 활용하여 종속성 먼저 다운로드
RUN ./gradlew dependencies --no-daemon

# 전체 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./gradlew build --no-daemon --exclude-task test
RUN ls -la ./build/libs/

# JAR 파일 복사 및 실행 설정
RUN cp ./build/libs/*.jar app.jar

# 포트 환경 변수와 Spring Profile 설정
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

# 컨테이너 포트 노출
EXPOSE ${PORT}

# 애플리케이션 시작 명령어
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-Dserver.port=${PORT}", "app.jar"]
