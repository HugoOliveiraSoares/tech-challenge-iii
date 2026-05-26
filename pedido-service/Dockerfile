FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY auth-service/pom.xml auth-service/pom.xml
COPY pedido-service/pom.xml pedido-service/pom.xml
COPY pagamento-service/pom.xml pagamento-service/pom.xml
RUN mvn dependency:resolve-plugins dependency:go-offline -pl pedido-service -am

COPY pedido-service/src pedido-service/src
RUN mvn clean package -pl pedido-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/pedido-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
