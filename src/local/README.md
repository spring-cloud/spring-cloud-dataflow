# Local Development Support

The files in this folder are to support local testing and development.

These scripts are useful when you want to build the containers on a different architecture and publish to a private repo.

## `download-apps.sh`
Downloads all applications needed by `create-containers.sh` from Maven repository.

*If the timestamp of snapshots matches the download will be skipped.*

Usage: `download-apps.sh [version]`

## `create-containers.sh`
Creates all containers and pushes to local docker registry.

Usage: `create-containers.sh [version] [jre-version]`

## `simple-integration-test.sh`

Execute simple Integration tests using MariaDB.
