#!/bin/bash
set -e

function validate_env_vars {
  [ -z "$GMS_COMMON_DIR" ] &&  echo "Required environment variable GMS_COMMON_DIR not set." && ./util/exit_with_failure.sh;
  [ ! -d "$GMS_COMMON_DIR" ] &&  echo "Required environment variable GMS_COMMON_DIR does not resolve to a directory." && ./util/exit_with_failure.sh;
  [ -z "$CI_DOCKER_REGISTRY" ] && echo "Required environment variable CI_DOCKER_REGISTRY not set." && ./util/exit_with_failure.sh;
  echo "Environment variables appear valid"
}

validate_env_vars
