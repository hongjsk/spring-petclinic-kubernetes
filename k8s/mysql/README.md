# Kubernetes에서 MySQL 서버 생성하기

일반적으로 Docker를 이용하여 MySQL 서버를 실행하려면 다음과 명령을 사용하게 됩니다.

``` bash
docker run -e MYSQL_ROOT_PASSWORD=petclinic -e MYSQL_DATABASE=petclinic -p 3306:3306 mysql:5.7.8
```

이를 Kubernetes로 구성하기 위해서는 Docker 이미지를 Deployment로 구성하고 이를 Service로 구성하여 노출해야 합니다. 또한 저장 공간도 준비해야 합니다.

## Persisent Volume 준비

MySQL 서버가 내용을 저장할 공간을 할당합니다. 클러스터 제공 환경에 따라 다양한 영구 저장소를 제공하지만, 가장 간단한 것은 Node 서버의 파일 시스템 공간을 이용한 것입니다. 이를 위해 다음과 같은 명령으로 `local-volume`이란 이름의 5Gi의 저장 공간을 생성합니다.

``` bash
kubectl create -f local-volumes.yaml
```

그리고, 이를 MySQL에게 할당해 주기위한 1Gi 용량의 Volume Claim을 구성합니다.

``` bash
kubectl create -f mysql-pv-claim.yaml
```

## Secret 정보 등록하기

mysql 사용자와 비밀번호는 ConfigMap이 아닌 Secret으로 입력합니다. 파일에서 생성하기 위해 다음과 같이 입력합니다.

``` bash
echo -n "root" > ./username
echo -n "petclinic" > ./password
```

`kubectl create secret` 명령으로 `mysql-credential` Secret을 생성합니다.

``` bash
kubectl create secret generic mysql-credential --from-file=./username --from-file=./password
rm ./username ./password
```

``` bash
kubectl get secret/mysql-credential -o yaml
```

``` yaml
apiVersion: v1
data:
  password: cGV0Y2xpbmljCg==fh
  username: cm9vdAo=
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

이렇게 생성된 Secret은 다음과 같이 secretKeyRef를 통해 환경 변수로 사용 할 수 있습니다.

``` yaml
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
```

### MySQL Deployment & Service 생성

다음 명령으로 MySQL Deployment & Service를 생성합니다.

``` bash
kubectl create -f mysql.yaml
kubectl create -f mysql-service.yaml
```

### MySQL 테이블 생성 및 초기 데이터 입력

Spring Pet Clinic에 사용하는 DB는 테이블 스키마 및 데이터가 필요합니다.

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

각각의 SQL 파일을 실행하여 테이블 및 데이터를 입력해 주어야 합니다만, [./sql/mysql-schema.sql](./sql/mysql-schema.sql) 및 [./sql/mysql-data.sql](./sql/mysql-data.sql)에 필요한 정보를 모아 놓았으므로 이를 이용할 수 있습니다. SQL 실행은 선호하는 SQL Client 또는 Open Source GUI Client인 [DBeaver](https://dbeaver.io/)를 이용할 수 있으며 kubernetes로 MySQL CLI를 이용 할 수도 있습니다.

``` bash
kubectl run -it --rm --image=mysql:5.7.8 --restart=Never mysql-cli -- bash
```

다른 터미널을 새로 열어 SQL 파일을 복사합니다.

``` bash
kubectl cp sql/mysql-schema.sql mysql-client:/tmp/
kubectl cp sql/mysql-data.sql mysql-client:/tmp/
```

앞서 실행한 mysql-cli가 실행 중인 터미널에서 다음과 같이 SQL을 로딩합니다.

``` bash
mysql -h mysql -u root -ppetclinic < /tmp/mysql-schema.sql
mysql -h mysql -u root -ppetclinic petclinic < /tmp/mysql-data.sql
```

실제 정상적으로 데이터가 들어갔는지 다음과 같이 확인합니다.

``` bash
mysql -h mysql -u root -ppetclinic -e 'select * from vets' petclinic
```

실행 결과가 아래와 같이 출력되면 정상적으로 생성된 것입니다.

```
+----+------------+-----------+
| id | first_name | last_name |
+----+------------+-----------+
|  1 | James      | Carter    |
|  2 | Helen      | Leary     |
|  3 | Linda      | Douglas   |
|  4 | Rafael     | Ortega    |
|  5 | Henry      | Stevens   |
|  6 | Sharon     | Jenkins   |
+----+------------+-----------+
```

`exit` 명령으로 CLI를 종료합니다.

``` bash
exit
```
