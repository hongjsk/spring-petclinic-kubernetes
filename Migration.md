# Spring PetClinic Microservice Kuberentes 적용

Spring PetClinic Kuberentes는 Spring Boot, Spring Cloud 그리고 Netflix OSS 서비스로 마이크로 서비스 아키텍쳐로 구성되어 있는 [Spring PetClinic Microservice](https://github.com/spring-petclinic/spring-petclinic-microservices)를 기반으로 작성되었습니다. 

Spring PetClinic Microservice의 경우 Local 서버에서 실행하거나 Docker를 이용할 수 있습니다. 그리고, 애플리케이션을 마이크로 서비스 아키텍쳐로 구성하기 위해 Netflix Eureka, Slueth, Zipkin 그리고 Hystrix 등을 이용합니다. 이런 서비스를 Kuberentes에 배포하여 사용 할 수 있지만 필요한 기능의 대부분을 Kubernetes 기본 기능에서 제공하므로 최대한 단순하게 Spring Boot만 남기고 나머지 모듈에 대한 의존성을 제거하여 실행합니다.

## 단계

### 하위 모듈 제거

[pom.xml](pom.xml) 파일에서 Kubernetes 클러스터에 배포할 4개 모듈만 남기고 나머지 모듈은 comment out 합니다.

* spring-petclinic-api-gateway
* spring-petclinic-customers-service
* spring-petclinic-vets-service
* spring-petclinic-visits-service

``` xml
<!-- <module>spring-petclinic-admin-server</module> -->
<module>spring-petclinic-customers-service</module>
<module>spring-petclinic-vets-service</module>
<module>spring-petclinic-visits-service</module>
<!-- <module>spring-petclinic-config-server</module> -->
<!-- <module>spring-petclinic-discovery-server</module> -->
<module>spring-petclinic-api-gateway</module>
<!-- <module>spring-petclinic-hystrix-dashboard</module> -->
```

### Spring Cloud 및 Netflix OSS 의존성 제거

각 하위 모듈에 포함된 pom.xml 파일에 있는 Spring Cloud와 Netflix OSS 모듈의 의존성을 제거합니다.

* [spring-petclinic-api-gateway/pom.xml](spring-petclinic-api-gateway/pom.xml)

``` xml
<!-- Spring Cloud -->
<!--
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
</dependency>
-->
```

이럴 경우 Build 시 Class를 찾을 수 없다고 나오므로 해당 클래스를 로딩하는 코드에서 Annotation을 comment out 합니다.

* [spring-petclinic-api-gateway/src/main/java/org/springframework/samples/petclinic/api/application/VisitsServiceClient.java](spring-petclinic-api-gateway/src/main/java/org/springframework/samples/petclinic/api/application/VisitsServiceClient.java)


``` java
//import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
...
public class VisitsServiceClient {

    private final RestTemplate loadBalancedRestTemplate;
...
    //@HystrixCommand(fallbackMethod = "emptyVisitsForPets")
    public Map<Integer, List<VisitDetails>> getVisitsForPets(final List<Integer> petIds) {
        UriComponentsBuilder builder = fromHttpUrl("http://visits-service/pets/visits")
            .queryParam("petId", joinIds(petIds));

        return loadBalancedRestTemplate.getForObject(builder.toUriString(), Visits.class)
            .getItems()
            .stream()
            .collect(groupingBy(VisitDetails::getPetId));
    }
...
```

### application.yml 적용

Spring PetClinic Microservice는 Spring Cloud Config Server를 이용하여 설정 정보를 사용하도록 되어 있습니다. 이 경우 애플리케이션 시작하면 bootstrap.yml 파일에 정의되어 있는 정보를 기반으로 Config Server에 접속을 하여 [GitHub에 공유된 설정 정보](https://github.com/spring-petclinic/spring-petclinic-microservices-config)을 로딩합니다. 본 튜토리얼에서는 Spring Cloud Config Server를 사용하지 않으므로 bootstrap.yml 파일의 정보가 로딩되지 않습니다. 그 대신 각 서비스별 설정과 공통 설정 값을 각각 application.yml과 application-common.yml에 입력하여 사용합니다.

application-common.yml 파일은 Config Server의 [application.yml](https://github.com/spring-petclinic/spring-petclinic-microservices-config/application.yml) 파일을 기반으로 합니다.

vets 서비스의 경우 [vets-service.yml](https://github.com/spring-petclinic/spring-petclinic-microservices-config/vet-service.yml)이 설정 파일이며, 이를 바탕으로 `application.yml` 파일을 작성하게 됩니다. 

Kubernetes 용으로 불필요한 항목은 삭제하고 공통항목으로 사용될 `application-common.yml` 파일을 로딩 할 수 있도록 `spring.profiles.include: 'common'` 항목을 추가합니다.

생성된 applicaiton.yml과 application-common.yml 파일은 다음과 같습니다.

* [spring-petclinic-vets-service/src/main/resources/application.yml](spring-petclinic-vets-service/src/main/resources/application.yml)


``` yml
spring.profiles.include: 'common'

vets:
  cache:
    ttl: 60
    heap-size: 100
```

앞서 공통 설정 정보 중 mysql 설정 정보와 같이 동적인 정보의 경우는 Container의 환경 변수로 전달하도록 합니다.

* [spring-petclinic-vets-service/src/main/resources/application-common.yml](spring-petclinic-vets-service/src/main/resources/application-common.yml)

``` yml
# COMMON APPLICATION PROPERTIES

# start services on random port by default
#server.port: 0
server.port: 8080

# embedded database init, supports mysql too trough the 'mysql' spring profile
petclinic.database: hsqldb
spring:
  datasource:
    schema: classpath*:db/hsqldb/schema.sql
    data: classpath*:db/hsqldb/data.sql
  sleuth:
    sampler:
      percentage: 1.0

# JPA
spring.jpa.hibernate.ddl-auto: none

# Spring Boot 1.5 makes actuator secure by default
management.security.enabled: false
# Enable all Actuators and not only the two available by default /health and /info starting Spring Boot 2.0
management.endpoints.web.exposure.include: "*"

# Temporary hack required by the Spring Boot 2 / Spring Cloud Finchley branch
# Waiting issue https://github.com/spring-projects/spring-boot/issues/13042
spring.cloud.refresh.refreshable: false


# Logging
logging.level.org.springframework: INFO

# Metrics
management:
  endpoint:
    metrics:
      enabled: true
    prometheus:
      enabled: true
  endpoints:
    web:
      exposure:
        include: '*'
  metrics:
    export:
      prometheus:
        enabled: true

---
spring:
  profiles: mysql
  datasource:
    schema: classpath*:db/mysql/schema.sql
    data: classpath*:db/mysql/data.sql
    url: jdbc:mysql://${MYSQL_HOSTINFO}/petclinic?useSSL=false
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
    initialization-mode: ALWAYS
```

### Kubernetes Secret 생성

MySQL의 사용자와 비밀번호는 사실 상 공개되어 있지만 이를 Plain Text로 적용할 수는 없습니다. 때문에 Kubernetes Object 중 Secret을 이용하여 정보를 저장합니다.

다음과 같이 파일에 정보를 출력합니다.

``` bash
echo -n "root" > username
echo -n "petclinic" > password
```

`kubectl create secret` 명령으로 `mysql-credential` Secret을 생성합니다. 

``` bash
kubectl create secret generic mysql-credential --from-file=username --from-file=password
```

참고로, Secret을 생성한 후 라면 `username`과 `password` 파일은 보안에 위협이 되므로 삭제해 주어야 합니다.

### Kubernetes ConfigMap 생성

MySQL 서버 연결 정보는 ConfigMap을 통해 전달됩니다. 다음과 같은 내용으로 `mysql-config`라는 이름으로 ConfigMap을 생성합니다. 이 정보는 Deployment를 작성 시 컨테이너에 환경 변수로 전달되도록 구성합니다.

``` yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mysql-config
  labels:
    app: spring-petclinic
data:
  hostinfo: mysql:3306
```

* [k8s/configmap.yaml](k8s/configmap.yaml)

### Kubernetes Deployment & Service 생성

각각의 마이크로 서비스는 컨테이너 이미지를 배포하는 Deployment 그리고 이를 외부에 노출하는 Service로 구성됩니다. 예를 들어 `vets-service`의 경우 `hongjs/spring-petclinic-vets-service:latest` 이미지를 기반으로 하며 Spring Boot Application이 사용하는 Port가 8080이므로 `containerPort`를 `8080`으로 설정합니다. 그리고, 이를 다른 마이크로 서비스에서 접근 할 수 있도록 `vets-service`란 이름의 Kuberntes Service로 등록하며 `80` 포트로 접근하게 됩니다.

각 마이크로 서비스별로 Deployment와 Service를 생성합니다. 상세 정보는 값은 다음 파일들을 참고 하시기 바랍니다.

* [k8s/api-gateway.yaml](k8s/api-gateway.yaml)
* [k8s/customers-service.yaml](k8s/customers-service.yaml)
* [k8s/vets-service.yaml](k8s/vets-service.yaml)
* [k8s/visits-service.yaml](k8s/visits-service.yaml)


## 맺음말

이 튜토리얼에서는 Spring PetClinic Microservice를 Kubernetes 용으로 변경하는 방법에 대해 학습했습니다.
