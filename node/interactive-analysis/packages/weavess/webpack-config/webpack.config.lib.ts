import { Configuration } from 'webpack';
import merge from 'webpack-merge';
import nodeExternals from 'webpack-node-externals';
import { bundleAnalyzeConfig } from './configs/webpack.config.analyze';
import { styleConfig } from './configs/webpack.config.styles';
import { tsConfig } from './configs/webpack.config.typescript';
import { urlConfig } from './configs/webpack.config.url';
import { WebpackConfig } from './types';
import { commonWebConfig, getEntry } from './webpack.config.common';
import { developmentConfig } from './webpack.config.development';
import { webProductionConfig } from './webpack.config.production';

/**
 * The webpack common configuration for libraries
 *
 * @param webpackConfig the webpack configuration
 */
const libCommonConfig = (webpackConfig: WebpackConfig): Configuration | any => {
  const common: Configuration | any = merge(commonWebConfig(webpackConfig), {
    externals: [
      nodeExternals({
        // the white list will be included in the library bundle
        whitelist: []
      }),
      nodeExternals({
        modulesFromFile: true,
        // the white list will be included in the library bundle
        whitelist: []
      }),
      // support for `yarn workspaces`
      nodeExternals({
        modulesDir: '../../node_modules',
        // the white list will be included in the library bundle
        whitelist: []
      })
    ]
  });
  return merge(
    common,
    tsConfig(webpackConfig.paths, webpackConfig.isProduction),
    styleConfig(webpackConfig.isProduction),
    urlConfig(webpackConfig.isProduction),
    webpackConfig.isProduction ? webProductionConfig() : developmentConfig(),
    webpackConfig.isProduction && process.env.BUNDLE_ANALYZE === 'true'
      ? bundleAnalyzeConfig(webpackConfig.paths.bundleAnalyze, webpackConfig.name)
      : {}
  );
};

/**
 * The webpack configuration for libraries (umd)
 *
 * @param webpackConfig the webpack configuration
 */
export const libUmdConfig = (webpackConfig: WebpackConfig): Configuration | any => {
  const libUmd: Configuration | any = {
    entry: getEntry(webpackConfig),
    output: {
      filename: '[name].js',
      chunkFilename: '[name].[contenthash:8].js',
      path: webpackConfig.paths.dist,
      sourcePrefix: '',
      libraryTarget: 'umd',
      library: webpackConfig.name
    }
  };
  return merge(libCommonConfig(webpackConfig), libUmd);
};

/**
 * The webpack configuration for libraries (commonJS)
 *
 * @param webpackConfig the webpack configuration
 */
export const libCjsConfig = (webpackConfig: WebpackConfig): Configuration | any => {
  const libCjs: Configuration | any = {
    entry: getEntry(webpackConfig),
    output: {
      filename: '[name].js',
      chunkFilename: '[name].[contenthash:8].js',
      path: webpackConfig.paths.dist,
      sourcePrefix: '',
      libraryTarget: 'commonjs2',
      library: webpackConfig.name
    }
  };
  return merge(libCommonConfig(webpackConfig), libCjs);
};
