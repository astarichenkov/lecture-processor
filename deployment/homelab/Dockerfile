# Builds the private-ai-chat service. Build context is the repository root.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY backend/pom.xml backend/pom.xml
COPY private-ai-chat/pom.xml private-ai-chat/pom.xml
RUN ./mvnw -q -N wrapper:wrapper -Dmaven=3.9.11 || true
COPY private-ai-chat private-ai-chat
RUN ./mvnw -q -pl private-ai-chat -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/private-ai-chat/target/private-ai-chat-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
