#!/bin/bash
if [ "$SKIPPER_VERSION" = "" ]; then
  SKIPPER_VERSION=2.9.3-SNAPSHOT
fi

docker pull "springcloud/spring-cloud-skipper-server:$SKIPPER_VERSION"
