#!/bin/bash
if [ "$K8S_DRIVER" = "" ]; then
  K8S_DRIVER=kind
fi
if [ "$2" == "" ]; then
  echo "Arguments: <docker-image> <tag> [<dont-pull>]"
  exit 1
fi
if [ "$K8S_DRIVER" != "tmc" ] && [ "$K8S_DRIVER" != "gke" ] ; then
  DONT_PULL=$3
  if [[ "$2" = "" ]]; then
    echo "A TAG is required for $1" >&2
    exit 2
  fi
  TAG=$2
  NAME=$1
  IMAGE="$NAME:$TAG"
  COUNT=$(docker images 2> /dev/null | grep -F "$NAME" | grep -c -F "$TAG")
  if [ "$DONT_PULL" = "true" ]; then
    if ((COUNT == 0)); then
      echo "ERROR:Image not found $IMAGE" >&2
      exit 2
    else
      echo "Not pulling:$IMAGE"
    fi
  elif [ "$DONT_PULL" = "" ]; then
    if ((COUNT == 0)) || [[ "$TAG" == *"SNAPSHOT"* ]] || [[ "$TAG" == *"latest"* ]]; then
      echo "Pulling:$IMAGE"
      docker pull "$IMAGE"
    else
      echo "Exists:$IMAGE"
    fi
  fi
  COUNT=$(docker images 2> /dev/null | grep -F "$NAME" | grep -c -F "$TAG")
  if ((COUNT == 0)); then
    echo "WARN:Image Not found:$IMAGE"
    exit 0
  fi
  err=$(docker history "$IMAGE" 2> /dev/null)
  rc=$?
  if [[ $rc -ne 0 ]]; then
    echo "$err" >&2
    exit 1
  fi
  echo "Loading:$IMAGE"
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
  *)
    echo "Loading $IMAGE to minikube"
    DOCKER_IDS=$(docker images | grep -F "$NAME" | grep -F "$TAG" | awk '{print $3}')
    MK_IDS=$(minikube image ls --format=table | grep -F "$NAME" | grep -F "$TAG" | awk '{print $6}')
    for did in $DOCKER_IDS; do
      for mid in $MK_IDS; do
        # Docker id may be shorter than Minikube id.
        if [ "${mid:0:12}" = "${did:0:12}" ]; then
          echo "$IMAGE already uploaded"
          exit 0
        fi
      done
    done
    PULL=true
    if [ "$DONT_PULL" == "true" ]; then
        PULL=false
    fi
    minikube image load --pull=$PULL "$IMAGE"
    ;;
  esac
  echo "Loaded:$IMAGE"
fi
