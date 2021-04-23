// tslint:disable-next-line: match-default-export-name
import CopyWebpackPlugin from 'copy-webpack-plugin';
import { join, resolve } from 'path';
import { Configuration, DefinePlugin } from 'webpack';
import { Configuration as DevServerConfiguration } from 'webpack-dev-server';
// tslint:disable-next-line: match-default-export-name
import merge from 'webpack-merge';
import { cesiumConfig } from './webpack-config/configs/webpack.config.cesium';
import { WebpackConfig } from './webpack-config/types';
import { webpackAppConfig } from './webpack-config/webpack.config';
import { getWebpackPaths } from './webpack-config/webpack.paths';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const fs = require('fs');

/**
 * Queries the published 'sound' directory and returns a list of the
 * available sound files.
 *
 * Note: this assumes that there are only files (no additional directories)
 */
const getAllFilesFromFolder = (dir: string, pattern?: RegExp): string[] => {
  const results: string[] = [];
  fs.readdirSync(dir)
    .filter((file: string) => !pattern || file.match(pattern))
    .forEach((file: string) => {
      results.push(file);
    });
  return results;
};

const config = (env?: { [key: string]: any }): Configuration[] | DevServerConfiguration[] => {
  const secure = env.ssl === 'true' || env.ssl === true;
  const protocol = secure ? 'https://' : 'http://';
  const webSocketProtocol = secure ? 'wss://' : 'ws://';

  const graphqlProxyUri = process.env.GRAPHQL_PROXY_URI || `${protocol}localhost:3000`;
  const waveformsProxyUri = process.env.WAVEFORMS_PROXY_URI || `${protocol}localhost:3000`;
  const subscriptionsProxyUri =
    process.env.SUBSCRIPTIONS_PROXY_URI || `${webSocketProtocol}localhost:4000`;
  const authProxyUri = graphqlProxyUri;

  const webpackPaths = getWebpackPaths(
    resolve(__dirname, '.'),
    true /* indicate that workspaces are being used */
  );

  const analystWebpackConfig: WebpackConfig = {
    name: 'ui-app',
    title: 'Interactive Analysis',
    paths: webpackPaths,
    isProduction: env && env.production,
    entry: {
      'ui-app': resolve(webpackPaths.src, 'ts/app/ui-app/index.tsx')
    },
    htmlWebpackPluginOptions: {
      cesiumScript: '<script src="./cesium/Cesium.js"></script>'
    },
    alias: {},
    circularDependencyCheckFailOnError: true
  };

  const sohWebpackConfig: WebpackConfig = {
    ...analystWebpackConfig,
    name: 'ui-soh-app',
    title: 'GMS SOH Monitoring',
    paths: webpackPaths,
    entry: {
      'ui-soh-app': resolve(webpackPaths.src, 'ts/app/ui-soh-app/index.tsx')
    }
  };

  const availableSoundFiles = getAllFilesFromFolder(
    join(webpackPaths.baseDir, 'sounds'),
    /.mp3$/
  ).join(';');

  // tslint:disable-next-line: no-console
  console.log(`Configured available sound files: ${availableSoundFiles}`);

  const commonConfig: Configuration | any = merge(
    {
      externals: {
        electron: 'electron'
      },
      plugins: [
        new CopyWebpackPlugin([
          {
            from: join(webpackPaths.baseDir, 'sounds'),
            to: resolve(webpackPaths.dist, 'sounds')
          }
        ]),
        new DefinePlugin({
          'process.env.AVAILABLE_SOUND_FILES': JSON.stringify(availableSoundFiles)
        }) as any
      ]
    },
    cesiumConfig(analystWebpackConfig.paths, env && env.production)
  );

  const analystCommonConfig: Configuration | any = merge(commonConfig, {
    plugins: [
      new DefinePlugin({
        'process.env.INTERACTIVE_ANALYSIS_MODE': JSON.stringify('ANALYST')
      }) as any
    ]
  });

  const sohCommonConfig: Configuration | any = merge(commonConfig, {
    plugins: [
      new DefinePlugin({
        'process.env.INTERACTIVE_ANALYSIS_MODE': JSON.stringify('SOH')
      }) as any
    ]
  });

  const devServerConfig: Configuration | any = {
    devServer: {
      https: secure,
      proxy: {
        '/alive': {
          target: graphqlProxyUri,
          // !WARNING: A backend server running on HTTPS with an invalid certificate
          // !will not be accepted by default - must set to false to accept
          secure: false,
          changeOrigin: true,
          logLevel: 'warn'
        },
        '/ready': {
          target: graphqlProxyUri,
          // !WARNING: A backend server running on HTTPS with an invalid certificate
          // !will not be accepted by default - must set to false to accept
          secure: false,
          changeOrigin: true,
          logLevel: 'warn'
        },
        '/health-check': {
          target: graphqlProxyUri,
          // !WARNING: A backend server running on HTTPS with an invalid certificate
          // !will not be accepted by default - must set to false to accept
          secure: false,
          changeOrigin: true,
          logLevel: 'warn'
        },
        '/graphql': {
          target: graphqlProxyUri,
          // !WARNING: A backend server running on HTTPS with an invalid certificate
          // !will not be accepted by default - must set to false to accept
          secure: false,
          changeOrigin: true,
          logLevel: 'warn'
        },
        '/waveforms': {
          target: waveformsProxyUri,
          // !WARNING: A backend server running on HTTPS with an invalid certificate
          // !will not be accepted by default - must set to false to accept
          secure: false,
          changeOrigin: true,
          logLevel: 'warn'
        },
        '/subscriptions': {
          target: subscriptionsProxyUri,
          ws: true,
          // !WARNING: A backend server running on HTTPS with an invalid certificate
          // !will not be accepted by default - must set to false to accept
          secure: false,
          changeOrigin: true,
          logLevel: 'warn'
        },
        '/auth': {
          target: authProxyUri,
          // !WARNING: A backend server running on HTTPS with an invalid certificate
          // !will not be accepted by default - must set to false to accept
          secure: false,
          changeOrigin: true,
          logLevel: 'warn'
        }
      }
    }
  };

  const wpConfig =
    env && env.soh
      ? [
          env && env.devserver
            ? merge(webpackAppConfig(sohWebpackConfig), sohCommonConfig, devServerConfig)
            : merge(webpackAppConfig(sohWebpackConfig), sohCommonConfig)
        ]
      : [
          env && env.devserver
            ? merge(webpackAppConfig(analystWebpackConfig), analystCommonConfig, devServerConfig)
            : merge(webpackAppConfig(analystWebpackConfig), analystCommonConfig)
        ];

  return wpConfig;
};

// tslint:disable-next-line: no-default-export
export default config;
