NAME = builder

BUILD_ARGS = --build-arg 'FROM_IMAGE=$${CI_THIRD_PARTY_DOCKER_REGISTRY}/centos:7' \
	     --build-arg 'CENTOS_YUM_URL=$${CENTOS_YUM_URL}' \
	     --build-arg 'EPEL_YUM_URL=$${EPEL_YUM_URL}' \
	     --build-arg 'GRADLE_BASE_URL=$${GRADLE_BASE_URL}' \
	     --build-arg 'NODE_YUM_URL=$${NODE_YUM_URL}' \
	     --build-arg 'A_NODE_PROXY=$${NODE_PROXY}' \
	     --build-arg 'NODEJS_FILES_URL=$${NODEJS_FILES_URL}' \
	     --build-arg 'YARN_YUM_URL=$${YARN_YUM_URL}' \
	     --build-arg 'A_YARN_PROXY=$${YARN_PROXY}' \
	     --build-arg 'NPM_REGISTRY=$${NPM_REGISTRY}' \
	     --build-arg 'YARN_REGISTRY=$${YARN_REGISTRY}' \
	     --build-arg 'CYPRESS_DOWNLOAD_MIRROR=$${CYPRESS_DOWNLOAD_MIRROR}' \
	     --build-arg 'ELECTRON_MIRROR=$${ELECTRON_MIRROR}' \
	     --build-arg 'ELECTRON_DIR_START=$${ELECTRON_DIR_START}' \
	     --build-arg 'ELECTRON_VERSION=$${ELECTRON_VERSION}' \
	     --build-arg 'CHROMEDRIVER_VERSION=$${CHROMEDRIVER_VERSION}' \
	     --build-arg 'MINICONDA_URL=$${MINICONDA_URL}' \
	     --build-arg 'PYPI_URL=$${PYPI_URL}' \
	     --build-arg 'SONAR_URL=$${SONAR_URL}' \
	     --build-arg 'SONAR_SCANNER_VER=$${SONAR_SCANNER_VERSION}' \
	     --build-arg 'DOCKER_YUM_URL=$${DOCKER_YUM_URL}' \
	     --build-arg 'DOCKER_PROXY=$${DOCKER_PROXY}' \
	     --build-arg 'GITHUB_URL=$${GITHUB_URL}' \
	     --build-arg 'DOCKER_COMPOSE_VER=$${DOCKER_COMPOSE_VERSION}' \
	     --build-arg 'BD_URL=$${BD_URL}' \
	     --build-arg 'BD_VER=$${BD_VERSION}'

CLEAN = rm -rf Dockerfile

include $(shell CI-MAKE-CONFIG-PATH)/Makefile.docker
