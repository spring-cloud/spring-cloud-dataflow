#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
pushd "$SCDIR" > /dev/null || exit
    ./mvnw install -am -pl :spring-cloud-dataflow-classic-docs,:spring-cloud-dataflow-docs,:spring-cloud-skipper-docs -DskipTests
    ./mvnw install -Pfull,asciidoctordocs -pl :spring-cloud-dataflow-classic-docs,:spring-cloud-dataflow-docs,:spring-cloud-skipper-docs
popd > /dev/null || exit
