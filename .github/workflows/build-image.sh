#!/usr/bin/env bash
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
xmllint --version
RC=$?
if((RC!=0)); then
    echo "xmllint not found"
    exit $RC
fi
function download_image() {
    TARGET=$1
    ARTIFACT_ID=$2
    VERSION=$3
    TARGET_FILE=$TARGET/$ARTIFACT_ID-$VERSION.jar
    if [[ "$VERSION" = *"-SNAPSHOT"* ]]; then
        META_DATA="https://repo.spring.io/snapshot/org/springframework/cloud/$ARTIFACT_ID/${VERSION}/maven-metadata.xml"
        echo "Downloading $META_DATA"
        rm -f maven-metadata.xml
        COUNT=5
        RC=0
        while ((COUNT>0)); do
            wget -O maven-metadata.xml -q "$META_DATA"
            RC=$?
            if((RC==0)); then
                break;
            fi
        done
        if((RC!=0)); then
            exit $RC
        fi
        xmllint --xpath "/metadata/versioning/snapshot/timestamp/text()" maven-metadata.xml | sed 's/\.//'
        DL_TS=$(xmllint --xpath "/metadata/versioning/snapshot/timestamp/text()" maven-metadata.xml | sed 's/\.//')
        echo "Metadata: $DL_TS"
        DL_VERSION=$(xmllint --xpath "/metadata/versioning/snapshotVersions/snapshotVersion[extension/text() = 'pom' and updated/text() = '$DL_TS']/value/text()" maven-metadata.xml)
        URL="https://repo.spring.io/snapshot/org/springframework/cloud/$ARTIFACT_ID/${VERSION}/$ARTIFACT_ID-${DL_VERSION}.jar"
    else
        REL_TYPE=
        if [[ "$STREAM_APPS_VERSION" = *"-M"* ]] || [[ "$STREAM_APPS_VERSION" = *"-RC"* ]]; then
            REL_TYPE=milestone
        fi
        if [ "$REL_TYPE" != "" ]; then
            URL="https://repo.spring.io/$REL_TYPE/org/springframework/cloud/$ARTIFACT_ID/${VERSION}/$ARTIFACT_ID-${VERSION}.jar"
        else
            URL="https://repo.maven.apache.org/maven2/org/springframework/cloud/$ARTIFACT_ID/${VERSION}/$ARTIFACT_ID-${VERSION}.jar"
        fi
    fi
    END=$(date '+%s')
    END=$((600 + END))
    RC=0
    mkdir -p $TARGET
    while ((END > $(date '+%s') )); do
        echo "Downloading $URL"
        rm -f "$TARGET_FILE"
        wget -O "$TARGET_FILE" -q "$URL"
        RC=$?
        if((RC == 0)); then
            break;
        fi
    done
    if((RC != 0)); then
        exit $RC
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
