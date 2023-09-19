#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
pushd "$SCDIR" > /dev/null || exit
    ./mvnw install -DskipTests -T 1C
    ./mvnw install -o -Pfull,asciidoctordocs,restdocs -pl :spring-cloud-dataflow-classic-docs,:spring-cloud-dataflow-docs
    ./mvnw install -o -Pasciidoctordocs,restdocs -pl :spring-cloud-skipper-server-core,:spring-cloud-skipper-docs
popd > /dev/null || exit
