ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
FROM $DOCKER_REGISTRY/gms-common/java:$DOCKER_IMAGE_TAG

WORKDIR /opt/gms

EXPOSE 8080

#Copying .jar file to /root
ADD soh-application*.tar ./
RUN mv soh-application* soh-application

WORKDIR soh-application

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}

LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

# Define default command.
CMD ["./bin/soh-application"]

HEALTHCHECK --interval=30s --start-period=180s --retries=10 CMD curl -f localhost:8080/alive || exit 1

