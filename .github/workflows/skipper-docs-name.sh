#!/bin/bash
VERSION=$1
if [ "$1" = "" ]; then
    echo "Version is required"
    exit 1
fi
if [ "$2" != "" ]; then
    REPO="$2"
fi

if [ -z "$REPO" ]; then
    if [[ "$VERSION" = *"-SNAPSHOT"* ]]; then
        REPO="libs-snapshot-local"
    elif [[ "$VERSION" = *"-M"* ]] || [[ "${VERSION}" = *"-RC"* ]]; then
        REPO="libs-milestone-local"
    else
        REPO="libs-release-local"
    fi
fi
CURL_TOKEN="$ARTIFACTORY_USERNAME:$ARTIFACTORY_PASSWORD"
if [[ "$REPO" = *"snapshot"* ]]; then
    META_DATA_URL="https://repo.spring.io/artifactory/$REPO/org/springframework/cloud/spring-cloud-skipper-docs/${VERSION}/maven-metadata.xml"
    curl -u "$CURL_TOKEN" --basic -o maven-metadata.xml -s -XGET -L "$META_DATA_URL" # > /dev/null
    DL_TS=$(xmllint --xpath "/metadata/versioning/snapshot/timestamp/text()" maven-metadata.xml | sed 's/\.//')
    DL_VERSION=$(xmllint --xpath "/metadata/versioning/snapshotVersions/snapshotVersion[extension/text() = 'pom' and updated/text() = '$DL_TS']/value/text()" maven-metadata.xml)
    REMOTE_PATH="org/springframework/cloud/spring-cloud-skipper-docs/${VERSION}/spring-cloud-skipper-docs-${DL_VERSION}.zip"
else
    REMOTE_PATH="org/springframework/cloud/spring-cloud-skipper-docs/${VERSION}/spring-cloud-skipper-docs-${VERSION}.zip"
fi
REMOTE_FILE="https://repo.spring.io/artifactory/${REPO}/$REMOTE_PATH"
RC=$(curl -u "$CURL_TOKEN" --basic -o /dev/null -L -s -Iw '%{http_code}' "$REMOTE_FILE")
if ((RC<300)); then
  echo "$REMOTE_PATH"
else
  echo "$REMOTE_FILE does not exist. Error code $RC"
  exit 2
fi
