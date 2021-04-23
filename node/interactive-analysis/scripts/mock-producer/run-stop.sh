#!/bin/bash

function schedule_run_help {
  should_print_help=1
}

function explain_basic_usage {
  ./util/validate_env_vars.sh
  echo "#############"
  echo "Mock Producer Stop Help"
  echo "runs docker-compose down. If verbose mode is on, it also spits out info about potentally relevant containers, images and networks."
  echo
  echo "Options:"
  echo "  -h | --help                            You're looking at it."
  echo "  -v | --verbose                         Turns on verbose mode for more info."
}

function stop_it {
  docker-compose down
  if ! [ -z "$verbose" ] ; then 
    ./run-info.sh
  fi
}

function stop_containers {
  docker stop $(docker ps -a -q  --filter ancestor=$1)
}

function stop_producers {
  stop_containers local/gms-common/frameworks-data-injector:latest
}

function stop_etcd {
  stop_containers "$CI_DOCKER_REGISTRY/gms-common/etcd:develop"
}

echo "mock-producer stop $@"
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

stop_producers
stop_etcd
stop_it
