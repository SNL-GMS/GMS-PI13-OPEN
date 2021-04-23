ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
FROM $DOCKER_REGISTRY/gms-common/java:$DOCKER_IMAGE_TAG

WORKDIR /opt/gms

EXPOSE 8080

#Copying .jar file to /root
ADD smds-application*.tar ./
RUN mv smds-application* smds-application

WORKDIR smds-application

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

# Define default command.
CMD ["./bin/smds-application"]

HEALTHCHECK --interval=30s --start-period=90s --retries=10 CMD curl -f localhost:8080/alive || exit 1

