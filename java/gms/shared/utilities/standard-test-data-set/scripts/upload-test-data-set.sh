#!/usr/bin/env bash

# Exit on errors
set -e

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
  echo "Usage: <directory of GMS data files e.g. gms_test_data_set> <fk files directory> <waveform files directory>"
  exit 1
fi

check_dir_exists () {
if [ ! -d "${1}" ]; then
  echo "${1} does not exist or is not directory"
  exit 1
fi
}

inputDir=$1
check_dir_exists ${inputDir}
fkDir=$2
check_dir_exists ${fkDir}
waveformsDir=$3
check_dir_exists ${waveformsDir}

echo "*** Loading test data set ***"

# Define workingDir at level gradle commands work at.
declare workingDir

if [ ! -z "${GMS_HOME}" ]; then
 workingDir=${GMS_HOME}
 echo "Working dir $workingDir set by environment variable GMS_HOME"
else
 scriptDir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
 workingDir=${scriptDir}/../../../../../
 echo "Working dir $workingDir set as relative to this script"
fi

# If the ${GMS_LOAD_WITHOUT_GRADLE} environment variable is not
# defined, use the traditional gradle-based approach to load the waveform data.
# If it is defined, then run `coi-data-loader` directly without using Gradle.
# This second condition is utilized by the standard-test-data-set-loader Docker
# image that gets built automatically by a CI pipeline on `gms-common`.
loader_args="-waveformClaimCheck ${inputDir}/segments-and-soh/segment-claim-checks.json \
      -wfDir ${waveformsDir} \
      -masks ${inputDir}/converted-qc-masks.json \
      -fkDir ${fkDir}\
      -sigDets ${inputDir}/signal-detections.json"
if [ ! -z "${GMS_LOAD_WITHOUT_GRADLE}" ]; then
    echo "*** Loading without Gradle ***"
    coi-data-loader ${loader_args}
else
    echo "*** Loading with Gradle ***"
    gradle -p ${workingDir} :coi-data-loader:run -Dexec.args="${loader_args}"
fi

echo "*** Done uploading the test data set ***"
