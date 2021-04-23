import { resolve } from 'path';
import { Configuration } from 'webpack';
import {
  Configuration as DevServerConfiguration
} from 'webpack-dev-server';
import { WebpackConfig } from './webpack-config/types';
import { webpackLibCjsConfig } from './webpack-config/webpack.config';
import { getWebpackPaths } from './webpack-config/webpack.paths';

const config = (env?: { [key: string]: any }): Configuration[] | DevServerConfiguration[] => {
  const webpackPaths = getWebpackPaths(resolve(__dirname, '.'));
  const webpackConfig: WebpackConfig = {
    name: 'ui-apollo',
    title: 'GMS UI Apollo Configuration',
    paths: webpackPaths,
    isProduction: env && env.production,
    entry: resolve(webpackPaths.src, 'ts/ui-apollo.ts'),
    alias: {},
    circularDependencyCheckFailOnError: true
  };

  return [webpackLibCjsConfig(webpackConfig)];
};

// tslint:disable-next-line: no-default-export
export default config;
