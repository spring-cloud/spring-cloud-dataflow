# App Launcher

The App Launcher provides a single entry point that bootstraps app JARs located in a Maven repository. A single Docker image can then be used to launch any of those JARs based on an environment variable. When running standalone, a system property may be used instead of an environment variable, so that multiple instances of the App Launcher may run on a single machine. The following examples demonstrate running the modules for the *ticktock* stream (`time-source | log-sink`).

## Prerequisites

1: clone and build the spring-cloud-dataflow project:

````
git clone https://github.com/spring-cloud/spring-cloud-dataflow.git
cd spring-cloud-dataflow-app-launcher
mvn package
cd ..`
````

2: start redis locally via `redis-server` or `docker-compose` (there's a `docker-compose.yml` in `spring-cloud-dataflow-app-launcher`). Optionally start `redis-cli` and use the `MONITOR` command to watch activity.

*NOTE:* redis.conf (on OSX it is found here: /usr/local/etc/redis.conf) may need to be updated to set the binding to an address other than 127.0.0.1 else the docker instances will fail to connect. For example: bind 0.0.0.0

## Running Standalone

From the `spring-cloud-dataflow/spring-cloud-dataflow-app-launcher` directory:

````
java -Dmodules=org.springframework.cloud.stream.module:time-source:jar:exec:1.0.0.BUILD-SNAPSHOT -Dspring.cloud.stream.bindings.output.destination=ticktock -jar target/spring-cloud-dataflow-app-launcher-1.0.0.BUILD-SNAPSHOT.jar
java -Dmodules=org.springframework.cloud.stream.module:log-sink:jar:exec:1.0.0.BUILD-SNAPSHOT -Dargs.0.server.port=8081 -Dspring.cloud.stream.bindings.input.destination=ticktock -jar target/spring-cloud-dataflow-app-launcher-1.0.0.BUILD-SNAPSHOT.jar
````

Note that `server.port` needs to be specified explicitly for the log sink app as the time source app already uses the default port `8080`.
The aoo launcher is able to launch several apps, hence the `args.0.` prefix.
The binding property is set to use the same name `ticktock` for both the output/input bindings of source/sink apps so that the log sink receives messages from the time source.

The time messages will be emitted every second. The console for the log app will display each:

````
2015-08-26 14:21:44.546  INFO 35725 --- [hannel-adapter1] o.s.cloud.stream.module.log.LogSink      : Received: 2015-08-26 14:21:44
2015-08-26 14:21:45.548  INFO 35725 --- [hannel-adapter1] o.s.cloud.stream.module.log.LogSink      : Received: 2015-08-26 14:21:45
2015-08-26 14:21:46.550  INFO 35725 --- [hannel-adapter1] o.s.cloud.stream.module.log.LogSink      : Received: 2015-08-26 14:21:46
````

*NOTE:* the two apps will be launched within a single process if both are provided (comma-delimited) via `-Dmodules`

## Running with Docker

The easiest way to get a demo working is to use `docker-compose` (From the `spring-cloud-dataflow/spring-cloud-dataflow-app-launcher` directory):

Make sure to set `DOCKER_HOST`. If you are running a `boot2docker` VM, $(boot2docker shellinit) would set that up.

```
$ mvn package docker:build
$ docker-compose up
...
logsink_1    | 2015-08-11 08:25:49.909  INFO 1 --- [hannel-adapter1] o.s.cloud.stream.module.log.LogSink      : Received: 2015-08-11 08:25:49
logsink_1    | 2015-08-11 08:25:50.909  INFO 1 --- [hannel-adapter1] o.s.cloud.stream.module.log.LogSink      : Received: 2015-08-11 08:25:50
...
```

You can also run each app individually as a Docker process by passing environment variables for the app name as well as the host machine's IP address for the redis connection to be established within the container:
To find out redis host IP:
```
Get the container ID of redis: `docker ps`
Get the IP address by inspecting the container: `docker inspect <containerID>`

```
To run the apps individually on docker:
````
docker run -p 8080:8080 -e MODULES=org.springframework.cloud.stream.module:time-source:jar:exec:1.0.0.BUILD-SNAPSHOT \
 -e spring.cloud.stream.bindings.output.destination=ticktock -e SPRING_REDIS_HOST=<Redis-Host-IP> springcloud/dataflow-app-launcher

docker run -p 8081:8080 -e MODULES=org.springframework.cloud.stream.module:log-sink:jar:exec:1.0.0.BUILD-SNAPSHOT \
  -e spring.cloud.stream.bindings.input.destination=ticktock -e SPRING_REDIS_HOST=<Redis-Host-IP> springcloud/dataflow-app-launcher
````
Note the binding name `ticktock` is specified for the source's output and sink's input.
The port mapping is done so that individual apps' http endpoints can be accessed via the docker VM port.

To run pub/sub apps individually on docker, the binding name has to start with `topic:`.

````
docker run -p 8080:8080 -e MODULES=org.springframework.cloud.stream.module:time-source:jar:exec:1.0.0.BUILD-SNAPSHOT \
 -e spring.cloud.stream.bindings.output.destination=topic:foo -e SPRING_REDIS_HOST=<Redis-Host-IP> springcloud/dataflow-app-launcher

docker run -p 8081:8080 -e MODULES=org.springframework.cloud.stream.module:log-sink:jar:exec:1.0.0.BUILD-SNAPSHOT \
 -e spring.cloud.stream.bindings.input.destination=topic:foo -e SPRING_REDIS_HOST=<Redis-Host-IP> springcloud/dataflow-app-launcher

docker run -p 8082:8080 -e MODULES=org.springframework.cloud.stream.module:log-sink:jar:exec:1.0.0.BUILD-SNAPSHOT \
 -e spring.cloud.stream.bindings.input.destination=topic:foo -e SPRING_REDIS_HOST=<Redis-Host-IP> springcloud/dataflow-app-launcher
````
In the above scenario, both the sink apps receive the same messages from the time source.
