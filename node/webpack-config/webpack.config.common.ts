import CaseSensitivePathsPlugin from 'case-sensitive-paths-webpack-plugin';
import CircularDependencyPlugin from 'circular-dependency-plugin';
import { Configuration, DefinePlugin, HashedModuleIdsPlugin, IgnorePlugin } from 'webpack';
import merge from 'webpack-merge';
import { WebpackConfig } from './types';

// tslint:disable-next-line
const GitRevisionPlugin = require('git-revision-webpack-plugin');
const gitRevisionPlugin = new GitRevisionPlugin();

/**
 * Returns the version number from the package.json file
 *
 * @param path the path to the package.json file
 */
export const getVersion = (path: string): string =>
  // tslint:disable-next-line: no-require-imports
  `${require(path).version}.${gitRevisionPlugin.version()}`;

/**
 * Returns the webpack entry
 *
 * @param webpackConfig the webpack configuration
 */
export const getEntry = (webpackConfig: WebpackConfig): { [id: string]: string } => {
  if (typeof webpackConfig.entry === 'string' || webpackConfig.entry instanceof String) {
    const entry = {};
    entry[`${webpackConfig.name}`] = webpackConfig.entry;
    return entry;
  }
  return webpackConfig.entry;
};

/**
 * The webpack common base common configuration
 *
 * @param webpackConfig the webpack configuration
 */
const commonConfig = (webpackConfig: WebpackConfig): Configuration | any => {
  const common: Configuration | any = {
    plugins: [
      new HashedModuleIdsPlugin(), // so that file hashes don't change unexpectedly

      new CaseSensitivePathsPlugin(),

      // Zero tolerance for circular dependencies
      new CircularDependencyPlugin({
        exclude: /.js|node_modules/,
        // !!! Recommended setting is to always have this set to true
        failOnError: webpackConfig.circularDependencyCheckFailOnError
      }),

      new DefinePlugin({
        // tslint:disable-next-line: max-line-length
        __VERSION__: `${JSON.stringify(
          getVersion(webpackConfig.paths.packageJson)
        )}.${JSON.stringify(gitRevisionPlugin.commithash())}`,
        'process.env.GIT_VERSION': JSON.stringify(gitRevisionPlugin.version()),
        'process.env.GIT_COMMITHASH': JSON.stringify(gitRevisionPlugin.commithash()),
        'process.env.GIT_BRANCH': JSON.stringify(gitRevisionPlugin.branch())
      }),

      new IgnorePlugin(/^\.\/locale$/, /moment$/),

      new IgnorePlugin(/^encoding$/, /node-fetch/)
    ],
    resolve: {
      alias: webpackConfig.alias,
      extensions: ['.json']
    }
  };
  return common;
};

/**
 * The webpack common base web configuration
 *
 * @param webpackConfig the webpack configuration
 */
export const commonWebConfig = (webpackConfig: WebpackConfig): Configuration | any => {
  const common: Configuration | any = merge(commonConfig(webpackConfig), {
    target: 'web',
    node: {
      // Resolve node module use of fs
      fs: 'empty'
    }
  });
  return common;
};

/**
 * The webpack common base node configuration
 *
 * @param webpackConfig the webpack configuration
 */
export const commonNodeConfig = (webpackConfig: WebpackConfig): Configuration | any => {
  const common: Configuration | any = merge(commonConfig(webpackConfig), {
    target: 'node'
  });
  return common;
};
