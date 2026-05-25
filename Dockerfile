# Stage 1: Compile C++ for Linux
FROM --platform=linux/amd64 gcc:13 AS cpp-builder
WORKDIR /app
COPY PPNNI.Mock/ .
RUN make all

# Stage 2: Build Kotlin JAR
FROM --platform=linux/amd64 gradle:8-jdk21 AS kotlin-builder
WORKDIR /app
COPY ServerManager/ .
RUN ./gradlew shadowJar -x test

# Stage 3: Final Production Image
FROM --platform=linux/amd64 openjdk:21-slim
WORKDIR /app

# Install C++ runtime dependencies
RUN apt-get update && apt-get install -y libstdc++6 && rm -rf /var/lib/apt/lists/*

# Copy the C++ project (The Kotlin API looks for ../PPNNI.Mock)
COPY --from=cpp-builder /app /app/PPNNI.Mock
RUN chmod +x /app/PPNNI.Mock/build/mock_server /app/PPNNI.Mock/scripts/*.sh

# Copy the Kotlin JAR
COPY --from=kotlin-builder /app/build/libs/*-all.jar server-manager.jar

# Copy the config
COPY ServerManager/config.json .

# EXTREMELY IMPORTANT: Fix the config.json for Docker
# 1. Set IP to 0.0.0.0 so it's reachable outside the container
# 2. Ensure model_dir points to the correct relative path
RUN sed -i 's/"hostIp": ".*"/"hostIp": "0.0.0.0"/' config.json && \
    sed -i 's|"model_dir": ".*"|"model_dir": "./PPNNI.Mock"|' config.json

# Expose API port and Mock server port range
EXPOSE 8080
EXPOSE 9000-9200

ENTRYPOINT ["java", "-jar", "server-manager.jar"]