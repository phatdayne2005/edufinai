FROM openjdk:21

ARG FILE_JAR=target/*.jar

ADD ${FILE_JAR} gamification-service.jar

ENTRYPOINT ["java", "-jar", "gamification-service.jar"]

EXPOSE 8203