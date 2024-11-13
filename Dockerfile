FROM azul/zulu-openjdk-alpine:17-latest

# libwebp-tools 설치하여 `cwebp` 바이너리 추가
RUN apk add --no-cache libwebp-tools
RUN which cwebp

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN sed -i 's/\r$//' gradlew
RUN chmod +x ./gradlew

ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
ENV PORT=8080

# Spring Boot 애플리케이션 빌드
RUN ./gradlew build --exclude-task test
RUN ls -la ./build/libs/

RUN cp ./build/libs/portfolio_project-0.0.1-SNAPSHOT.jar app.jar

EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "-Dserver.port=${PORT}", "app.jar"]
