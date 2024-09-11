#!/usr/bin/env bash

function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "env var $1 not defined"
    if ((sourced != 0)); then
      return 1
    else
      exit 1
    fi
  fi
}

TMP=$(mktemp -d)
if [ "$PACKAGE_BUNDLE_GENERATED" = "" ]; then
    export PACKAGE_BUNDLE_GENERATED="$TMP/generated/packagebundle"
fi
mkdir -p "$PACKAGE_BUNDLE_GENERATED"
if [ "$IMGPKG_LOCK_GENERATED_IN" = "" ]; then
    export IMGPKG_LOCK_GENERATED_IN="$TMP/generated/imgpkgin"
fi
mkdir -p "$IMGPKG_LOCK_GENERATED_IN"
if [ "$IMGPKG_LOCK_GENERATED_OUT" = "" ]; then
    export IMGPKG_LOCK_GENERATED_OUT="$TMP/generated/imgpkgout"
fi
mkdir -p "$IMGPKG_LOCK_GENERATED_OUT"

check_env PACKAGE_BUNDLE_TEMPLATE
check_env SERVER_VERSION
check_env SERVER_REPOSITORY
check_env DATAFLOW_VERSION
check_env SKIPPER_VERSION
check_env SKIPPER_REPOSITORY
check_env PACKAGE_NAME
check_env IMGPKG_LOCK_TEMPLATE
check_env VENDIR_SRC_IN

echo "Build Package Bundle: $PACKAGE_BUNDLE_TEMPLATE package.name=$PACKAGE_NAME, server.repository=$SERVER_REPOSITORY, server.version=$SERVER_VERSION,skipper.repository=$SKIPPER_REPOSITORY, skipper.version=$SKIPPER_VERSION, output=$PACKAGE_BUNDLE_GENERATED"
set +e
time ls > /dev/null 2>&1
RC=$?
if((RC=0)); then
  MEASURE="time -v -o times.txt -a"
else
  MEASURE=""
fi
set -e
echo "ytt -f $PACKAGE_BUNDLE_TEMPLATE" > times.txt

$MEASURE ytt -f "$PACKAGE_BUNDLE_TEMPLATE" \
    --output-files "$PACKAGE_BUNDLE_GENERATED" \
    --data-value-yaml server.version="$SERVER_VERSION" \
    --data-value-yaml server.repository="$SERVER_REPOSITORY" \
    --data-value-yaml ctr.version="$DATAFLOW_VERSION" \
    --data-value-yaml dataflow.version="$DATAFLOW_VERSION" \
    --data-value-yaml skipper.version="$SKIPPER_VERSION" \
    --data-value-yaml skipper.repository="$SKIPPER_REPOSITORY" \
    --data-value-yaml grafana.version="$DATAFLOW_VERSION" \
    --data-value-yaml package.name="$PACKAGE_NAME" \
    --file-mark 'config/values.yml:type=text-template' \
    --file-mark '.imgpkg/bundle.yaml:type=text-template'
echo "ytt -f $IMGPKG_LOCK_TEMPLATE" >> times.txt
$MEASURE ytt -f "$IMGPKG_LOCK_TEMPLATE" \
    --output-files "$IMGPKG_LOCK_GENERATED_IN" \
    --data-value-yaml server.version="$SERVER_VERSION" \
    --data-value-yaml server.repository="$SERVER_REPOSITORY" \
    --data-value-yaml ctr.version="$DATAFLOW_VERSION" \
    --data-value-yaml dataflow.version="$DATAFLOW_VERSION" \
    --data-value-yaml skipper.version="$SKIPPER_VERSION" \
    --data-value-yaml skipper.repository="$SKIPPER_REPOSITORY" \
    --data-value-yaml grafana.version="$DATAFLOW_VERSION" \
    --file-mark '**/*.yml:type=text-template'

mkdir -p "$PACKAGE_BUNDLE_GENERATED/config/upstream"
cp -R "$VENDIR_SRC_IN" "$PACKAGE_BUNDLE_GENERATED/config/upstream"
echo "vendir -f $IMGPKG_LOCK_TEMPLATE" >> times.txt
$MEASURE vendir sync --chdir "$PACKAGE_BUNDLE_GENERATED"
mkdir -p "$IMGPKG_LOCK_GENERATED_OUT"

for DIR in $(ls $IMGPKG_LOCK_GENERATED_IN); do
    echo "ytt for $DIR" >> times.txt
    $MEASURE ytt -f "$PACKAGE_BUNDLE_GENERATED" -f "$IMGPKG_LOCK_GENERATED_IN/$DIR" > "$IMGPKG_LOCK_GENERATED_OUT/$DIR.yml"
done

mkdir -p "$PACKAGE_BUNDLE_GENERATED/.imgpkg"
echo "kbld -f $IMGPKG_LOCK_GENERATED_OUT" >> times.txt
$MEASURE kbld -f "$IMGPKG_LOCK_GENERATED_OUT" \
    --imgpkg-lock-output "$PACKAGE_BUNDLE_GENERATED/.imgpkg/images.yml"

cat times.txt