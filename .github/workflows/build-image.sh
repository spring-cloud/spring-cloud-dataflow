#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
set +e
if [ "$PUSH" == "" ]; then
    PUSH=true
fi
if [ "$TAG" == "" ]; then
    echo "TAG not found"
    exit 1
fi
if [ "$DEFAULT_JDK" = "" ]; then
    echo "DEFAULT_JDK not found using 17"
    DEFAULT_JDK=17
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

TARGET=$(realpath $1)
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
# TODO add Java 21 when packeto supports it
for v in 17; do
    echo "Creating: $REPO:$TAG-jdk$v"
    pack build --builder gcr.io/paketo-buildpacks/builder:base \
        --path "$JAR" \
        --trust-builder --verbose \
        --env BP_JVM_VERSION=$v "$REPO:$TAG-jdk$v"
    RC=$?
    if((RC != 0)); then
        exit $RC
    fi
    echo "Created: $REPO:$TAG-jdk$v"
    if [ "$PUSH" == "true" ]; then
        if [ "$DELETE_TAGS" == "true" ]; then
            $SCDIR/docker-rm-tag.sh $REPO $TAG-jdk$v
        fi
        docker push "$REPO:$TAG-jdk$v"
        RC=$?
        if ((RC!=0)); then
            exit $RC
        fi
        echo "Pushed $REPO:$TAG-jdk$v"
    else
        echo "Skipped push $REPO:$TAG-jdk$v"
    fi

    if [ "$DEFAULT_JDK" == "$v" ]; then
        docker tag "$REPO:$TAG-jdk$DEFAULT_JDK" "$REPO:$TAG"
        if [ "$PUSH" == "true" ]; then
            if [ "$DELETE_TAGS" == "true" ]; then
                $SCDIR/docker-rm-tag.sh $REPO $TAG-jdk$v
            fi
            docker push "$REPO:$TAG"
            echo "Pushed $REPO:$TAG"
        else
            echo "Skipped push $REPO:$TAG"
        fi
    fi
done


