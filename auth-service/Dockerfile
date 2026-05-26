FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY auth-service/pom.xml auth-service/pom.xml
COPY pedido-service/pom.xml pedido-service/pom.xml
COPY pagamento-service/pom.xml pagamento-service/pom.xml
RUN mvn dependency:resolve-plugins dependency:go-offline -pl auth-service -am

COPY auth-service/src auth-service/src
RUN mvn clean package -pl auth-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
