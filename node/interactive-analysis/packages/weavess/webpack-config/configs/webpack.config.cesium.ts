import CopyWebpackPlugin from 'copy-webpack-plugin';
import { join, resolve } from 'path';
import { Configuration, DefinePlugin, Plugin, RuleSetRule } from 'webpack';
import { WebpackPaths } from '../types';

// Explanations:
// https://cesiumjs.org/tutorials/cesium-and-webpack/

/**
 * Cesium loader rule set.
 */
const cesiumLoader: RuleSetRule = {
  test: /cesium\.js$/,
  loader: 'script-loader'
};

/**
 * KML loader rule set.
 */
const kmlLoader: RuleSetRule = {
  test: /\.(kml)$/,
  use: {
    loader: 'file-loader'
  }
};

/**
 * Cesium webpack plugins.
 *
 *
 * @param paths the paths
 * @param isProduction true if production; false if development
 */
const cesiumPlugins = (paths: WebpackPaths, isProduction: boolean): Plugin[] | any[] => {
  const scriptBuild = isProduction ? 'Cesium' : 'CesiumUnminified';
  return [
    new CopyWebpackPlugin([
      {
        from: join(paths.cesium, `../Build/${scriptBuild}/`),
        to: resolve(paths.dist, paths.dist, 'cesium')
      }
    ]),
    new DefinePlugin({
      CESIUM_BASE_URL: JSON.stringify(resolve(paths.dist, 'cesium')),
      'process.env': {
        CESIUM_OFFLINE: JSON.stringify(process.env.CESIUM_OFFLINE)
      }
    })
  ];
};

/**
 * The webpack cesium configuration.
 *
 * @param paths the paths
 */
export const cesiumConfig = (paths: WebpackPaths, isProduction: boolean): Configuration | any => ({
  amd: {
    // enable webpack-friendly use of require in Cesium
    toUrlUndefined: true
  },
  node: {
    // Resolve node module use of fs
    fs: 'empty'
  },
  module: {
    rules: [cesiumLoader, kmlLoader]
  },
  resolve: {
    alias: {
      cesium: paths.cesium
    },
    extensions: ['.js']
  },
  plugins: cesiumPlugins(paths, isProduction)
});
