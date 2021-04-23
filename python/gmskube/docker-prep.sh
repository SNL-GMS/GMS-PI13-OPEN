#!/bin/bash

set -ex

SCRIPT_PATH=$( cd $( dirname "${BASH_SOURCE[0]}" ) > /dev/null && pwd)
PYTHON_PATH="${SCRIPT_PATH}/.."
DEPLOY_PATH="${SCRIPT_PATH}/../../deploy"

# Copy dependent python libraries to a local python directory
if [ ! -d "_python" ]; then mkdir "_python"; fi
cp -r ${PYTHON_PATH}/gms-utils _python

# Copy the contents of the deploy directory from gms-common to the current directory
if [ ! -d "_deploy" ]; then mkdir "_deploy"; fi
cp -r ${DEPLOY_PATH}/* _deploy
