ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
FROM $DOCKER_REGISTRY/gms-common/java:$DOCKER_IMAGE_TAG

ENV CD11_INJECTOR_CONFIG_NAME=20for10min

# Copy in the CD1.1 injector tarfile and untar
WORKDIR cd11-injector
COPY cd11-injector-*.tar .
RUN tar -xf cd11-injector-*.tar -C . --strip-components 1 && \
    rm -r cd11-injector-*.tar

# Copy in the CD1.1 RSDF data and configuration
COPY cd11-test-data /rsdf/cd11/
COPY conf /rsdf/cd11/

# Copy in the CD1.1 RSDF data and configuration
COPY run-cd11-injector.sh bin

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}
    
LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

# This script will set the CD11_INJECTOR_CONFIG_PATH and run the cd11-injector
CMD bin/run-cd11-injector.sh

