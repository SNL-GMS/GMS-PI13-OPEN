#!/bin/sh

# construct the CD11_INJECTOR_CONFIG_PATH environment variable based on the desired config name
export CD11_INJECTOR_CONFIG_PATH=/rsdf/cd11/${CD11_INJECTOR_CONFIG_NAME}-config.json

# run the injector with the CD11_INJECTOR_CONFIG_PATH in the environment
bin/cd11-injector run gms.dataacquisition.stationreceiver.cd11.injector.Cd11MainVerticle

