#!/bin/bash

rm -rf config stationdata
mkdir config stationdata

# Copy config files from config repo into local dir to be packaged into docker image
cp -rf ../../config/processing config/processing
cp -rf ../../config/station-reference/station_ref_data stationdata




