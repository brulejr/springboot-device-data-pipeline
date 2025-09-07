#!/bin/sh

echo "Setting up secrets..."
export MONGODB_USERNAME=$(cat /run/secrets/mongodb_username)
export MONGODB_PASSWORD=$(cat /run/secrets/mongodb_password)
export RABBITMQ_CLIENT_USERNAME=$(cat /run/secrets/rabbitmq_client_username)
export RABBITMQ_CLIENT_PASSWORD=$(cat /run/secrets/rabbitmq_client_password)

env

ping -c 3 $MONGODB_HOST

ping -c 3 $RABBITMQ_HOST

echo "Waiting for RabbitMQ to start..."
sleep 10

# Call the original Quarkus entrypoint
java -jar /app/quarkus-run.jar
