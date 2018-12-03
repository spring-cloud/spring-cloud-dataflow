#!/bin/bash
systemproperties=${ACCEPTANCE_TEST_SYSTEM_PROPERTIES:=}
sleeptime=${ACCEPTANCE_TEST_START_WAIT:=0}
echo sleeping $sleeptime
sleep $sleeptime
exec java $systemproperties -Djava.security.egd=file:/dev/./urandom -jar /app.jar
