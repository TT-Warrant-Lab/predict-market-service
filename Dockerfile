FROM eclipse-temurin:25-jdk-jammy AS builder
WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
# Repositories cloned on Windows can retain CRLF here; Linux cannot execute
# a shell script whose shebang ends in \r.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN --mount=type=cache,id=predict-market-service-gradle,target=/root/.gradle,sharing=locked \
    ./gradlew dependencies --console=plain --no-daemon

COPY src ./src
RUN --mount=type=cache,id=predict-market-service-gradle,target=/root/.gradle,sharing=locked \
    ./gradlew bootJar -x test --console=plain --no-daemon

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone \
    && groupadd -r spring && useradd -r -g spring spring
COPY --from=builder --chown=spring:spring /app/build/libs/*.jar app.jar
USER spring
EXPOSE 8003
ENTRYPOINT ["java", "-jar", "app.jar"]
