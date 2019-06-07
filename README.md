# Kubernetes에서 실행되는 Spring PetClinic Microservice 예제

이 애플리케이션의 코드는 [Spring PetClinic Microservices version](https://github.com/spring-petclinic/spring-petclinic-microservices)을 기반으로 작성되었습다. [Spring Cloud Netflix](https://github.com/spring-cloud/spring-cloud-netflix)를 이용하여 구성된 마이크로 서비스를 Kubernetes에서 실행하도록 몇 가지 의존성과 코드를 제거할 뿐 최대한 원본 코드를 유지한 형태로 구성하는 것을 목표로 구성되었습니다. 

본 문서는 Kuberentes에서 실행 및 배포를 위한 내용에 대한 것을 설명하며  애플리케이션 마이그레이션에 대한 정보는 [Migration.md](Migration.md)를 참고 하시기 바랍니다.

애플리케이션 빌드 및 배포는 다음과 같은 단계로 진행 합니다.

* [Step1. 준비 사항](#준비-사항)
* [Step2. 애플리케이션 배포](#애플리케이션-배포)
* [Step3. 웹 브라우저로 확인](#웹-브라우저로-확인)
* [참고. IKS 무료 클러스터를 이용하는 경우](#iks-무료-클러스터를-이용하는-경우)


## 준비 사항

Kubernetes 실행 환경은 개발용으로 minikube를 이용하거나, Public Cloud Vendor가 제공하는 클러스터를 사용하거나 Local VM이나 Baremetal 서버에 클러스터를 구성하여 이용 하기도 합니다. 그러나, 본 글에서는 Kubernetes 클러스터 구성에 목적이 있지 않으므로, IBM Cloud Kubernetes Service(이하 IKS)를 이용해 보려고 합니다. IKS는 Kubernetes Cluster Control Plane을 자동으로 관리해 주므로 개발자는 Kubernetes Node만 신경쓰면 됩니다. 또한, IKS의 Node를 한국(판교D/C)에 생성 할 수 있는 큰 장점이 있습니다.

### IBM Cloud 회원 가입

IKS를 이용하려면 먼저 IBM Cloud 계정이 필요합니다. 만약, 회원 가입을 하지 않으셨다면, 아래 URL을 통해 회원 가입을 해 주시기 바랍니다.
	
[IBM Cloud 회원 가입](http://console.bluemix.net/registration)

### IBM Cloud 계정 업그레이드

회원 가입이 끝났다면, IBM Cloud 사용을 위한 카드 등록이 필요합니다. 카드 등록이 없는 상태의 trail 계정은 Kubernetes Node를 생성 할 수 없습니다. 경우에 따라 신용카드 등록에 최대 2일 정도가 소요될 수 있습니다.

### IBM Cloud Kubernetes 클러스터 생성   

카드 등록 완료 후 standard 계정으로 업그레이드 되었다면, [IBM Cloud Kubernetes 클러스터](https://console.bluemix.net/containers-kubernetes/catalog/cluster/create)를 생성합니다. 

생성 할 수 있는 클러스터 종류는 Free와 Standard가 있습니다. Free 클러스터는 미국 Dallas와 호주 Melbourne 중 한 곳을 선택하여 무료로 1개를 생성할 수 있습니다. 무료 클러스터는 2 CPUs, 4 GB RAM, 1 Worker Node로 구성되며 30일 동안 사용할 수 있으며 일부 기능 사용에 제한적입니다. 표준 클러스터와 차이점은 [무료 및 표준 클러스터 비교](https://console.bluemix.net/docs/containers/cs_why.html#cluster_types)을 참고 하시기 바랍니다.

이 예제는 고성능 클러스터가 필요한 것은 아니지만, 한국 데이터 센터를 선택 할 수 있고 Ingress 서비스를 사용이 가능한 장점이 있어 유료 클러스터를 기준으로 설명하고 있습니다. 제일 저렴한 2코어 4GB 메모리의 Shared VM을 이용하여 1개의 Worker Node만 사용하는 경우 1시간에 133원 정도의 비용이 발생합니다. 추가적인 IKS 가격 정보는 [IBM Cloud Kubernetes 클러스터 생성하기](https://cloud.ibm.com/kubernetes/catalog/cluster/create)에서 예상 금액을 확인 할 수 있으니 이를 참고하시기 바랍니다.
	
표준 클러스터를 생성하면 해당 클러스터를 외부에서 접속 가능한 Ingress Subdomain 주소를 부여 받게 됩니다.

> <CLUSTER_NAME>.<DC_ZONE_NAME>.containers.appdomain.cloud

예를 들어,  클러스터 이름을 `mycluster`로 작성하고 한국에 표준 클러스터를 생성하였다면, 

> mycluster.seo01.containers.appdomain.cloud

로 부여됩니다. 


### IBM Cloud CLI 준비

IKS는 IBM Cloud의 서비스로서 IBM Cloud CLI를 이용하여 정보를 확인 할 수 있습니다. 물론 웹 대시보드를 통해서도 확인이 가능하지만, Kubernetes CLI (kubectl) 사용을 위한 정보가 제공됩니다. 아래 나오는 CLI 설정은 IBM Cloud 컨테이너 Dashboard에서 확인 할 수 있습니다.

다음 명령을 실행하여 CLI 및 플러그인을 설치합니다.

``` bash
curl -sL https://ibm.biz/idt-installer | bash
```

Windows 10 Pro 환경에서는 Windows PowerShell을 이용 할 수 있습니다. 시작 메뉴에서 `PowerShell`을 검색하여 파일을 찾은 후, 해당 파일을 마우스로 오른쪽 클릭하여 `관리자 권한`으로 실행합니다. 그리고, 다음과 같은 명령을 실행합니다.

``` bash
Set-ExecutionPolicy Unrestricted; iex(New-Object Net.WebClient).DownloadString('http://ibm.biz/idt-win-installer')
```

IBM Cloud account에 로그인 합니다.

``` bash
ibmcloud login
```

IBM Cloud 컨테이너의 서비스 지역을 지정합니다.

``` bash
ibmcloud ks region-set <REGION>
```

Kubernetes 환경 설정 정보를 다운로드 합니다.

``` bash
ibmcloud ks cluster-config <CLUSTER_NAME>
```

`KUBECONFIG` 환경 변수 정보를 설정합니다.

``` bash
export KUBECONFIG=/Users/$USER/.bluemix/plugins/container-service/clusters/<CLUSTER_NAME>/kube-config-<DC_ZONE_NAME>-<CLUSTER_NAME>.yml
```

Windows 터미널 환경에서는 다음과 같습니다.

``` bash
SET KUBECONFIG=%HOMEPATH%\.bluemix\plugins\container-service\clusters\<CLUSTER_NAME>\kube-config-<DC_ZONE_NAME>-<CLUSTER_NAME>.yml
```

만약, Windows PowerShell을 이용하는 경우는 다음과 같습니다.

``` bash
$env:KUBECONFIG=$env:HOMEPATH+'\.bluemix\plugins\container-service\clusters\<CLUSTER_NAME>\kube-config-<DC_ZONE_NAME>-<CLUSTER_NAME>.yml'
```

환경 설정이 되었다면 Worker 노드 정보를 확인합니다.

``` bash
kubectl get nodes
```

## 애플리케이션 배포

### Container Image 생성 (선택 사항)

본 튜토리얼에 사용되는 이미지는 사전에 만들어 놓은 이미지를 이용합니다. 만약, 여러분이 직접 생성하고자 하는 경우 [Container Image 생성 가이드 문서](DockerBuild.md)를 참고하여 이미지를 생성 후 아래 서비스를 생성하시기 바랍니다.

참고로, Container 이미지를 생성했다면 아래 Kubernetes Deployment의 Container Image 정보는 새로 Build된 정보에 맞추어 변경해 주어야 합니다.

``` yaml
...
      containers:
        - image: <YOUR_IMAGE_NAME_SPACE>/spring-petclinic-visits-service:latest
          imagePullPolicy: Always
...
```

### API 마이크로 서비스 생성하기

다음 명령을 실행하여 API 마이크로 서비스 Deployment와 Service를 생성합니다.

``` bash
kubectl create -f k8s/api-gateway.yaml
```

### Customers 마이크로 서비스 생성하기

다음 명령을 실행하여 Customers 마이크로 서비스 Deployment와 Service를 생성합니다.

``` bash
kubectl create -f k8s/customers-service.yaml
```

### Vets 마이크로 서비스 생성하기

다음 명령을 실행하여 Vets 마이크로 서비스 Deployment와 Service를 생성합니다.

``` bash
kubectl create -f k8s/vets-service.yaml
```

### Visits 마이크로 서비스 생성하기

다음 명령을 실행하여 Visits 마이크로 서비스 Deployment와 Service를 생성합니다.

``` bash
kubectl create -f k8s/visits-service.yaml
```

### 배포 상태 확인

다음 명령을 실행하여 마이크로 서비스들이 정상적으로 배포 되었는지 확인 하십시오

``` bash
kubectl get pods -o=wide
```

``` bash
NAME                           READY     STATUS    RESTARTS   AGE       IP              NODE
api-gateway-745db58c94-zdwfb   1/1       Running   0          1h        xxx.xxx.xxx.16   xxx.xxx.xxx.247
customers-77b6c4784f-tp8gn     1/1       Running   0          1h        xxx.xxx.xxx.20   xxx.xxx.xxx.247
vets-6ddf965b54-7jhpt          1/1       Running   0          1h        xxx.xxx.xxx.23   xxx.xxx.xxx.247
visits-7f97889974-psmsh        1/1       Running   0          1h        xxx.xxx.xxx.24   xxx.xxx.xxx.247
```

``` bash
kubectl get svc
```

``` bash
NAME                TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)          AGE
api-gateway         NodePort    xxx.xxx.xxx.104   <none>        80:32002/TCP     1h
customers-service   NodePort    xxx.xxx.xxx.63    <none>        80:32003/TCP     1h
kubernetes          ClusterIP   xxx.xxx.xxx.1     <none>        443/TCP          2d
vets-service        NodePort    xxx.xxx.xxx.129   <none>        80:32005/TCP     1h
visits-service      NodePort    xxx.xxx.xxx.196   <none>        80:32004/TCP     1h
```

### Ingress 설정 및 웹 브라우저로 확인

표준 클러스터에 배포된 경우 subdomain은 cluster name과 region에 따라 자동으로 할당되며 본 튜토리얼에서는 이 정보를 이용합니다. 만약 표준 클러스터가 아닌 경우 subdomain 생성 정보가 없으므로 아래 [IKS 무료 클러스터를 이용하는 경우](#iks-무료-클러스터를-이용하는-경우)를 참고합니다.

클러스터에 자동으로 할당되는 subdomain은 다음과 같은 명령으로 확인할 수 있습니다.

``` bash
ibmcloud ks cluster-get <CLUSTER_NAME>
```

``` bash
$ ibmcloud ks cluster-get mycluster
Retrieving cluster mycluster...
OK

                                
Name:                           mycluster
ID:                             xxxx
State:                          normal
Created:                        2019-03-20T06:13:44+0000
Location:                       seo01
Master URL:                     https://xxx.xxx.xxx:xxxx
Public Service Endpoint URL:    https://xxx.xxx.xxx:xxxx
Private Service Endpoint URL:   -
Master Location:                seo01
Master Status:                  Ready (1 day ago)
Master State:                   deployed
Master Health:                  normal
Ingress Subdomain:              mycluster.seo01.containers.appdomain.cloud
Ingress Secret:                 mycluster
Workers:                        1
Worker Zones:                   seo01
Version:                        1.12.9_1555* (1.13.6_1524 latest)
Owner:                          xxxx@xxxx.xxx
Monitoring Dashboard:           -
Resource Group ID:              xxxxxxxxxxx
Resource Group Name:            default
```
 
그 중 `Ingress Subdomain` 항목이 자동으로 할당되는 subdomain 주소 입니다. 위의 경우는 <cluster_name>이 `mycluster` <region_or_zone>이 `seo01`이므로 `mycluster.seo01.containers.appdomain.cloud`이 할당된 것을 알 수 있습니다.

k8s/ingress.yaml 파일을 열어 `<INGRESS_SUBDOMAIN>`를 위에서 확인한 ingress subdomin 값으로 변경 후 저장합니다.

``` yaml
spec:
  rules:
  - host: petclinic.<INGRESS_SUBDOMAIN>
```

ingress.yaml 파일이 준바되었으면 아래 명령으로 Ingress 를 생성합니다.

``` bash
kubectl create -f k8s/ingress.yaml
```

Ingress가 정상적으로 생성된 경우 다음 URL에 접근하면 현재 실행 중인 petclinic 애플리케이션을 확인 할 수 있습니다.

> https://petclinic.\<INGRESS_SUBDOMAIN\>


## IKS 무료 클러스터를 이용하는 경우

무료 클러스터의 경우 Node의 Public IP만 제공되며 아래와 같이 두 가지 방식으로 접근 할 수 있습니다.

* [Nginx 이미지와 NodePort 타입 Service를 이용하는 방법](nginx-이미지와-nodeport-타입-service를-이용하는-방법)
* [Nginx Ingress Controller를 이용하는 방법](nginx-ingress-controller를-이용하는-방법)

### Nginx 이미지와 NodePort 타입 Service를 이용하는 방법

다음 명령을 실행하여 Nginx Deployment와 Service를 생성합니다.

``` bash
kubectl create -f k8s/nginx/nginx-configmap.yaml
kubectl create -f k8s/nginx/nginx-service.yaml
kubectl create -f k8s/nginx/nginx.yaml
```

다음 명령을 이용하여 worker node의 EXTERNAL-IP를 확인 합니다.

``` bash
kubectl get nodes -o wide
```

nginx는 NodePort 32010을 이용하므로 웹 브라우저를 실행하여 다음과 같은 URL에 접근합니다.

> http://\<EXTERNAL-IP\>:32010/

### Nginx Ingress Controller를 이용하는 방법

Nginx Ingress Controller는 Ingress Controller를 Nginx로 구현한 것으로 아래와 같이 코드가 공개되어 있습니다.

* https://github.com/kubernetes/ingress-nginx
* https://github.com/kubernetes/ingress-nginx/blob/master/docs/deploy/index.md

다양한 형태로 설치 가능하지만 Helm Chart를 이용하면 별다른 옵션 없이 바로 배포 할 수 있습니다. [Helm Chart를 이용한 nginx 패키지 배포](Practical.md#helm-install) 문서를 참고하면 Nginx Ingress Controller를 설치 할 수 있습니다.


Ingress Controller가 준비되었다면 다음 명령으로 ingress를 생성합니다.

``` bash
kubectl create -f k8s/ingress-nginx.yaml
```

Ingress가 정상적으로 생성된 경우 다음 URL에 접근하면 현재 실행 중인 petclinic 애플리케이션을 확인 할 수 있습니다.

> http://\<EXTERNAL-IP\>/


## MySQL DB 이용하기 (선택 사항)

Spring PetClinic Microservice는 Database를 HSQL을 기본으로 사용하고 추가적으로 MySQL DB를 사용 할 수도 있습니다. 먼저 MySQL DB를 준비합니다.

### MySQL Database 준비하기

MySQL DB는 IBM Cloud에서 접속가능한 Instance 이어야 합니다. 사전에 MySQL DB가 준비되지 않은 경우 [Kubernetes 클러스터에 MySQL을 배포하는 방법](MySQL.md)을 이용할 수 있습니다.

### MySQL 호스트 서버 및 포트 정보 입력하기

본 예제에서는 [k8s/mysql/mysql-configmap.yaml](k8s/mysql/mysql-configmap.yaml) 파일에 MySQL 서버 호스트 및 포트 번호가 입력되어 있습니다. 기본적으로 Kubernetes 클러스터에 생성한 MySQL 서버로 연결하도록 `hostinfo` 값이 `mysql:3306`으로 되어있습니다. 만약 외부 DB 서버를 사용하는 경우 해당 정보를 변경하도록 합니다. 그리고, 다음 명령을 이용하여 ConfigMap 을 생성합니다.

``` bash
kubectl create -f k8s/mysql/mysql-configmap.yaml
```

### MySQL Secret 생성하기

MySQL DB에 접근한는 사용자와 비밀번호는 ConfigMap이 아닌 Secret으로 입력합니다. 

기본적으로는 Kubernetes 클러스터에 생성한 MySQL 서버로 연결을 위한 `username`는 `root`, `password`는 `petclinic`으로 다음 명령으로 Secret 정보를 생성합니다. 만약, 기본 사용자 정보와 다른 경우 [파일에서 MySQL Secret 생성하기](#파일에서-mysql-secret-생성하기)를 이용하시기 바랍니다.

``` bash
kubectl create -f k8s/mysql/mysql-secret.yaml
```

### 파일에서 MySQL Secret 생성하기

파일에서 Secret을 생성하기 위해 다음과 같이 입력합니다.

``` bash
# Create files needed for rest of example.
echo -n "<YOUR_MYSQL_USERNAME" > username
echo -n "<YOUR_MYSQL_PASSWORD" > password
```

참고로, `username`과 `password` 파일은 보안에 위협이 되므로 Secret 생성 후 삭제해 주어야 합니다.

`kubectl create secret` 명령으로 `mysql-credential` Secret을 생성합니다. 

``` bash
kubectl create secret generic mysql-credential --from-file=username --from-file=password
```

생성한 secret 정보를 확인하면 다음과 같습니다.

``` bash
kubectl get secret/mysql-credential -o yaml
```

이렇게 생성된 Secret은 다음과 같이 secretKeyRef를 통해 환경 변수로 로딩됩니다.

``` yaml
env:
- name: MYSQL_USERNAME
valueFrom:
  secretKeyRef:
    name: mysql-credential
    key: username
- name: MYSQL_PASSWORD
valueFrom:
  secretKeyRef:
    name: mysql-credential
    key: password
```

### MySQL DB 이용을 위한 환경 변수 설정 하기

Spring PetClinic에서 MySQL DB를 사용하려면 DB를 사용하는 다음 세 개의 서비스 모듈에 MySQL 관련 환경 변수를 전달해야 합니다.

* customers-service
* vets-service
* visits-service

환경 변수는 Deployment의 Container Template 정보에 추가하게 되며, 다음은 vets-service에 대한 예시 입니다.

``` yaml
spec:
  strategy:
    type: Recreate
  template:
    metadata:
      labels:
        app: spring-petclinic
        tier: visits
    spec:
      containers:
        - image: hongjs/spring-petclinic-visits-service:latest
          imagePullPolicy: Always
          name: visits
          ports:
            - containerPort: 8080
          env:
          - name: MYSQL_HOSTINFO
            valueFrom:
              configMapKeyRef:
                name: mysql-config
                key: hostinfo
          - name: MYSQL_USERNAME
            valueFrom:
              secretKeyRef:
                name: mysql-credential
                key: username
          - name: MYSQL_PASSWORD
            valueFrom:
              secretKeyRef:
                name: mysql-credential
                key: password
          - name: spring.profiles.active
            value: mysql
```

이 내용을 적용한 Deployment 파일들을 다음 명령으로 Kubernetes Cluster에 적용합니다.

``` bash
kubectl apply -f k8s/mysql/mysql-customers-service.yaml
kubectl apply -f k8s/mysql/mysql-vets-service.yaml
kubectl apply -f k8s/mysql/mysql-visits-service.yaml
```
