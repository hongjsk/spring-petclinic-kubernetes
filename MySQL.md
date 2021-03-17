# Kubernetes에서 MySQL 서버 생성하기

일반적으로 MySQL 서버를 구성하려면 Host OS에 MySQL 설치 파일을 두고 실행하게 됩니다. 만약 Host에 Docker가 설처되어 있는 경우 Docker를 이용하여 MySQL 서버 컨테이너를 실행하기도 합니다. 

본 튜토리얼에서는 MySQL 서버를 Kubernetes에 배포하는 방법에 대해 설명합니다. 

## 구성

MySQL 서버를 Kubernetes 클러스터에 배포하려면 먼저 배포에 사용할 Docker 이미지가 준비되어야 합니다. 사용할 Docker 이미지는 `mysql:5.7.8`을 사용하며 Docker 이미지를 Deployment로 구성합니다.

생성된 Deployment에 대해 다른 Container에서 접근할 수 있도록 Service를 생성합니다.

마지막으로 Database가 저장되는 공간은 PV(Persistant Volume)로 지정합니다.

## Persisent Volume 준비

MySQL 서버가 내용을 저장할 공간을 할당합니다. 클러스터 제공 환경에 따라 다양한 영구 저장소를 제공하지만, 가장 간단한 것은 Node 서버의 파일 시스템 공간을 이용한 것입니다. 이를 위해 다음과 같은 명령으로 `local-volume`이란 이름의 5Gi 용량의 저장 공간을 생성합니다.

``` bash
kubectl create -f local-volumes.yaml
```

그리고, 이를 MySQL에게 할당하기 위한 1Gi 용량의 Volume Claim을 구성합니다.

``` bash
kubectl create -f mysql-pv-claim.yaml
```

## Secret 정보 등록하기

mysql 사용자와 비밀번호는 정보가 그대로 노출되는 ConfigMap 직접 입력이 아닌 Secret으로 입력 후 환경 변수로 간접적으로 로딩하는 방법을 사용합니다. 입력 정보를 명확하게 하게 할 수 있도록 파일을 생성하고 이를 Secret으로 생성하는 방법을 사용합니다. 예를 들어, username이 `root`이고 password가 `petclinic`인 경우 다음과 같이 입력합니다. root/petclinic 정보는 공개적으로 노출될 수 있으므로, 실제로 사용환경에 맞추어 변경해서 사용해야 합니다.

``` bash
echo -n "root" > username
echo -n "petclinic" > password
```

그리고, `kubectl create secret` 명령으로 `mysql-credential` Secret을 생성합니다.

``` bash
kubectl create secret generic mysql-credential --from-file=username --from-file=password
rm username password
```

정상적으로 Secret이 생성되었는지 확인합니다.

``` bash
kubectl get secret/mysql-credential -o yaml
```

``` yaml
apiVersion: v1
data:
  password: xxxxxxx
  username: yyyyyyy
kind: Secret
metadata:
  creationTimestamp: 2018-04-23T08:13:44Z
  name: mysql-credential
  namespace: default
  resourceVersion: "1619371"
  selfLink: /api/v1/namespaces/default/secrets/mysql-credential
  uid: 3d7468f6-46ce-11e8-8c50-08002742030b
type: Opaque
```

Deployment는 이렇게 생성된 Secret을 Container에서 환경 변수로 인식될 수 있도록 다음과 같은 모습으로 구성되어 있습니다.

``` yaml
...
env:
valueFrom:
  secretKeyRef:
    name: mysql-credential
    key: username
- name: MYSQL_PASSWORD
valueFrom:
  secretKeyRef:
    name: mysql-credential
    key: password
...
```

## MySQL Deployment & Service 생성

다음 명령으로 MySQL Deployment & Service를 생성합니다.

``` bash
kubectl create -f k8s/mysql/mysql.yaml
kubectl create -f k8s/mysql/mysql-service.yaml
```

## MySQL 테이블 생성 및 초기 데이터 입력 (선택 사항)

Spring PetClinic은 `petclinic` 이라는 이름의 DB를 사용하며 다음과 같은 테이블 스키마 및 데이터가 필요합니다.

Service Name | SQL Schema File
---|---
customers-service | spring-petclinic-customers-service/src/main/resources/db/mysql/schema.sql
visits-service    | spring-petclinic-visits-service/src/main/resources/db/mysql/schema.sql
vets-service      | spring-petclinic-vets-service/src/main/resources/db/mysql/schema.sql

Service Name | SQL Data File
---|---
customers-service | spring-petclinic-customers-service/src/main/resources/db/mysql/data.sql
visits-service    | spring-petclinic-visits-service/src/main/resources/db/mysql/data.sql
vets-service      | spring-petclinic-vets-service/src/main/resources/db/mysql/data.sql

각각의 SQL 파일을 참고하여 테이블 및 데이터를 입력해 주어야 하지만 [k8s/mysql/sql/mysql-schema.sql](k8s/mysql/sql/mysql-schema.sql) 및 [k8s/mysql/sql/mysql-data.sql](k8s/mysql/sql/mysql-data.sql) 파일에 필요한 정보를 모아 놓았으므로 이를 이용할 수 있습니다. 

#pc의-mysql-cli를-이용하는-경우
#kubernetes에-배포된-mysql을-이용하는 경우

### PC의 MySQL CLI를 이용하는 경우

MySQL 서버는 Kuberenetes로 실행되며 NodePort 서비스로 외부에서 접근 가능한 상태입니다.

먼저, Kubernetes Cluster의 External IP 정보를 다음과 같이 확인합니다.

다음 명령을 이용하여 worker node의 EXTERNAL-IP를 확인 합니다.

``` bash
kubectl get nodes -o wide
```

만약, MySQL CLI가 설치되어 있고 `mysql` 명령을 실행 할 수 있다면 다음 명령을 실행하여 SQL을 Import 할 수 있습니다.

``` bash
mysql -h <EXTERNAL-IP> -P 32001 -u root -ppetclinic < k8s/mysql/sql/mysql-schema.sql
mysql -h <EXTERNAL-IP> -P 32001 -u root -ppetclinic petclinic < k8s/mysql/sql/mysql-data.sql
```

Open Source GUI Client인 [DBeaver](https://dbeaver.io/)를 이용할 수도 있는데 본 튜토리얼에서는 다루지 않습니다. 단, Connection 정보는 다음과 같습니다.

Property | Value
---|---
Host | <EXTERNAL-IP>
Port | 32001
User | root
Password | petclinic
Database | petclinic
JDBC URL | jdbc:mysql:<EXTERNAL-IP>:32001/petclinic

### Kubernetes에 배포된 MySQL을 이용하는 경우

먼저 다음 명령으로 앞서 배포된 MySQL Deployment 가 정상적으로 배포되었는지 확인합니다.

``` bash
kubectl get deploy
```

``` bash
$ kubectl get deploy
NAME          DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
api-gateway   1         1         1            1           19h
customers     1         1         1            1           19h
mysql         1         1         1            1           19h
nginx         1         1         1            1           18h
vets          1         1         1            1           19h
visits        1         1         1            1           19h
```

이제 다음 명령으로 실제 컨테이너가 실행되는 pod 정보를 확인합니다.

``` bash
kubectl get pod -l app=mysql
```

``` bash
NAME                     READY   STATUS    RESTARTS   AGE
mysql-6d87765586-2q7sn   1/1     Running   0          19h
```

위의 경우 mysql deployment가 실행되는 <MYSQL_POD_NAME>은 `mysql-6d87765586-2q7sn`인 것을 알 수 있습니다.

이를 참고하여 `kubectl cp` 명령으로 해당 pod에 sql 파일을 복사 할 수 있습니다.

``` bash
kubectl cp k8s/mysql/sql/mysql-schema.sql <MYSQL_POD_NAME>:/tmp/
kubectl cp k8s/mysql/sql/mysql-data.sql <MYSQL_POD_NAME>:/tmp/
```

이제 `kubectl exec` 명령을 이용하여 mysql pod에 Database Schema와 Table을 생성합니다.

``` bash
kubectl exec <MYSQL_POD_NAME> -- sh -c 'mysql -u root -p$MYSQL_ROOT_PASSWORD petclinic < /tmp/mysql-schema.sql'
kubectl exec <MYSQL_POD_NAME> -- sh -c 'mysql -u root -p$MYSQL_ROOT_PASSWORD petclinic < /tmp/mysql-data.sql'
```

다음 명령으로 실제 정상적으로 데이터가 들어갔는지 다음 명령으로 확인합니다.

``` bash
kubectl exec <MYSQL_POD_NAME> -- sh -c 'mysql -u root -p$MYSQL_ROOT_PASSWORD -e "select * from vets" petclinic'
```

실행 결과가 아래와 같이 출력되면 정상적으로 생성된 것입니다.

```
mysql: [Warning] Using a password on the command line interface can be insecure.
id	first_name	last_name
1 James	Carter
2	Helen	Leary
3	Linda	Douglas
4	Rafael	Ortega
5	Henry	Stevens
6	Sharon	Jenkins
mysql: [Warning] Using a password on the command line interface can be insecure.
```

`exit` 명령으로 CLI를 종료합니다.

``` bash
exit
```
