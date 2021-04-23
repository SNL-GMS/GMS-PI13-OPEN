#!/bin/bash

set -ex

# --------------------------------------------------------
# Convert the gms-common CSS data to COI JSON in place
# 
# The source CSS files live under this directory in:
#   station-reference/stationdata
#
# The destination COI JSON files are created in:
#   station-reference/data
# --------------------------------------------------------

SCRIPT_PATH=$( cd $( dirname "${BASH_SOURCE[0]}" ) > /dev/null && pwd)

# Assume this is being called from the parent directory
STATION_DATA_PATH="${SCRIPT_PATH}/station-reference"
CSS_FULL_PATH="$STATION_DATA_PATH/data"
JSON_FULL_PATH="$STATION_DATA_PATH/stationdata"
NETWORK_FILE_NAME="network.dat"

# Set full paths to the source CSS data and the destination JSON data

# Set environment variable needed by the script that converts CSS to JSON
export GMS_HOME=${SCRIPT_PATH}/../java

# Ensure that the Java `css-stationref-converter` program is built.
time gradle -p ${GMS_HOME} :css-stationref-converter:build --no-daemon

# If the JSON_FULL_PATH subdirectory exists remove it.
if [ -d "${JSON_FULL_PATH}" ]; then
    rm -rf ${JSON_FULL_PATH};  # The css-stationref-converter requires this be missing when running outside a container
fi

# If running in CI then make sure JSON_FULL_PATH exists, since CI runs in a container
if [[ -z "${CI}" ]]; then
    echo "Running outside of CI";
else
    echo "Running in CI, creating ${JSON_FULL_PATH}";
    mkdir -p ${JSON_FULL_PATH};
fi

# Run the java code to generate the COI JSON data from the CSS source
cd ${STATION_DATA_PATH}

echo "*** Running station reference converter ***"
time gradle -p ${GMS_HOME} :css-stationref-converter:run -Dexec.args="-data ${CSS_FULL_PATH} -outputDir ${JSON_FULL_PATH} -network ${NETWORK_FILE_NAME}" --no-daemon
echo "*** Done running station reference converter ***"

