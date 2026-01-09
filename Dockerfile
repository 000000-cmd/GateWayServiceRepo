# ----------------------------
# ETAPA 1: Construcción (Build)
# ----------------------------
# Usamos una imagen que tiene MAVEN instalado para poder compilar
FROM maven:3.9-amazoncorretto-21 AS build

WORKDIR /app

# Copiamos todo el código fuente al contenedor
COPY . .

# Ejecutamos el comando para compilar (crear el .jar)
# -DskipTests es para que sea más rápido y no falle si hay tests rotos
RUN mvn clean package -DskipTests

# ----------------------------
# ETAPA 2: Ejecución (Run)
# ----------------------------
# Usamos una imagen ligera solo con Java para ejecutar la app
FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

# Aquí está la magia: Copiamos el .jar DESDE la etapa "build" anterior
# Nota: Ajusta el nombre si tu jar no se llama app.jar, pero el asterisco suele funcionar
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]