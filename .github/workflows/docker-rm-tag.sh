#!/usr/bin/env bash
# springcloud/spring-cloud-skipper-server
IMAGE=$1
# 2.11.0
# 2.11.0-jdk8
# 2.11.0-jdk11
# 2.11.0-jdk17
TAG=$2

login_data() {
cat <<EOF
{
  "username": "$DOCKERHUB_USERNAME",
  "password": "$DOCKERHUB_TOKEN"
}
EOF
}

TOKEN=$(curl -s -H "Content-Type: application/json" -X POST -d "$(login_data)" "https://hub.docker.com/v2/users/login/" | jq -r .token)
echo "Deleting tag from $IMAGE:$TAG"
curl "https://hub.docker.com/v2/repositories/${IMAGE}/tags/${TAG}/" -X DELETE -H "Authorization: JWT ${TOKEN}"
