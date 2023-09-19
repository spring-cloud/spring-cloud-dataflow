#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
set +e
if [ "$TAG" == "" ]; then
    echo "TAG not found"
    exit 1
fi
if [ "$DEFAULT_JDK" = "" ]; then
    echo "DEFAULT_JDK not found using 11"
    DEFAULT_JDK=11
else
    echo "DEFAULT_JDK=$DEFAULT_JDK"
fi

function download_image() {
    TARGET=$1
    ARTIFACT_ID=$2
    VERSION=$3
    TARGET_FILE=$TARGET/$ARTIFACT_ID-$VERSION.jar
    pushd $SCDIR/download-jar > /dev/null || exit
        ./gradlew downloadJar -PartifactId=$ARTIFACT_ID -PartifactVersion=$VERSION -PartifactPath=$TARGET
        RC=$?
        if((RC != 0)); then
            exit $RC
        fi
    popd > /dev/null || exit
    if [ ! -f $TARGET_FILE ]; then
        echo "Cannot find $TARGET_FILE"
        ls -al $TARGET
        exit 2
    fi
    echo "Downloaded $TARGET_FILE"
}

TARGET=$1
REPO="$2"
ARTIFACT_ID=$3

if [ "$ARTIFACT_ID" = "" ]; then
    echo "Usage: <path> <container-repo> <artifactId>"
fi
JAR="$TARGET/$ARTIFACT_ID-$TAG.jar"
if [ ! -f "$JAR" ]; then
    echo "$JAR not found downloading"
    download_image "$TARGET" "$ARTIFACT_ID" "$TAG"
    RC=$?
    if((RC != 0)); then
        exit $RC
    fi
fi
echo "Creating: $REPO:$TAG-jdk$v"

for v in 8 11 17; do
    END=$(date '+%s')
    END=$((600 + END))
    RC=0
    while ((END > $(date '+%s') )); do
        pack build --builder gcr.io/paketo-buildpacks/builder:base \
            --path "$JAR" \
            --trust-builder --verbose \
            --env BP_JVM_VERSION=$v "$REPO:$TAG-jdk$v"
        RC=$?
        if ((RC == 0)); then
            break;
        fi
        echo "Sleeping for 1m"
        sleep 1m
    done
    if((RC != 0)); then
        exit $RC
    fi
    echo "Created: $REPO:$TAG-jdk$v"
    docker push "$IMAGE:$TAG-jdk$v"
    RC=$?
    if ((RC!=0)); then
        exit $RC
    fi
    echo "Pushed $IMAGE:$TAG-jdk$v"
    if [ "$DEFAULT_JDK" == "$v" ]; then
        docker tag "$IMAGE:$TAG-jdk$DEFAULT_JDK" "$IMAGE:$TAG"
        docker push "$IMAGE:$TAG"
        echo "Pushed $IMAGE:$TAG"
    fi
done
echo "Pruning Docker"
docker system prune -f
docker system prune --volumes -f
