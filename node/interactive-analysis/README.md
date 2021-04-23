# Interactive Analysis UI

Front end for the GMS Interactive Analysis.

This project consists of multiple sub-projects connected using [lerna](https://github.com/lerna/lerna).
Sub-Projects:

* ui-app -> main user interface components, organized using golden layout
* ui-electron -> runs ui-app in an electron stand-alone window
* ui-core-components -> library of reusable ui components
* weavess -> library used in analysis to visualize waveform data

## Configuration

/packages/ui-app/src/ts/workspaces/analyst-ui/config/user-preferences.ts

* Contains the configuration accessible to the user including color and configurable visual components

## Installation

* Install [Nodejs v10](https://nodejs.org/en/download/)

Then,

```bash
[ .../interactive_analysis_ui] $ yarn
[ .../interactive_analysis_ui] $ yarn build
```

## Build Scripts

* `clean`: runs a lerna clean to remove all node_modules
* `build:dev`: runs a lerna development build for all sub packages
* `build:prod`: runs a lerna production build for all sub packages
* `build`: runs a lerna production build for all sub packages
* `start:dev`: starts the webpack dev server in development mode `localhost:8080/` for analyst-core-ui
* `start:prod`: starts the webpack dev server in production mode `localhost:8080/` for analyst-core-ui
* `start`: starts the webpack dev server in production mode `localhost:8080/` for analyst-core-ui
* `producer`: the mock producer. See below for sub-commands.
* `docs`: generates the package source documentation for all sub packages
* `docs-interactive-analysis`: generates the package source documentation for interactive analysis
* `"docs-ui-app`: generates the package source documentation for ui-app
* `"docs-ui-electron`: generates the package source documentation for ui-electron
* `docs-ui-core-components`: generates the package source documentation for ui-core-components
* `docs-weavess`: generates the package source documentation for weavess
* `sonar`: runs sonar lint checks across all sub packages
* `test`: runs lerna test to run the package jest tests for all sub packages
* `test-jest`: runs the package jest tests
* `version`: returns the version of the package

## Mock Producer

### Environment config for Mac

You will need Java 11 for the producer. To install it on a mac, you can run

```bash
brew tap AdoptOpenJDK/openjdk
brew cask install adoptopenjdk11
```

You will also need to alias localhost to kafka. Add this line to your `/etc/hosts` file:

```bash
127.0.0.1   kafka
```

You will need the $GMS_COMMON_DIR set to contain the absolute path to your `gms-common` directory. This
should probably be set in a permanent location, like your `.bashrc` file.

### Producer

There is a yarn script, `yarn producer` which can be used to build, run, and manage the producer. It has many options,
which can be explored using yarn producer help, or by running help on any of the following commands:

* `yarn producer boot` The all-in-one command. Removes existing docker containers, creates the docker containers with Kafka and Zookeeper and etcd, creates a Kafka topic, and runs the producer.
* `yarn producer build` Builds the producer image and starts the docker containers.
* `yarn producer clean` Stop, remove, and prune docker containers, images and volumes.
* `yarn producer run` Runs the mock producer (must run build, first).
* `yarn producer info` print info about the docker containers, images, and volumes that may be of interest
* `yarn producer stop` Stop the containers for the producer.

## Deployment

This directory contains a `Dockerfile` and can be built as such, e.g. `docker build -t gms/analyst-ui .`

## Development

After installing dependencies, see the README in any sub-project under [./packages](packages) for instructions on developing in that particular project

## Packages

[ui-app](./packages/ui-app)

[ui-electron](./packages/ui-electron)

[ui-core-components](./packages/ui-core-components)

[weavess](./packages/weavess)
