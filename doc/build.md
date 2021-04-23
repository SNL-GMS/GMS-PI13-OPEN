# GMS-Common Build Instructions

Follow these instructions on your local machine to build and run the
SOH deployment for **gms-common**.  The build process results in a
series of tagged Docker container images.

By default, containers are built locally but may optionally be pushed
to a remote repository.

## External Dependencies

The following external dependencies are required to build this software.

| Dependency                                            | Version  | 
|:------------------------------------------------------|:---------|
| [**Docker**](https://www.docker.com)                  | 19.03.13 |
| [**Java**](https://www.oracle.com/java)               | 11.0.7   |
| [**Gradle**](https://gradle.org)                      | 14.10.3  |
| [**Yarn**](https://yarnpkg.com)                       | 1.22.5   |
| [**Node.js**](https://nodejs.org)                     | v10.22.1 |
| [**Python**](https://yarnpkg.com)                     | 3.7.4    |
| [**Make**](https://www.gnu.org/software/make)         | 3.82     |
| [**postgresql-devel**](https://www.postgresql.org)    | 9.2.24   |

Additional dependent software libraries are downloaded by the Docker,
Gradle, and Python tools in the process of building.

## Environment

These instructions assume a bash shell.

Sourcing the `gms-common/.bash_env` will add the `gms-common/bin` and
`gms-common/ci` directories to your **PATH**.

Depending on your configuration, you will also need to set one or more
of the following environment variables.

* **CI_DOCKER_REGISTRY**<br>
  This should be set to the name of a remote docker registry used when
  building.  If not using a remote registry, set this to `local`.  <br>
  Example: *gms-docker-registry.example.com* or *local*

* **CI_REMOTE_REPOSITORY_URL**<br>
  If a repository mirror is available and configured (such as
  Artifactory or Nexus) this should be set to the URL of the remote
  repository. <br>
  Example: *https://artifactory.example.com/artifactory*

* **CI_THIRD_PARTY_DOCKER_REGISTRY**<br>
  For base images (such as *centos*, *zookeeper*, or *traefik*) you
  may specify a repository from which those base images should be
  obtained. If not specified, base images will be obtained from
  the registry specified by **CI_DOCKER_REGISTRY**. 
  Example: *unset* or *registry-1.docker.io/library*

* **CI_DOCKER_IMAGE_TAG**<br>
  Docker containers will be automatically be tagged with the current
  git commit reference (such as the. branch name) that is currently
  checked out. For gradle-built Java docker containers, this
  environment variable *must* be set to match the current git
  reference. <br>
  Example: *develop*

## Building

* **Build Base Containers**
  ```bash
  $ cd ${GMS_COMMON_HOME}/docker
  $ make all
  ```

* **Build Java**
  ```bash
  $ cd ${GMS_COMMON_HOME}/java
  $ gradle -q --no-daemon build -x test docker dockerTagImageTag dockerTagCiCommitSha
  $ gradle -q --no-daemon test 
  $ make all
  ```

* **Build Typescript**
  ```bash
  $ cd ${GMS_COMMON_HOME}/interactive-analysis
  $ yarn
  $ yarn build:prod
  $ yarn bundle:prod:soh
  $ make all
  ```
  
* **Build Python**
  ```bash
  $ cd ${GMS_COMMON_HOME}/python
  $ make all
  ```
