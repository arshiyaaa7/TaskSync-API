########################################
# Stage 1 - build with Maven
########################################
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# speed up rebuilds: copy mvn wrapper & metadata then fetch dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN mvn -B -f pom.xml -q -DskipTests dependency:go-offline

# copy sources and build
COPY src ./src
RUN mvn -B -f pom.xml clean package -DskipTests

########################################
# Stage 2 - runtime
########################################
# use JRE (smaller than JDK)
FROM eclipse-temurin:17-jre-jammy

# create non-root user for safety
RUN addgroup --system app && adduser --system --ingroup app app

WORKDIR /app

# copy built jar
COPY --from=build /app/target/*.jar app.jar

# runtime tunables
ENV JAVA_OPTS=""

# expose default port (Render provides $PORT at runtime)
EXPOSE 8080

# run as non-root
USER app

# entrypoint uses sh so JAVA_OPTS env var expands
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

# optional: lightweight healthcheck (uncomment if you enable /actuator/health)
# HEALTHCHECK --interval=30s --timeout=3s --start-period=10s CMD curl -f http://localhost:8080/actuator/health || exit 1
