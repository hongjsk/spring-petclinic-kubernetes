FROM java:8
VOLUME /tmp
ADD target/spring-petclinic-vets-service-1.5.9.jar /app.jar
#ENV SPRING_PROFILES_ACTIVE docker
RUN bash -c 'touch /app.jar'

# Expose
EXPOSE 8080

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]