## Analyst Core Electron
Electron code for window management/layout persistence 

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
  * `start:dev`: starts electron development mode `localhost:8080/`
  * `start:prod`: starts electron in production mode `localhost:8080/`
  * `dev`: starts electron in development mode `localhost:8080/`
  * `start`: starts electron in production mode `localhost:8080/`
  * `watch:dev`: runs the development build of the package and watches for changes to recompile
  * `watch:prod`: runs the production build of the package and watches for changes to recompile
  * `watch`: runs the production build and watches for changes to recompile
  * `docs`: generates the package source documentation
  * `sonar`: runs sonar lint checks
  * `test`: runs the package jest tests
  * `version`: returns the version of the package

## Documentation

To generate HTML documentation files:
```
$ yarn docs
```

## Running

By default, this will start an electron application and connect to `http://localhost:8080`, unless a `SERVER_URL` environment variable is specified

```bash
[.../ui-electron] $ yarn start
```

or, to connect to a different url

```bash
[.../ui-electron] $ SERVER_URL=http://otherdomain.com:8080 yarn start
```

## Generating Binaries

Generate binaries (on mac os, wine will need to be installed first. This can be done using `brew install wine`)

Set the `SERVER_URL` environment variable to set the default backend that the electron app will attempt to connect to. Otherwise, the url will be set to a development default (localhost)

```bash
[.../ui-electron] $ SERVER_URL=http://otherdomain.com:8080 yarn generate-bin
```

Binaries for darwin (mac os) and windows (win32) will be generated under `dist/`
