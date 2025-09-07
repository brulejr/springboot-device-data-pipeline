#!/usr/bin/env bash

set -euo pipefail

echo "Setting up secrets..."
export RABBITMQ_DEFAULT_USER=$(cat /run/secrets/rabbitmq_admin_username)
export RABBITMQ_DEFAULT_PASS=$(cat /run/secrets/rabbitmq_admin_password)

# Call the original RabbitMQ entrypoint
/usr/local/bin/docker-entrypoint.sh "$@"
