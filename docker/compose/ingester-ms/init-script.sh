#!/bin/sh

echo "Setting up secrets..."
export INCOMING_MQTT_HOST=$(cat /run/secrets/incoming_mqtt_host)
export INCOMING_MQTT_PORT=$(cat /run/secrets/incoming_mqtt_port)
export RABBITMQ_CLIENT_USERNAME=$(cat /run/secrets/rabbitmq_client_username)
export RABBITMQ_CLIENT_PASSWORD=$(cat /run/secrets/rabbitmq_client_password)

env

ping -c 3 $RABBITMQ_HOST

echo "Waiting for RabbitMQ to start..."
sleep 10

# Call the original Quarkus entrypoint
#java -Djava.security.egd=file:/dev/./urandom -cp @/app/jib-classpath-file io.quarkus.runner.GeneratedMain
java -jar /app/quarkus-run.jar
