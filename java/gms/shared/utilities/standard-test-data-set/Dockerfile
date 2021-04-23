ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
FROM $DOCKER_REGISTRY/gms-common/java:$DOCKER_IMAGE_TAG

# Copy in the test data files and loader tools.
COPY w /standard-test-data-set/w
COPY FkSpectra /standard-test-data-set/FkSpectra
COPY feature-prediction /standard-test-data-set/feature-prediction
COPY scripts /standard-test-data-set/scripts
COPY coi-data-loader /standard-test-data-set/coi-data-loader
COPY gms_test_data_set /standard-test-data-set/gms_test_data_set
COPY network-soh-status /standard-test-data-set/network-soh-status

WORKDIR /standard-test-data-set/scripts

# Environment variables used by the `upload-test-data-set.sh` script.
# With $GMS_LOAD_WAVEFORMS_WITHOUT_GRADLE defined, the
# `scripts/upload-channel-segments.sh` script will run the waveform loader tool
# embedded in the Docker image without using Gradle.  The addition to $PATH
# allows `scripts/upload-channel-segments.sh` to run `waveform-loader` easily.
ENV SD_URL=osd-signaldetection-repository-service:8080 \
    WAVEFORMS_URL=osd-waveforms-repository-service:8080 \
    PERFORM_MONITOR_URL=osd-performance-monitoring-service:8080 \
    GMS_LOAD_WITHOUT_GRADLE=1 \
    PATH=/standard-test-data-set/scripts:/standard-test-data-set/coi-data-loader/bin:${PATH}

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}
    
LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

# The default command will load all of the test data into running OSD services.
CMD ["upload-test-data-set.sh", "/standard-test-data-set/gms_test_data_set", "/standard-test-data-set/FkSpectra/ChanSeg", "/standard-test-data-set/w"]

# This command will copy the data needed by the
# `interactive-analysis-api-gateway` into the (empty)
# `interactive-analysis-api-gateway-volume` mounted at `/api-gateway-volume`.
#CMD ["load-api-gateway.sh", "/standard-test-data-set", "/api-gateway-volume/Standard_Test_Data"]

