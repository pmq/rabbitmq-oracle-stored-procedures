#!/bin/sh -e

USER_PASS='hr/hr'
SCHEMA='HR'
LOADJAVA_OPTS="-v -u $USER_PASS"

# mvn clean package
mvn dependency:copy-dependencies

cd target/dependency

rm -rf extract
mkdir extract

for jarfile in *.jar
do
	unzip -o $jarfile -d extract
done

cd extract

# cleanup
find . -type f -not -iname *.class -delete
rm -rf META-INF
# rm *.html

# useless classes for runtime
rm -rf org/junit
rm -rf junit
rm -rf org/hamcrest

# classes using unresolvable references
rm -rf org/springframework/amqp/rabbit/log4j
rm -rf org/springframework/beans/factory/access/el

# make a new jar
jar cf orcl-rabbitmq.jar com org

# load these classes into Oracle
loadjava "${LOADJAVA_OPTS}" -resolve -resolver "((* $SCHEMA) (* PUBLIC) (* -))" orcl-rabbitmq.jar
