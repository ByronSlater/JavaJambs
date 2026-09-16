# ---- Build stage: compiles the app and runs the Tailwind/htmx npm steps (see pom.xml exec-maven-plugin) ----
FROM eclipse-temurin:21-jdk-jammy AS build

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates gnupg && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y --no-install-recommends nodejs && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

COPY package.json package-lock.json ./
RUN npm ci

COPY tailwind.config.js ./
COPY src ./src

RUN ./mvnw -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app
COPY --from=build /app/target/cher-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
