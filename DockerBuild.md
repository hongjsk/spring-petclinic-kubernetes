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

### 마이크로 서비스 빌드

각 마이크로 서비스는 다음과 같은 디렉토리에서 Maven을 이용해 Build 할 수 있습니다.

빌드가 정상적으로 진행되면 각각 `target`이란 이름의 하위 디렉토리가 생성되고 Build 절차를 통해 JAR 파일이 생성됩니다.

예를 들어 API 마이크로 서비스의 경우 `spring-petclinic-api-gateway` 디렉토리로 이동 후 maven 명령을 실행 할 수 있습니다.

``` bash
cd spring-petclinic-api-gateway
mvn clean
mvn install
```

만약, 다음과 같이 JAVA_HOME이 정의되지 않았다고 나타나는 경우 JDK가 설치된 위치를 JAVA_HOME으로 지정해 주어야 합니다.

> The JAVA_HOME environment variable is not defined correctly
> This environment variable is needed to run this program
> NB: JAVA_HOME should point to a JDK not a JRE

MacOS, Linux 인 경우는 다음 명령어로 JAVA_HOME 환경 변수를 설정 할 수 있습니다.

``` bash
export JAVA_HOME=$(/usr/libexec/java_home)
```

Maven으로 빌드가 오류 없이 진행되었다면 `target` 디렉토리에 `spring-petclinic-api-gateway-1.5.9.jar` 파일이 생성된 것을 확인 할 수 있습니다.

다른 마이크로 서비스도 마찬가지로 각 마이크로 서비스 디렉토리에서 maven으로 빌드를 하면 jar 파일이 생성됩니다.

### Docker Image 생성

이제 Docker CLI를 이용하여 Docker 이미지를 생성합니다. 이미지는 Docker Hub ID가 Container Registry Namespace가 되어 이미지는 `<DOCKER_HUB_ID>/<IMAGE_REPOSITORY_NAME>:<TAG_NAME>`가 됩니다. 예를 들어 ID가 hongjs이고 이미지 이름이 인 경우라면 `hongjs/spring-petclinic-api-gateway:openjdk8`입니다.

앞서 마이크로 서비스와 같이 해당 디렉토리에서 다음과 같은 명령을 실행합니다.

``` bash
docker build . -t <DOCKER_HUB_ID>/spring-petclinic-api-gateway:openjdk8
```
오류가 없었다면 다음 명령으로 이미지가 생성되어 있는 것을 확인 할 수 있습니다.

``` bash
docker images <DOCKER_HUB_ID>/spring-petclinic-api-gateway
```

### Docker Image 배포

다음 명령을 실행하여 생성한 이미지를 DockerHub에 배포합니다. 이렇게 배포되는 이미지는 DockerHub에 Public으로 배포되며 다른 사람들에게 공유될 수 있습니다. 만약, Private 저장소를 사용하는 경우 외부로 노출되지 않으며 Kubernetes Cluster에서 해당 저장소로 접근하기 위한 권한이 필요하게 됩니다.

``` bash
docker push <DOCKER_HUB_ID>/spring-petclinic-api-gateway:openjdk8
```

### 마이크로 서비스별 Kuberenetes Deployment YAML 파일 변경하기

아래 파일들을 Spring PetClinic용 Deployment를 생성합니다.

* k8s/api.yaml
* k8s/customers.yaml
* k8s/vets.yaml
* k8s/visits.yaml

 이 파일들에 정의된 `image` 항목 값을 변경합니다.

``` yaml
apiVersion: extensions/v1beta1
kind: Deployment
...
    spec:
      containers:
        - image: <DOCKER_HUB_ID>/spring-petclinic-api-gateway:latest
...
```

### 변경된 마이크로 서비스별 Kuberenetes Deployment YAML 배포 하기

다음과 명령으로 마이크로 서비스를 배포합니다.

``` bash
kubectl apply -f k8s/api.yaml
kubectl apply -f k8s/customers.yaml
kubectl apply -f k8s/vets.yaml
kubectl apply -f k8s/visits.yaml
```

만약, 기존에 배포된 Deployment가 있고 변경사항이 있는 경우 새로운 정보로 업데이트가 진행됩니다. `kubectl get pods` 명령으로 기존 배포된 pod가 삭제되고 업데이트된 이미지로 새로운 pod가 생성되는지 확인 합니다.

기존에 배포된 것이 없으면 신규로 생성됩니다.


## 맺음말

이 튜토리얼에서는 JDK, Maven 그리고 Docker CLI를 이용한 Spring PetCloud 이미지 생성 방법에 대해 알아보았습니다.