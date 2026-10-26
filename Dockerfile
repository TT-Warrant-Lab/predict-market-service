FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --console=plain --no-daemon

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar -x test --console=plain --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone \
    && groupadd -r spring && useradd -r -g spring spring
COPY --from=builder --chown=spring:spring /app/build/libs/*.jar app.jar
USER spring
EXPOSE 8003
ENTRYPOINT ["java", "-jar", "app.jar"]
