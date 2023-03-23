#!/bin/bash
if [ "$DATAFLOW_VERSION" = "" ]; then
  DATAFLOW_VERSION=2.11.0-SNAPSHOT
fi
docker pull "springcloud/spring-cloud-dataflow-composed-task-runner:$DATAFLOW_VERSION"
