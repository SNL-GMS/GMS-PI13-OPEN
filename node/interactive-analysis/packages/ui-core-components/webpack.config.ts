import { resolve } from 'path';
import { Configuration } from 'webpack';
import {
  Configuration as DevServerConfiguration
} from 'webpack-dev-server';
import { WebpackConfig } from './webpack-config/types';
import { webpackAppConfig, webpackLibCjsConfig } from './webpack-config/webpack.config';
import { getWebpackPaths } from './webpack-config/webpack.paths';

const config = (env?: { [key: string]: any }): Configuration[] | DevServerConfiguration[] => {
  const webpackPaths = getWebpackPaths(resolve(__dirname, '.'));
  const webpackConfig: WebpackConfig = {
    name: 'ui-core-components',
    title: 'UI Core Components',
    paths: webpackPaths,
    isProduction: env && env.production,
    entry:
      env && env.devserver
        ? resolve(webpackPaths.src, 'ts/examples/index.tsx')
        : resolve(webpackPaths.src, 'ts/ui-core-components.ts'),
    alias: {},
    circularDependencyCheckFailOnError: true
  };

  return env && env.devserver
    ? [webpackAppConfig(webpackConfig)]
    : [webpackLibCjsConfig(webpackConfig)];
};

// tslint:disable-next-line: no-default-export
export default config;
