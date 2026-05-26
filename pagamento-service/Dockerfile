FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY auth-service/pom.xml auth-service/pom.xml
COPY pedido-service/pom.xml pedido-service/pom.xml
COPY pagamento-service/pom.xml pagamento-service/pom.xml
RUN mvn dependency:resolve-plugins dependency:go-offline -pl pagamento-service -am

COPY pagamento-service/src pagamento-service/src
RUN mvn clean package -pl pagamento-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/pagamento-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
