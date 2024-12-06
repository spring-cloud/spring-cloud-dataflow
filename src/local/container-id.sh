#!/bin/bash
if [ "$1" == "" ]; then
  echo "Usage <container-name>"
  exit 1
fi
ID=$(docker ps --filter "name=$1" --format "{{.ID}}")
echo "$ID"