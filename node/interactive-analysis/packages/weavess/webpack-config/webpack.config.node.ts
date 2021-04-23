import { Configuration } from 'webpack';
import merge from 'webpack-merge';
import { bundleAnalyzeConfig } from './configs/webpack.config.analyze';
import { tsConfig } from './configs/webpack.config.typescript';
import { WebpackConfig } from './types';
import { commonNodeConfig, getEntry } from './webpack.config.common';
import { developmentConfig } from './webpack.config.development';
import { nodeProductionConfig } from './webpack.config.production';

/**
 * The webpack common configuration for node applications
 *
 * @param webpackConfig the webpack configuration
 */
const nodeCommonConfig = (webpackConfig: WebpackConfig): Configuration | any => {
  const entry = getEntry(webpackConfig);

  const common: Configuration | any = merge(commonNodeConfig(webpackConfig));

  return merge(common, {
    entry,
    output: {
      filename: '[name].js',
      chunkFilename: '[name].[contenthash:8].js',
      path: webpackConfig.paths.dist,
      // needed to compile multiline strings in Cesium
      sourcePrefix: ''
    }
  });
};

/**
 * The webpack configuration for node applications
 *
 * @param webpackConfig the webpack configuration
 */
export const nodeConfig = (webpackConfig: WebpackConfig): Configuration | any =>
  merge(
    tsConfig(webpackConfig.paths, webpackConfig.isProduction),
    nodeCommonConfig(webpackConfig),
    webpackConfig.isProduction ? nodeProductionConfig() : developmentConfig(),
    webpackConfig.isProduction && process.env.BUNDLE_ANALYZE === 'true'
      ? bundleAnalyzeConfig(webpackConfig.paths.bundleAnalyze, webpackConfig.name)
      : {}
  );
