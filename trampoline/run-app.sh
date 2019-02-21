#!/usr/bin/env bash
SERVICE_NAME=trampoline

JAR_FILENAME=${SERVICE_NAME}-0.0.1-SNAPSHOT.jar
PID_PATH_NAME=./${SERVICE_NAME}.pid
MEMORY="-d64 -Xms1G -Xmx1G"
GC="-XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -XX:+HeapDumpOnOutOfMemoryError -XX:+CMSParallelRemarkEnabled  -XX:+ScavengeBeforeFullGC -XX:+CMSScavengeBeforeRemark"
URANDOM="-Djava.security.egd=file:/dev/./urandom"
LOGGING="-Dlog.level=INFO"
ENVIRONMENT="-Denv.hostname=${HOSTNAME}"

java $MEMORY $LOGGING $URANDOM $GC $ENVIRONMENT -jar ./target/${JAR_FILENAME}