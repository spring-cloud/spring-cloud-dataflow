#!/bin/bash

function print_args() {
    echo "Arguments: <docker-image:tag> [<dont-pull>]"
    echo "Not there is not space between name and tag. It is like on any docker registry"
    echo "pull: true - will always pull"
    echo "pull: false - will never pull"
    echo "pull: <not-provided>: will pull if not present"
}
if [ "$K8S_DRIVER" = "" ]; then
  K8S_DRIVER=kind
fi
if [ "$1" == "" ]; then
  print_args
  exit 1
fi
if [ "$2" != "" ] && [ "$2" != "true" ] && [ "$2" != "false" ]; then
  print_args
  exit 1
fi
DONT_PULL=$2
IMAGE="$1"
echo "image: $IMAGE, dont_pull=$DONT_PULL"
if [ "$K8S_DRIVER" != "tmc" ] && [ "$K8S_DRIVER" != "gke" ] ; then
  DONT_PULL=$2
  IMAGE="$1"
  COUNT=$(docker images --filter "reference=$IMAGE" --format "{{ .Repository }}:{{ .Tag }}" 2> /dev/null | grep -c -F "$IMAGE")
  if [ "$DONT_PULL" = "true" ]; then
    if ((COUNT == 0)); then
      echo "ERROR:Image not found $IMAGE" >&2
      exit 2
    else
      echo "Not pulling:$IMAGE"
    fi
  elif [ "$DONT_PULL" = "" ]; then
    if ((COUNT == 0)) || [[ "$IMAGE" == *"-SNAPSHOT" ]] || [[ "$IMAGE" == *":latest" ]]; then
      echo "Pulling:$IMAGE"
      docker pull "$IMAGE"
    else
      echo "Exists:$IMAGE"
    fi
  fi
fi
DOCKER_SHA=$(docker image inspect "$IMAGE" --format json | jq -r '.[0].Id')
case "$K8S_DRIVER" in
    "kind")
        echo "Loading $IMAGE to kind"
        kind load docker-image "$IMAGE" "$IMAGE"
        ;;
    "tce")
        echo "Harbor push will be supported soon"
        ;;
    "gke")
        echo "gcr push will be supported soon"
        ;;
    "tmc")
        echo "not supported in TMC"
        ;;
    "microk8s")
        echo "Loading $IMAGE to microk8s"
        docker tag $IMAGE localhost:32000/$IMAGE
        docker push localhost:32000/$IMAGE
        ;;
    *)
        MINIKUBE_JSON=$(minikube image ls --format json | jq -c --arg image $IMAGE '.[] | {id: .id, image: .repoTags[0]} | select(.image | contains($image))')
        if [ "$MINIKUBE_JSON" != "" ]; then
            MINIKUBE_SHA="sha256:$(echo $MINIKUBE_JSON | jq -r '.id')"
            if [ "$MINIKUBE_SHA" = "$DOCKER_SHA" ]; then
                echo "$IMAGE already uploaded"
                exit 0
            else
                echo "Image: $IMAGE, docker $DOCKER_SHA, minikube $MINIKUBE_SHA"
            fi
        fi
        echo "Loading $IMAGE to minikube"
        minikube image load "$IMAGE"
        ;;
esac
echo "Loaded:$IMAGE"