FROM openjdk:21

ARG FILE_JAR=target/*.jar

ADD ${FILE_JAR} eureka-service.jar

ENTRYPOINT ["java", "-jar", "eureka-service.jar"]

EXPOSE 8761

