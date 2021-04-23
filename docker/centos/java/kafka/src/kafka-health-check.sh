#!/bin/bash

# With docker swarm, the network is not available until the health check passes,
# but the network is required for kafka to contact its peers.
#
# We'll do a simple healthcheck and confirm that a 'java' process is running 'kafka'.

kafka_pid=$(ps ax | grep java | grep -i kafka | grep -v grep | awk '{print $1}')

if [ "$kafka_pid" != "" ]; then
  echo kafka running with pid ${kafka_pid}
  exit 0
fi

echo kafka not running
exit 1
