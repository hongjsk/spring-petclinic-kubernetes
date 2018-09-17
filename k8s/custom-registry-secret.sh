SERVER_NAME=
USER_NAME=
USER_PASSWORD=
kubectl create secret docker-registry regcred --docker-server=$SERVER_NAME --docker-username=$USER_NAME --docker-password=$USER_PASSWORD
