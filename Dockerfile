FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Установка необходимых пакетов
RUN apk add --no-cache bash

# Копирование собранного JAR
COPY build/libs/rapidcoderForwardBot-*.jar app.jar

# Создание пользователя для безопасности
RUN addgroup -S bot && adduser -S bot -G bot
USER bot

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]