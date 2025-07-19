# For Java 17, try this
FROM eclipse-temurin:17.0.9_9-jre

# Refer to Maven build -> finalName
ARG JAR_FILE=build/libs/pos-mcp-server-0.0.1-SNAPSHOT.jar

# cd /opt/app
WORKDIR /opt/app

# cp build/libs/pos-mcp-server-0.0.1-SNAPSHOT.jar /opt/app/app.jar
COPY ${JAR_FILE} app.jar

# java -jar /opt/app/app.jar
ENTRYPOINT ["java","-jar","app.jar"]