ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
ARG PROJECT_NAME

FROM ${DOCKER_REGISTRY}/${PROJECT_NAME}/centos:${DOCKER_IMAGE_TAG}

# Set Java Home (required by gradle palantir plugin)
ENV JAVA_HOME /usr/lib/jvm/jre-11-openjdk

# install openjdk11
RUN set -ex && \
    yum -y install java-11-openjdk.x86_64 && \
    yum clean all

CMD ["/bin/bash"]

