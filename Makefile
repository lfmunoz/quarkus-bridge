

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