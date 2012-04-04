#!/bin/sh -e

USER_PASS='hr/hr'
SCHEMA='HR'
LOADJAVA_OPTS="-v -u $USER_PASS"

mvn clean package
mvn dependency:copy-dependencies

# load dependencies
loadjava "${LOADJAVA_OPTS}" -resolve -resolver "((* $SCHEMA) (* PUBLIC) (* -))" target/dependency/commons-logging*.jar
loadjava "${LOADJAVA_OPTS}" -resolve -resolver "((* $SCHEMA) (* PUBLIC) (* -))" target/dependency/amqp-client*.jar

# load the actual Java procedures
loadjava "${LOADJAVA_OPTS}" -resolve -resolver "((* $SCHEMA) (* PUBLIC) (* -))" target/classes/com/zenika/oracle/amqp/*.class
