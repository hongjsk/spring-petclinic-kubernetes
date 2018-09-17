# Ingress

## Helm install
```
curl https://raw.githubusercontent.com/kubernetes/helm/master/scripts/get > ~/get_helm.sh
chmod 700 ~/get_helm.sh
~/get_helm.sh
```

## Helm init and install nginx-ingress
* https://kubernetes.io/docs/concepts/services-networking/ingress/
* https://github.com/kubernetes/ingress-nginx

```
helm init
kubectl get nodes -o wide
EXTERNAL_IP=$(kubectl get nodes -o jsonpath='{..addresses[?(@.type=="ExternalIP")].address}')
helm install stable/nginx-ingress --name=nginx-ingress --namespace=kube-system --set rbac.create=true --set controller.service.externalIPs="{$EXTERNAL_IP}"
```

* Helm release 삭제방법
```
helm delete --purge nginx-ingress
```

* kubectl JSONPath support: https://kubernetes.io/docs/reference/kubectl/jsonpath/


## Ingress-nginx 생성
```
kubectl create -f ./k8s/ingress-nginx.yaml
```


# Scaling

## Manual scale
```
kubectl scale --replicas=3 deployment api-gateway
kubectl get pods
```

## Autoscaler
* https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/
* https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale-walkthrough/
```
kubectl run php-apache --image=k8s.gcr.io/hpa-example --requests=cpu=200m --expose --port=80
kubectl autoscale deployment php-apache --cpu-percent=50 --min=1 --max=10
kubectl get horizontalpodautoscalers
```

## 부하 설정
다른 터미널에서,
```
kubectl run -i --tty load-generator --image=busybox /bin/sh
while true; do wget -q -O- http://php-apache.default.svc.cluster.local; done
```

## 상태 확인
```
kubectl get horizontalpodautoscalers
kubectl get deployment php-apache
```
자동 확인 (CTRL-C 로 중단)
```
watch -n 1 "kubectl get horizontalpodautoscalers && kubectl get deployment php-apache && kubectl get pods"
```
