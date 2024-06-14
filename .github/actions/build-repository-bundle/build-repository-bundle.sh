#!/usr/bin/env bash
function check_env() {
  eval ev='$'$1
  if [ "$ev" = "" ]; then
    echo "env var $1 not defined"
    if ((sourced != 0)); then
      return 1
    else
      exit 1
    fi
  fi
}

TMP=$(mktemp -d)
if [ "$REPO_BUNDLE_GENERATED" = "" ]; then
    export REPO_BUNDLE_GENERATED="$TMP/generated/repobundle"
fi
mkdir -p $REPO_BUNDLE_GENERATED/packages
mkdir -p $REPO_BUNDLE_GENERATED/.imgpkg

if [ "$REPO_BUNDLE_RENDERED" = "" ]; then
    export REPO_BUNDLE_RENDERED="$TMP/generated/reporendered"
fi
mkdir -p "$REPO_BUNDLE_RENDERED"

check_env REPO_BUNDLE_TEMPLATE
check_env REPO_BUNDLE_RENDERED
check_env PACKAGE_VERSION
check_env PACKAGE_BUNDLE_REPOSITORY
check_env PACKAGE_NAME

echo "Build Repository Bundle: $REPO_BUNDLE_TEMPLATE, project.version=$PACKAGE_VERSION, package.name=$PACKAGE_NAME, repository=$PACKAGE_BUNDLE_REPOSITORY, output=$REPO_BUNDLE_RENDERED"

set -e

ytt \
    -f $REPO_BUNDLE_TEMPLATE \
    --output-files $REPO_BUNDLE_RENDERED \
    --data-value-yaml project.version=$PACKAGE_VERSION \
    --data-value-yaml repository=$PACKAGE_BUNDLE_REPOSITORY \
    --data-value-yaml package.name=$PACKAGE_NAME \
    --data-value-yaml package.timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ") \
    --file-mark 'package.yml:type=text-plain' \
    --file-mark 'metadata.yml:type=text-plain' \
    --file-mark 'values-schema.yml:type=text-plain' \
    --file-mark 'values-schema.star:type=text-plain' \
    --file-mark 'values-schema.star:for-output=true' \
    --file-mark 'versions.yml:type=text-template'

ytt \
    -f $REPO_BUNDLE_RENDERED \
    --file-mark 'values-schema.yml:type=data' \
    > $REPO_BUNDLE_GENERATED/packages/packages.yml

kbld \
    --file $REPO_BUNDLE_GENERATED/packages \
    --imgpkg-lock-output $REPO_BUNDLE_GENERATED/.imgpkg/images.yml
