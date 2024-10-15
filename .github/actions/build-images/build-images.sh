#!/usr/bin/env bash
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

function pack_image {
    JAR="$1-$TAG.jar"
    REPO="$2"
    v="$3"
    if [ ! -f "$JAR" ]; then
        echo "File not found $JAR"
        exit 2
    fi
    echo "Creating: $REPO:$TAG-jdk$v"
    # --buildpack "paketo-buildpacks/java@10.0.0" --buildpack "paketo-buildpacks/bellsoft-liberica@10.3.2"
    pack build --builder paketobuildpacks/builder-jammy-base:latest \
            --path "$JAR" \
            --trust-builder --verbose \
            --env BP_JVM_VERSION=$v "$REPO:$TAG-jdk$v"
    RC=$?
    if ((RC!=0)); then
        echo "Error $RC packaging $JAR"
        exit $RC
    fi
    echo "Created: $REPO:$TAG-jdk$v"
}
LEN=$(jq '.include | length' .github/workflows/images.json)
for ((i = 0; i < LEN; i++)); do
    TARGET="$(jq -r --argjson index $i '.include[$index] | .path' .github/workflows/images.json)"
    IMAGE="$(jq -r --argjson index $i '.include[$index] | .image' .github/workflows/images.json)"
    ARTIFACT_ID="$(jq -r --argjson index $i '.include[$index] | .name' .github/workflows/images.json)"
    # 8 11 17 21
    for v in 17 21; do
        pack_image "$TARGET/$ARTIFACT_ID"  $IMAGE $v $ARTIFACT_ID
        RC=$?
        if [ $RC -ne 0 ]; then
            exit $RC
        fi
        docker push "$IMAGE:$TAG-jdk$v"
        echo "Pushed $IMAGE:$TAG-jdk$v"
        if [ "$DEFAULT_JDK" == "$v" ]; then
            docker tag "$IMAGE:$TAG-jdk$DEFAULT_JDK" "$IMAGE:$TAG"
            docker push "$IMAGE:$TAG"
            echo "Pushed $IMAGE:$TAG"
        fi
    done
done

