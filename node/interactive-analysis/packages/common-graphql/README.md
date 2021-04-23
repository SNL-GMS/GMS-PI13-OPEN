## Common Graphql

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
  * `start:dev`: starts development mode `localhost:8080/`
  * `start:prod`: starts in production mode `localhost:8080/`
  * `dev`: starts in development mode `localhost:8080/`
  * `start`: starts in production mode `localhost:8080/`
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

## Building

```bash
[.../common-util] $ yarn build
```

## Generating Binaries

```bash
[.../common-util] $ yarn build
```