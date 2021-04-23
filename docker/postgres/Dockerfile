#
#  Dockerfile to make a configured etcd image for GMS.
#
ARG FROM_IMAGE
FROM ${FROM_IMAGE}

ENV CONFIGDIR /etc/postgresql

#Copy in our init scripts
COPY db-scripts/* /docker-entrypoint-initdb.d/

# copy the configuration files
COPY postgresql.conf $CONFIGDIR/
COPY pg_hba.conf $CONFIGDIR/

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}
    
LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

#Used default Postgres entrypoint

