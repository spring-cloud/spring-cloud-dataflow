#!/usr/bin/env bash

DC_FILE=docker-compose.yml
SERVICE=dataflow-server

while [ "$1" != "" ]; do
    case $1 in
    "--file" | "-f")
        DC_FILE=$2
        shift
      ;;
    "--service" | "-s")
        SERVICE=$2
        shift
        ;;
    "--platform" | "-p")
        PLATFORM=$2
        shift
        ;;
    "-h" | "--help")
        echo "Usage: --platform <platform> [--service <service>] [--file <docker-compose-file>]"
        echo "Where:"
        echo "  --platform | -p: <platform> like linux/amd64 or linux/arm"
        echo "  --service | -s: <service> name of service in docker compose file"
        echo "  --file | -f: <docker-compose-file> docker compose file"
    esac
    shift
done

if [ ! -f "$DC_FILE" ]; then
    echo "File not found $DC_FILE"
    exit 2
fi

if [ "$PLATFORM" = "" ]; then
    echo "platorm is required. One of linux/amd64 or linux/arm64"
    exit 1
fi
echo "Checking service $SERVICE in $DC_FILE"
IMAGE=$(yq -r ".services.${SERVICE}.image" "$DC_FILE")
if [ "$IMAGE" = "" ] || [ "$IMAGE" = "null" ]; then
    echo "The platform entry need to exist next to image attribute"
    exit 2
fi

CURRENT_PLATFORM=$(yq -r ".services.${SERVICE}.platform" "$DC_FILE")
if [ "$CURRENT_PLATFORM" != "" ] && [ "$CURRENT_PLATFORM" != "null" ]; then
    if [ "$PLATFORM" = "$CURRENT_PLATFORM" ]; then
        echo "Platform $CURRENT_PLATFORM already set."
        exit 0
    fi
fi
yq ".services.${SERVICE}.platform=\"${PLATFORM}\"" -i "$DC_FILE"
echo "Setting platform to $PLATFORM"
