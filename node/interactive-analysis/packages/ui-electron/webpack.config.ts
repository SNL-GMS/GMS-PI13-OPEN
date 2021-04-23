import HtmlWebpackPlugin from 'html-webpack-plugin';
import { resolve } from 'path';
import { Configuration, DefinePlugin } from 'webpack';
import { Configuration as DevServerConfiguration } from 'webpack-dev-server';
import merge from 'webpack-merge';
import { WebpackConfig } from './webpack-config/types';
import { webpackNodeConfig } from './webpack-config/webpack.config';
import { getWebpackPaths } from './webpack-config/webpack.paths';

const SERVER_URL = process.env.SERVER_URL || 'http://localhost:8080';

const config = (env?: { [key: string]: any }): Configuration[] | DevServerConfiguration[] => {
  const webpackPaths = getWebpackPaths(resolve(__dirname, '.'));
  const webpackConfig: WebpackConfig = {
    name: 'ui-electron',
    title: 'Interactive Analysis',
    paths: webpackPaths,
    isProduction: env && env.production,
    entry: resolve(webpackPaths.src, 'ts/index.ts'),
    alias: {},
    circularDependencyCheckFailOnError: true
  };

  return [
    merge(webpackNodeConfig(webpackConfig), {
      target: 'electron-main',
      externals: ['utf-8-validate', 'bufferutil'],
      plugins: [
        new HtmlWebpackPlugin({
          template: resolve(webpackConfig.paths.baseDir, 'index.html'),
          title: `${webpackConfig.title}`,
          favicon: resolve(webpackConfig.paths.baseDir, 'webpack-config/resources/gms-logo.ico')
        }),

        new DefinePlugin({
          DEFAULT_SERVER_URL: JSON.stringify(SERVER_URL)
        }) as any
      ]
    })
  ];
};

// tslint:disable-next-line: no-default-export
export default config;
