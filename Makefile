
#________________________________________________________________________________
#  BRIDGE DEVELOPMENT
#________________________________________________________________________________
# /home/luis/.sdkman/candidates/java/19.3.1.r8-grl/bin/gu install native-image
config:
	#source /home/luis/.sdkman/bin/sdkman-init.sh
	sdk use maven 3.6.3
	sdk use java 19.3.1.r8-grl

# ./gradlew addExtension --extensions="vertx"
# ./gradlew listExtensions

.PHONY: run build

run:
	./gradlew quarkusDev


build:
	./gradlew build
	docker build -f src/main/docker/Dockerfile.jvm -t quarkus/quarkus-analytics-jvm .

package:
	./gradlew quarkusBuild --uber-jar

# Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-native-image:19.3.1-java11
package-native:
	#./gradlew buildNative --stacktrace --debug --scan
	./gradlew buildNative --docker-build=true

package-docker:
# 	 ./gradlew buildNative --docker-build=true --stacktrace
# 	 ./gradlew buildNative --debug
	 docker build -f src/main/docker/Dockerfile.native -t quarkus-quickstart/getting-started .

run-docker:
# 	docker run -i --rm -p 8080:8080 quarkus-quickstart/getting-started
	docker run -i --rm -p 8080:8080 quarkus/quarkus-analytics-jvm

run-build:
	 java -jar target/getting-started-1.0-SNAPSHOT-runner.jar


test-native:
	./gradlew testNative


#________________________________________________________________________________
#  BVT
#   FLINK: http://oc112-22.maas.auslab.2wire.com:8081/
#   KAFKA

#  PEIDONG
#     bootstrapServer = "10.37.240.46:9094"
#     rabbitmq = "amqp://guest:guest@10.37.240.51:5672"
#     http://10.37.240.51:15672/
#________________________________________________________________________________
# 10.37.120.197
ELASTIC=elastic
# 10.37.120.52
# 10.37.137.22
BVT=bvt


#________________________________________________________________________________
#  FLINK
#   https://github.com/apache/flink/tree/master/flink-container/docker
#________________________________________________________________________________

NETWORK=collect-bridge
dependencies:
	docker network create -d bridge ${NETWORK}
	docker network ls
	docker network inspect collect-bridge


#docker run --rm --name alpine_linux --network=collect-bridge  -it  alpine /bin/sh

#________________________________________________________________________________
#  FLINK
#   https://github.com/apache/flink/tree/master/flink-container/docker
#
#  Public 8081
#   http://localhost:8081

# https://stackoverflow.com/questions/44186209/how-to-configure-flink-cluster-for-logging-via-web-ui
#________________________________________________________________________________

#JOBMANAGER_CONTAINER=$(docker ps --filter name=jobmanager --format={{.ID}})
#docker cp path/to/jar "$JOBMANAGER_CONTAINER":/job.jar
#docker exec -t -i "$JOBMANAGER_CONTAINER" flink run /job.jar

FLINK_DOCKERFILE=src/main/docker/flink/Dockerfile
FLINK_DOCKER_COMPOSE=src/main/docker/flink/docker-compose.yml
FLINK_VERSION=1.8.2
FLINK_SCALA_RUNTIME=2.12
# Comma separated list to job archives
FLINK_JOB_ARCHIVES=""
FLINK_IMAGE_NAME=flink-image-current
FLINK_JOB_NAME=flink-job-current



flink-build:
# 	build.sh --from-archive <PATH_TO_ARCHIVE> --job-artifacts <COMMA_SEPARATED_PATH_TO_JOB_ARTIFACTS>  --image-name <IMAGE_NAME>
# 	cd src/main/docker/flink/; bash build.sh --from-release --flink-version ${FLINK_VERSION} --scala-version ${FLINK_SCALA_RUNTIME} --job-artifacts ${LINK_JOB_ARCHIVES} --image-name ${FLINK_IMAGE_NAME}
	cd src/main/docker/flink/; bash build.sh --from-release --flink-version ${FLINK_VERSION} --scala-version ${FLINK_SCALA_RUNTIME} --image-name ${FLINK_IMAGE_NAME}

# flink-start:
# 	DEFAULT_PARALLELISM=1 FLINK_DOCKER_IMAGE_NAME=${FLINK_IMAGE_NAME} FLINK_JOB=${FLINK_JOB_NAME} docker-compose -f ${FLINK_DOCKER_COMPOSE} up
# 	DEFAULT_PARALLELISM=1 FLINK_DOCKER_IMAGE_NAME=${FLINK_IMAGE_NAME} FLINK_JOB=${FLINK_JOB_NAME} docker-compose -f ${FLINK_DOCKER_COMPOSE} up -d

# flink-stop:
# 	docker-compose -f ${FLINK_DOCKERFILE} kill

#  docker-compose scale taskmanager=<N>
#    docker exec -it $(docker ps --filter name=flink_jobmanager --format={{.ID}}) /bin/sh

# CLUSTER_COMMAND=job-cluster --job-classname ${FLINK_JOB} -Djobmanager.rpc.address=flink-cluster -Dparallelism.default=1 ${SAVEPOINT_OPTIONS} ${FLINK_JOB_ARGUMENTS}
# CLUSTER_COMMAND=job-cluster --job-classname ${FLINK_JOB_NAME} -Djobmanager.rpc.address=flink-cluster -Dparallelism.default=1
# CLUSTER_COMMAND=job-cluster -Djobmanager.rpc.address=flink-cluster -Dparallelism.default=1
# TASK_COMMAND=task-manager -Djobmanager.rpc.address=flink-cluster
# 	 docker run --rm --name flink-cluster -p  8081:8081 --network=${NETWORK} ${FLINK_IMAGE_NAME} ${CLUSTER_COMMAND}
# 	 docker run --rm --name flink-task-1 --network=${NETWORK} ${FLINK_IMAGE_NAME} ${TASK_COMMAND}







#docker run --rm --name jobmanager -p  8081:8081 --network=collect-bridge -e JOB_MANAGER_RPC_ADDRESS=jobmanager  -it flink:1.8.2-scala_2.12 jobmanager
#docker run --rm --name taskmanager  --network=collect-bridge -e JOB_MANAGER_RPC_ADDRESS=jobmanager -it flink:1.8.2-scala_2.12 taskmanager

# https://hub.docker.com/_/flink?tab=tags
# docker run --name flink_jobmanager -d -t flink jobmanager
# docker run --name flink_taskmanager -d -t flink taskmanager
# The Web Client is on port 8081
# JobManager RPC port 6123
# TaskManagers RPC port 6122
# TaskManagers Data port 6121
FLINK_OFFICIAL_IMAGE=flink:1.8.2-scala_2.12

flink-start:
	 docker run --rm -d --name jobmanager -p  8081:8081 --network=${NETWORK} -e JOB_MANAGER_RPC_ADDRESS=jobmanager  ${FLINK_OFFICIAL_IMAGE} jobmanager
	 docker run --rm -d --name taskmanage  --network=${NETWORK}  -e JOB_MANAGER_RPC_ADDRESS=jobmanager ${FLINK_OFFICIAL_IMAGE} taskmanager
# 	 docker exec -it jobmanager bash

flink-stop:
	 docker stop jobmanager
	 docker stop taskmanage


flink-upload:
	 curl -X POST -H "Expect:" -F "jarfile=@path/to/flink-job.jar" http://hostname:8081/jars/upload


# --KafkaBootstrap 10.37.240.46:9094 --KafkaGroupId ep_read_group --KafkaTopic element-ts --KafkaDiagnosticTopic razvan-topic --LocalEnv true
#172.20.0.2
#________________________________________________________________________________
# Zookepper
#  Public port 2181 2888 3888 8091
#________________________________________________________________________________
zookeeper-start:
	 docker run -d --rm --name zookeeper -p 2181:2181 -p 2888:2888 -p 3888:3888 -p 8091:8080 --network=${NETWORK} -e "ZOO_4LW_COMMANDS_WHITELIST=*" zookeeper:3.5.5

zoo-stop:
	docker stop zookeeper

zookeeper-ui-start:
	docker run -d --rm --name zkui -p 1090:9090 --network=${NETWORK} -e ZK_SERVER=zookeeper:2181 qnib/plain-zkui

zookeeper-ui-stop:
	docker stop zkui

zookeeper-log:
	docker logs zookeeper

#________________________________________________________________________________
# Kafka
#  Public port 9092
#________________________________________________________________________________
KAFKA_ADVERTISED_HOST_NAME=10.37.120.197

kafka-start:
	docker run -d --rm --name kafka -p 9092:9092 --network=${NETWORK} -e "ZOOKEEPER_CONNECT=zookeeper:2181" -e "ADVERTISED_HOST_NAME=${KAFKA_ADVERTISED_HOST_NAME}" debezium/kafka:1.0

kafka-stop:
	docker stop kafka

#________________________________________________________________________________
# RabbitMQ
# #  Public port 5672  15672
#________________________________________________________________________________
rabbit-start:
	docker run --name rabbitmq-collect --rm --network=${NETWORK} -p 5672:5672 -p 15672:15672 -d rabbitmq:3.7.2-management

rabbit-stop:
	docker stop rabbitmq-collect

#________________________________________________________________________________
# Grafana / Prometheus
#  Prometheus exposes 9090
#  Grafana exposes 3000
#________________________________________________________________________________
grafana-start:
	docker run -d --rm --name grafana --network=${NETWORK} -p 3000:3000 grafana/grafana

prometheus-start:
	docker run -d --rm --name prometheus --network=${NETWORK} -p 9090:9090 -v ~/prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus
