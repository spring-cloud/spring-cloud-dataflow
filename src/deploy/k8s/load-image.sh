#!/usr/bin/env bash
function print_args() {
    echo "Arguments: <docker-image:tag> [<dont-pull>]"
    echo "Not there is not space between name and tag. It is like on any docker registry"
    echo "dont-pull: true - will not pull never"
    echo "dont-pull: false | will pull always"
    echo "dont-pull: <pot-provided>: will pull if not present"
}
if [ "$K8S_DRIVER" = "" ]; then
  K8S_DRIVER=kind
fi
if [ "$1" = "" ]; then
  print_args
  exit 1
fi
if [ "$2" != "" ] && [ "$2" != "true" ] && [ "$2" != "false" ]; then
  print_args
  exit 1
fi

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
    if ((COUNT == 0)) || [[ "$IMAGE" = *"-SNAPSHOT" ]] || [[ "$IMAGE" = *":latest" ]]; then
      echo "Pulling:$IMAGE"
      docker pull "$IMAGE"
    else
      echo "Exists:$IMAGE"
    fi
  fi
  COUNT=$(docker images --filter "reference=$IMAGE" --format "{{ .Repository }}:{{ .Tag }}" 2> /dev/null | grep -c -F "$IMAGE")
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
  "microk8s")
    echo "Loading $IMAGE to microk8s"
    docker tag $IMAGE localhost:32000/$IMAGE
    docker push localhost:32000/$IMAGE
    ;;
  *)
    echo "Loading $IMAGE to minikube"
    DOCKER_IDS=$(docker images --filter "reference=$IMAGE" --format "{{ .ID }}")
    NAME="${IMAGE%%:*}"
    colon=":"
    TAG=${IMAGE#*$colon}
    MK_IDS=$(minikube image ls --format table | grep -F "$NAME" | grep -F "$TAG" | awk '{print $6}')
    for did in $DOCKER_IDS; do
      for mid in $MK_IDS; do
        # Docker id may be shorter than Minikube id.
        if [ "${mid:0:12}" = "${did:0:12}" ]; then
          echo "$IMAGE:$did already uploaded"
          exit 0
        fi
      done
    done
    PULL=false
    if [ "$DONT_PULL" = "false" ]; then
        PULL=true
        echo "Loading:$IMAGE"
    else
        echo "Loading:$IMAGE:$DOCKER_IDS"
    fi
    OPTIONS="--overwrite true --daemon true --pull $PULL"
    if [ "$PULL" = "false" ]; then
        OPTIONS="$OPTIONS --remote false"
    fi
    minikube image load "$IMAGE" $OPTIONS
    MK_IDS=$(minikube image ls --format table | grep -F "$NAME" | grep -F "$TAG" | awk '{print $6}')
    for did in $DOCKER_IDS; do
      for mid in $MK_IDS; do
        # Docker id may be shorter than Minikube id.
        if [ "${mid:0:12}" = "${did:0:12}" ]; then
              echo "$IMAGE:$did uploaded"
              exit 0
        fi
        done
    done
    echo "Unable to load $IMAGE:$DOCKER_IDS. It may be in active use."
    exit 0
    ;;
  esac
  echo "Loaded:$IMAGE"
fi
