# --- Etap budowania: kompilacja i spakowanie aplikacji ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Najpierw pliki potrzebne do pobrania zaleznosci (lepszy cache warstw Dockera)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

# Zrodla i build (bez testow - te uruchamiamy w CI/lokalnie)
COPY src/ src/
RUN ./mvnw -q -B clean package -DskipTests

# --- Etap uruchomienia: sam JRE + zbudowany artefakt ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.war app.war

# Port informacyjnie; aplikacja i tak slucha na $PORT (patrz application.properties)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.war"]
