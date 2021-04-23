ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
FROM $DOCKER_REGISTRY/gms-common/java:$DOCKER_IMAGE_TAG

WORKDIR /opt/gms

EXPOSE 8080

#Copying jar to docker container
ADD ./soh-quieted-list-kafka-consumer*.tar ./
RUN mv soh-quieted-list-kafka-consumer* soh-quieted-list-kafka-consumer

HEALTHCHECK --interval=30s CMD ps -o args -p 1 | grep java | grep soh-quieted-list-kafka-consumer

WORKDIR soh-quieted-list-kafka-consumer

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}

LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

#Running the jar in the docker container
CMD ["./bin/soh-quieted-list-kafka-consumer"]

