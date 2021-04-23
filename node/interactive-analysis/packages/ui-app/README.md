# Analyst Core UI

Analysis display core codebase.

## Build Scripts
  * `precommit`: executed when files are staged for commit (git add) for prettify; this currently is not enabled,
  * `clean:node_modules`: removes the node_modules directory
  * `clean:build-scripts`: removes the (copied over) common build scripts directory
  * `clean:dist`: removes all of the build directories (build/, dist/, bundle-analyzer/)
  * `clean:coverage`: removes the coverage directory 
  * `clean:docs`: removes the documents directory
  * `clean`: cleans and removes all
  * `build:build-scripts`: copies over the common build scripts
  * `build:tslint`: runs tslint checks on the package
  * `build:tsc`: compiles the typescript project and generates the type definitions
  * `build:tsc-defs`: generates the type definitions
  * `build:dev`: runs the development build of the package, includes source maps
  * `build:prod`: runs the production build of the package, does not include source maps (preforms minification)
  * `build`: runs the production build of the package
  * `start:dev`: starts the webpack dev server in development mode `localhost:8080/`
  * `start:prod`: starts the webpack dev server in production mode `localhost:8080/`
  * `dev`: starts the webpack dev server in development mode `localhost:8080/`
  * `start`: starts the webpack dev server in production mode `localhost:8080/`
  * `watch:dev`: runs the development build of the package and watches for changes to recompile
  * `watch:prod`: runs the production build of the package and watches for changes to recompile
  * `watch`: runs the production build and watches for changes to recompile
  * `docs`: generates the package source documentation
  * `sonar`: runs sonar lint checks
  * `test`: runs the package jest tests
  * `version`: returns the version of the package

## Build

```
$ yarn build
```

## Usage

```
$ yarn start
```

## Development
To run the application (see: http://localhost:8080)
```
$ yarn dev
```

## Documentation

To generate HTML documentation files:
```
$ yarn docs
```

## Development

To build the node application

```bash
[.../ui-app] $ yarn build
```

To start a development server hosting the analysis UI

```bash
[.../ui-app] $ yarn start
```

Then, access `http://localhost:8080` in your browser, or run [../ui-electron](../ui-electron) to access the native version of the analysis UI

