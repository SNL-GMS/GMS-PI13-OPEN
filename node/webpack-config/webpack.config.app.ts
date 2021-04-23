import HtmlWebpackPlugin from 'html-webpack-plugin';
import { resolve } from 'path';
import { Configuration } from 'webpack';
import merge from 'webpack-merge';
import { bundleAnalyzeConfig } from './configs/webpack.config.analyze';
import { devServerConfig } from './configs/webpack.config.devserver';
import { sourceMapConfig } from './configs/webpack.config.sourcemap';
import { styleConfig } from './configs/webpack.config.styles';
import { tsConfig } from './configs/webpack.config.typescript';
import { urlConfig } from './configs/webpack.config.url';
import { WebpackConfig } from './types';
import { commonWebConfig, getEntry } from './webpack.config.common';
import { developmentConfig } from './webpack.config.development';
import { webProductionConfig } from './webpack.config.production';

/**
 * The webpack common configuration for applications
 *
 * @param webpackConfig the webpack configuration
 */
const appCommonConfig = (webpackConfig: WebpackConfig): Configuration | any => {
  const entry = getEntry(webpackConfig);

  return merge(commonWebConfig(webpackConfig), {
    entry,
    output: {
      filename: '[name].js',
      chunkFilename: '[name].[contenthash:8].js',
      path: webpackConfig.paths.dist,
      // needed to compile multiline strings in Cesium
      sourcePrefix: ''
    },
    plugins: [
      new HtmlWebpackPlugin({
        filename: 'index.html',
        template: resolve(webpackConfig.paths.baseDir, 'index.html'),
        title: `${webpackConfig.title}`,
        favicon: resolve(webpackConfig.paths.baseDir, 'webpack-config/resources/gms-logo.ico'),
        ...webpackConfig?.htmlWebpackPluginOptions
      })
    ]
  });
};

/**
 * The webpack configuration for applications
 *
 * @param webpackConfig the webpack configuration
 */
export const appConfig = (webpackConfig: WebpackConfig): Configuration | any =>
  merge(
    tsConfig(webpackConfig.paths, webpackConfig.isProduction),
    styleConfig(webpackConfig.isProduction),
    urlConfig(webpackConfig.isProduction),
    sourceMapConfig(webpackConfig.isProduction),
    appCommonConfig(webpackConfig),
    devServerConfig(webpackConfig.isProduction),
    webpackConfig.isProduction ? webProductionConfig() : developmentConfig(),
    webpackConfig.isProduction && process.env.BUNDLE_ANALYZE === 'true'
      ? bundleAnalyzeConfig(webpackConfig.paths.bundleAnalyze, webpackConfig.name)
      : {}
  );
