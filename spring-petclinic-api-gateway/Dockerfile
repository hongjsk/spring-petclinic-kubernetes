FROM openjdk:8-jre-alpine
VOLUME /tmp
ADD target/spring-petclinic-api-gateway-1.5.9.jar /app.jar
#ENV SPRING_PROFILES_ACTIVE docker
RUN sh -c 'touch /app.jar'

# Expose
EXPOSE 8080

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]