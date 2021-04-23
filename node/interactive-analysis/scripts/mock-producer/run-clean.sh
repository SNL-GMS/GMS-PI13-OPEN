#!/bin/bash
set -e

function schedule_run_help {
  should_print_help=1
}

function explain_basic_usage {
  echo "#############"
  echo "SOH Producer Clean Help"
  echo "Brings down the mock-producer docker containers. Then stops each container. Then prunes containers, volumes, and images. Then removes the frameworks-data-injector image."
  echo "If verbose mode is set, it will also print out info about what potentially relevant containers, images and volumes are found."
  echo
  echo "Options:"
  echo "  -h | --help                            You're looking at it."
  echo "  -v | --verbose                         Turns on verbose mode for more info."
}

function clean_up {
  echo "Clean: Freeing up Docker data"
  set +e
  ./run-stop.sh
  verify_no_ucp
  docker stop zookeeper
  docker stop kafka
  docker stop producer_etcd_1
  docker stop frameworks-data-injector
  image_hash="$(docker image ls -aqf 'reference=local/gms-common/frameworks-data-injector:latest')"
  docker image rm $image_hash
  docker system prune && docker volume prune && docker image prune
  if ! [ -z "$verbose" ] ; then
    ./run-info.sh
  fi
  set -e
}

function verify_no_ucp {
  if ! [ -z "$DOCKER_HOST" ] ; then
    echo "It looks like you are connected to a docker enterprise swarm: \$DOCKER_HOST=$DOCKER_HOST."
    echo "This could cause unintended and severe consequences. Please run in a terminal that is not connected to $DOCKER_HOST"
    exit 1
    ./util/exit_with_failure.sh
  fi
}

echo "mock-producer clean $@"

while [[ $# -gt 0 ]]
do
key="$1"

case $key in
  -h|--help)
    schedule_run_help
    shift
    ;;
  -v|--verbose)
    verbose="I would have written a shorter placeholder, but I did not have the time."
    shift
    ;;
  *)    # unknown option
    echo "Unknown option $1"
    ./util/offer_help.sh
    ./util/exit_with_failure.sh
    shift # past argument
    ;;
esac
done

if ! [ -z "$should_print_help" ] ; then
  explain_basic_usage
  exit 0
fi

clean_up

