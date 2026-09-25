# syntax=docker/dockerfile:1
# One Dockerfile for every service:  docker build --build-arg SERVICE=order-service -t order-service .

FROM eclipse-temurin:21-jdk-alpine AS build
ARG SERVICE
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw
# sharing=locked: services build in parallel and must not write the shared Maven cache concurrently
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw -B -ntp -q -pl ${SERVICE} -am package -DskipTests
# Split the fat jar into layers so dependency layers are cached between builds
RUN cp ${SERVICE}/target/${SERVICE}-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
