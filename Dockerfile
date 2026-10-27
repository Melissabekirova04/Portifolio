FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
ENV MAVEN_OPTS="-Xmx384m"
RUN mvn -B -DskipTests package dependency:copy-dependencies

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/dependency ./lib
EXPOSE 7070
CMD ["java", "-Xmx384m", "-cp", "classes:lib/*", "org.example.Main"]
