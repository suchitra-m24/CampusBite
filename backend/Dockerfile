# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata wget \
    && addgroup -S campusbite \
    && adduser -S -G campusbite campusbite
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN chown -R campusbite:campusbite /app
USER campusbite
ENV TZ=Asia/Kolkata
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
