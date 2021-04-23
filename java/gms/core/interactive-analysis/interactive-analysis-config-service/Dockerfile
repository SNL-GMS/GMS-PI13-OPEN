ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
FROM $DOCKER_REGISTRY/gms-common/java:$DOCKER_IMAGE_TAG

WORKDIR /opt/gms

EXPOSE 8080


#Copying .jar file to /root
ADD interactive-analysis-config-service-*.tar ./
RUN mv interactive-analysis-config-service-* interactive-analysis-config-service
WORKDIR interactive-analysis-config-service

HEALTHCHECK --interval=30s --retries=10 \
 CMD curl -sfm 30 localhost:8080/alive || exit 1

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
CMD ["./bin/interactive-analysis-config-service"]

