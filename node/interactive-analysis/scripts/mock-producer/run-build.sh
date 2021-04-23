#!/bin/bash

set -e

function run_help {
  should_print_help=1
}

function explain_basic_usage {
  ./util/validate_env_vars.sh
  echo "#############"
  echo "Mock Producer Build Help"
  echo "Runs docker-compose up and gradle docker to compile and build the necessary docker images."
  echo
  echo "Options:"
  echo "  -h | --help                            You're looking at it."
  echo "  -v | --verbose                         Turns on verbose mode for more info."
}

function compose_docker_container {
  ./util/validate_env_vars.sh
  docker-compose up -d && sleep 10s
}

function build_producer {
  ./util/validate_env_vars.sh
  pushd $GMS_COMMON_DIR/java/gms/shared/frameworks/data-injector
    CI_DOCKER_REGISTRY=local DOCKER_IMAGE_TAG=latest gradle docker
  popd
}

function info {
  ./run-info.sh $options
}

function fail_helpfully {
  ./util/offer_help.sh
  ./util/exit_with_failure.sh
}

while [[ $# -gt 0 ]]
do
key="$1"

case $key in
  -h|--help)
    run_help
    shift
    ;;
  -v|--verbose)
    verbose="I would have written a shorter placeholder, but I did not have the time."
    shift
    options="$options -v"
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

# Now run the producer with the variables set above.
compose_docker_container
build_producer

if ! [ -z "$verbose" ] ; then
  info $options
fi

exit 0;
