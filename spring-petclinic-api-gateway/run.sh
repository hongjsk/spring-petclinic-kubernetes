#!/bin/sh
#docker run -d -p 8080:8080 hongjs/spring-petclinic-api-gateway:latest
docker run -p 8080:8080 -e MYSQL_HOSTINFO=docker.for.mac.localhost:3306 hongjs/spring-petclinic-api-gateway:latest