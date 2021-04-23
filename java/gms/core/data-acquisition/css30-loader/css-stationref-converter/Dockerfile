ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
FROM $DOCKER_REGISTRY/gms-common/java:$DOCKER_IMAGE_TAG

# Copy tars, untar them
WORKDIR /css-stationref-converter/

RUN mkdir css-root

COPY ./Metadata css-root

COPY ./css-stationref-converter-*.tar ./

RUN set -ex && \
    mkdir css-stationref-converter && \
    tar -xf css-stationref-converter-*.tar --strip 1 -C css-stationref-converter && \
    rm -f css-stationref-converter-*.tar && \
    chgrp -R 0 /css-stationref-converter && \
    chmod -R g=u /css-stationref-converter && \
    chmod -R o=u /css-stationref-converter && \
    chmod -R g+s /css-stationref-converter

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}

LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

ENTRYPOINT ["./css-stationref-converter/bin/css-stationref-converter"]
