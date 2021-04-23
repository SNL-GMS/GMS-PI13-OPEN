#!/usr/bin/env bash

# Exit on errors
set -e

if [ $# -ne 2 ]; then
  echo "Usage requires 2 arguments: ./create-test-data-set.sh <inputDir> <station_ref_gen_files_dir>"
  echo "    i.e. ./create-test-data-set.sh test-data/standard_test_data_set gms_test_data"
  exit 1
fi

inputDir=$1
outputDir=${inputDir}/gms_test_data_set
mkdir -p ${outputDir}

station_ref_gen_files_dir=$2

commonFileName=ueb_test

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

echo "*** Rsync-ing generated station reference data ***"
time rsync -a $station_ref_gen_files_dir/ $outputDir
echo "*** Rsync-ing generated station reference data ***"

echo "*** Running beam definition converter ***"
gradle -p ${workingDir} :beam-converter:run -Dexec.args=\
"-beamDefinitionDir ${inputDir}/beam-definitions/  -outputDir ${outputDir}"
echo "*** Done running beam definition converter ***"

echo "*** Running qc mask converter ***"
gradle -p ${workingDir} :qc-mask-converter:run -Dexec.args=\
"-qcDir ${inputDir}/masks/ -outputDir ${outputDir}"
echo "*** Done running qc mask converter ***"

echo "*** Running event and signal detection converter ***"
gradle -p ${workingDir} :css-processing-converter:run -Dexec.args=\
"-outputDir ${outputDir} \
-wfidToChannel ${inputDir}/WfidToDerivedChannel.json \
-wfdisc ${inputDir}/${commonFileName}.wfdisc \
-aridToWfid ${inputDir}/Arid2Wfid.json \
-event ${inputDir}/${commonFileName}.event \
-origin ${inputDir}/${commonFileName}.origin \
-origerr ${inputDir}/${commonFileName}.origerr \
-assoc ${inputDir}/${commonFileName}.assoc \
-arrival ${inputDir}/${commonFileName}.arrival \
-amplitude ${inputDir}/${commonFileName}.amplitude \
-netmag ${inputDir}/${commonFileName}.netmag \
-stamag ${inputDir}/${commonFileName}.stamag"
echo "*** Done running event and signal detection converter ***"


echo "*** Running waveform converter ***"
gradle -p ${workingDir} :css-waveform-converter:run -Dexec.args=\
"-wfDiscFile ${inputDir}/${commonFileName}.wfdisc
-stationGroupFile ${outputDir}/processing-station-group.json
-outputDir ${outputDir}
-wfidToChannelFile ${inputDir}/WfidToDerivedChannel.json"
echo "*** Done running waveform converter ***"

echo "*** Done ***"

