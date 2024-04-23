# Local Development Support

The files in this folder are to support local testing and development.

These scripts are useful when you want to build the containers on a different architecture and publish to a private repo.

## `download-apps.sh`
Downloads all applications needed by `create-containers.sh` from Maven repository.

*If the timestamp of snapshots matches the download will be skipped.*

Usage: `download-apps.sh [version]`
* `version` is the dataflow-server version like `2.10.3`. Default is `2.11.3-SNAPSHOT`

## `create-containers.sh`
Creates all containers and pushes to local docker registry.

This script requires [jib-cli](https://github.com/GoogleContainerTools/jib/tree/master/jib-cli)

Usage: `create-containers.sh [version] [jre-version]`
* `version` is the dataflow-server version like `2.9.6`. Default is `2.11.3-SNAPSHOT`
* `jre-version` should be one of 11, 17. Default is 11
