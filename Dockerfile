FROM openjdk:17-oracle
LABEL maintainer="bakr458"
COPY file-service.jar service.jar
ENTRYPOINT ["java", "-jar", "service.jar"]