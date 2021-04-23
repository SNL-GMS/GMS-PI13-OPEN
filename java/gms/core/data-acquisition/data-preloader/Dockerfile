# Run the pre-loader

ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
FROM $DOCKER_REGISTRY/gms-common/java:$DOCKER_IMAGE_TAG

# Copy tars, untar them
WORKDIR /data-preloader/

COPY ./data-preloader-*.tar * ./

RUN set -ex && \
    mkdir data-preloader && \
    tar -xf data-preloader-*.tar --strip 1 -C data-preloader && \
    rm -f data-preloader-*.tar && \
    chgrp -R 0 /data-preloader && \
    chmod -R g=u /data-preloader && \
    chmod -R g+s /data-preloader && \
    chmod +x /data-preloader/data-preloader/load-soh-data.sh

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA
ARG CI_VERSION=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME} \
    CI_VERSION=${CI_VERSION}

LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME} \
      gms.ci.version=${CI_VERSION}

# Start the pre-loader
###PARAMETER COUNT                  $1               $2              $3                   $4            $5                 $6                      $7                   $8                             $9                   $10                            $11                   $12                             $13             $14
CMD data-preloader/load-soh-data.sh ${STATION_GROUP} ${DAYS_TO_LOAD} ${DAYS_AGO_TO_START} ${LOAD_RSDFS} ${RECEPTION_DELAY} ${RSDF_SAMPLE_DURATION} ${LOAD_STATION_SOHS} ${STATION_SOH_SAMPLE_DURATION} ${LOAD_ANALOG_ACEIS} ${ACEI_ANALOG_SAMPLE_DURATION} ${LOAD_BOOLEAN_ACEIS} ${ACEI_BOOLEAN_SAMPLE_DURATION} ${LOAD_ROLLUPS} ${ROLLUP_SAMPLE_DURATION}
