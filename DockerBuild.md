# Container Image 생성 가이드

Spring PetClinic Kuberentes는 사전 배포된 Docker Image를 이용하여 컨테이너를 생성합니다. 단순하게 배포하는 경우라면 상관 없지만 코드를 변경하고 적용하려면 Docker Image를 Build해야 합니다. 본 튜토리얼에서는 Docker Image를 생성하는 방법에 대해 학습합니다.

## 학습 목표

이 튜토리얼을 마치게 되면 다음과 같은 것을 할 수 있습니다:

* Maven을 이용한 Spring Boot 애플리케이션 빌드
* Docker CLI를 이용한 Docker Image 빌드 및 배포
* Maven Build Image를 이용한 Build Pipeline 구성 (선택, 확정 아님)

## 사전 준비 사항

1. [Docker 설치](https://docs.docker.com/install/)
1. [DockerHub 계정](https://hub.docker.com/)
1. [Apache Maven 설치](https://maven.apache.org/install.html)
1. [JDK(AdoptOpenJDK) 설치](https://adoptopenjdk.net/)


## 소요 시간

이 튜토리얼을 완료하기까지 대략 15분 정도가 소요됩니다.

## 단계

### DockerHub 저장소 확인

[DockerHub](https://hub.docker.com/) 에 접속해 Docker 사용자 ID로 정상 로그인이 되는지 확인합니다.

이제 Docker 사용자 ID를 pom.xml에 지정합니다. Build 명령으로 생성되는 이미지는 <DOCKER_HUB_ID>가 Container Registry Namespace가 되어 `<DOCKER_HUB_ID>/<IMAGE_REPOSITORY_NAME>:<TAG_NAME>` 형식으로 이미지 이름이 지정됩니다. 예를 들어 <DOCKER_HUB_ID>가  `hongjs`이고 이미지 이름이 `spring-petclinic-api-gateway`인 경우라면 `hongjs/spring-petclinic-api-gateway:latest`입니다.

프로젝트 디렉토리의 [pom.xml](pom.xml) 파일 중 `docker.image.prefix` 항목을 사용자 ID로 수정 합니다.

``` xml
...
<properties>
    <java.version>1.8</java.version>
    <assertj.version>3.11.1</assertj.version>

    <spring-boot.version>2.1.2.RELEASE</spring-boot.version>
    <spring-cloud.version>Greenwich.SR1</spring-cloud.version>

    <maven-surefire-plugin.version>2.22.0</maven-surefire-plugin.version>

    <docker.image.prefix>hongjs</docker.image.prefix>
    <docker.image.exposed.port>8080</docker.image.exposed.port>
    <docker.image.dockerfile.dir>${basedir}</docker.image.dockerfile.dir>
    <docker.image.dockerize.version>v0.6.1</docker.image.dockerize.version>
    <docker.plugin.version>1.2.0</docker.plugin.version>
</properties>
...
```

### 마이크로 서비스 빌드

각 마이크로 서비스는 Maven을 이용해 Build 할 수 있습니다.

``` bash
mvn clean install -PbuildDocker
```

빌드가 정상적으로 진행되면 각각 하위 모듈에 `target`이란 이름의 하위 디렉토리가 생성되고 Build 절차를 통해 JAR 파일과 함께 docker image가 생성됩니다.

예를 들어 API Gateway 마이크로 서비스의 경우 `spring-petclinic-api-gateway` 디렉토리 아래 `target\spring-petclinic-api-gateway-x.x.x.jar` 파일이 생성됩니다.

만약, 다음과 같이 JAVA_HOME이 정의되지 않았다고 나타나는 경우 JDK가 설치된 위치를 JAVA_HOME으로 지정해 주어야 합니다.

> The JAVA_HOME environment variable is not defined correctly
> This environment variable is needed to run this program
> NB: JAVA_HOME should point to a JDK not a JRE

MacOS, Linux 인 경우는 다음 명령어로 JAVA_HOME 환경 변수를 설정 할 수 있습니다.

``` bash
export JAVA_HOME=$(/usr/libexec/java_home)
```

오류가 없었다면 Docker CLI 명령으로 이미지가 생성되어 있는 것을 확인 할 수 있습니다.

``` bash
docker images
```


### 하위 모듈 독립 Build

한 번에 Build 하게 되는 경우가 아니면 다음과 같이 하위 모듈 디렉토리로 이동 후 해당 모듈만 build 할 수 있습니다.

``` bash
cd spring-petclinic-api-gateway
mvn clean install
```

그리고, Docker Image를 생성합니다.

``` bash
mvn docker:build -PbuildDocker
```

### Docker Image 배포

다음 명령을 실행하여 생성한 이미지를 DockerHub에 배포합니다. 이렇게 배포되는 이미지는 DockerHub에 Public으로 배포되며 다른 사람들에게 공유될 수 있습니다. 만약, Private 저장소를 사용하는 경우 외부로 노출되지 않으며 Kubernetes Cluster에서 해당 저장소로 접근하기 위한 권한이 필요하게 됩니다.

``` bash
cd spring-petclinic-api-gateway
mvn docker:push -PbuildDocker
```

혹은 Docker CLI를 직접 이용하는 방법으로 다음 명령을 이용 할 수도 있습니다.

``` bash
docker push <DOCKER_HUB_ID>/spring-petclinic-api-gateway:latest
```

## 맺음말

이 튜토리얼에서는 JDK, Maven 그리고 Docker CLI를 이용한 Spring PetCloud 이미지 생성 방법에 대해 알아보았습니다.