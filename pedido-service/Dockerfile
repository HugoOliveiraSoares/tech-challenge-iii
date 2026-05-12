# Estágio 1: compila o módulo pedido-service e dependências do parent
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -pl pedido-service -am -DskipTests

# Estágio 2: imagem enxuta só com o JAR
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/pedido-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
