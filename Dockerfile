# Usa uma imagem oficial do OpenJDK 21 como base.
FROM eclipse-temurin:21-jdk-jammy

# Define o diretório de trabalho dentro do contêiner.
WORKDIR /app

# Copia os arquivos do Maven Wrapper primeiro.
COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .

# Copia o código-fonte do projeto para dentro do contêiner.
COPY src ./src

# Executa o comando do Maven para construir o projeto e gerar o arquivo .jar.
RUN ./mvnw package -B -DskipTests

# Expõe a porta 3003
EXPOSE 3003

# Define o comando que será executado quando o contêiner iniciar.
ENTRYPOINT ["java", "-jar", "target/parking-management-0.0.1-SNAPSHOT.jar"]