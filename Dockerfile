# ── Étape 1 : Build ──────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copier pom.xml et télécharger les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source et builder
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Étape 2 : Runtime ─────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copier le JAR buildé
COPY --from=builder /app/target/*.jar app.jar

# Port exposé
EXPOSE 8080

# Variables d'environnement par défaut
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport"

# Lancer l'application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]