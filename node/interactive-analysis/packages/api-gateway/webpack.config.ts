import includes from 'lodash/includes';
import { resolve } from 'path';
import { Configuration, DefinePlugin } from 'webpack';
import {
  Configuration as DevServerConfiguration
} from 'webpack-dev-server';
import merge from 'webpack-merge';
import nodeExternals from 'webpack-node-externals';
import { WebpackConfig } from './webpack-config/types';
import { webpackNodeConfig } from './webpack-config/webpack.config';
import { getWebpackPaths } from './webpack-config/webpack.paths';

const config = (env?: { [key: string]: any }): Configuration[] | DevServerConfiguration[] => {
  const webpackPaths = getWebpackPaths(resolve(__dirname, '.'));

  const webpackConfig: WebpackConfig = {
    name: 'api-gateway',
    title: 'Interactive Analysis Api Gateway',
    paths: webpackPaths,
    isProduction: env && env.production,
    entry: {
      'api-gateway':  resolve(webpackPaths.src, 'ts/server/api-gateway-server.ts'),
    },
    alias: {},
    circularDependencyCheckFailOnError: true
  };

  const sohWebpackConfig: WebpackConfig = {
    ...webpackConfig,
    name: 'api-soh-gateway',
    entry: {
      'api-soh-gateway':  resolve(webpackPaths.src, 'ts/server/api-soh-gateway-server.ts'),
    },
   };

  /* define the external libraries -> not to be bundled */

  // bundling the following libraries cause critical warnings and `config` fails to load correctly
  // Webpack - Critical dependency: the request of a dependency is an expression
  // When a library uses variables or expressions in a require call, Webpack cannot
  // resolve them statically and imports the entire package.
  const externals = [
    'express',
    'config',
    'winston',
    'winston-daily-rotate-file'
  ];

  const commonConfig: Configuration | any = {
    externals : [
      nodeExternals({
        // the white list will be included in the library bundle
        whitelist: [
          name => {
            if (includes(externals, name)) {
              return false;
            }
            return true;
        }] as any
      }),
      'bufferutil',
      'utf-8-validate'
    ],
    plugins: [
      // https://github.com/lorenwest/node-config/wiki/Webpack-Usage
      // tslint:disable-next-line: no-require-imports
      new DefinePlugin({ CONFIG: JSON.stringify(require('config')) }) as any,
    ]
  };

  return env && env.soh ?
    [
      merge(
        webpackNodeConfig(sohWebpackConfig),
        commonConfig,
        {
          plugins: [
            new DefinePlugin({
              'process.env.INTERACTIVE_ANALYSIS_MODE': JSON.stringify('SOH')
            }) as any,
          ]
        }
      )
    ] :
    [
      merge(
        webpackNodeConfig(webpackConfig),
        commonConfig,
        {
          plugins: [
            new DefinePlugin({
              'process.env.INTERACTIVE_ANALYSIS_MODE': JSON.stringify('ANALYST')
            }) as any,
          ]
        }
      )
    ];
};

// tslint:disable-next-line: no-default-export
export default config;
