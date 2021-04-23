NAME       = gmskube
BUILD_ARGS = --build-arg 'GOOGLEAPIS_URL=$${GOOGLEAPIS_URL}' \
             --build-arg 'HELM_URL=$${HELM_URL}' \
             --build-arg 'KUBECTL_VERSION=$${KUBECTL_VERSION}' \
             --build-arg 'HELM_VERSION=$${HELM_VERSION}'

CLEAN = rm -rf _python _config _deploy  ../config/station-reference/stationdata

include $(shell CI-MAKE-CONFIG-PATH)/Makefile.docker
