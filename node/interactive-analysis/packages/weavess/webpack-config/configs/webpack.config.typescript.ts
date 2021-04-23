import ForkTsCheckerWebpackPlugin from 'fork-ts-checker-webpack-plugin';
import { TsconfigPathsPlugin } from 'tsconfig-paths-webpack-plugin';
import { Configuration, Plugin, RuleSetRule } from 'webpack';
import { WebpackPaths } from '../types';

/**
 * Typescript loader rule set.
 *
 * @param paths the paths
 */
const tsLoader = (nodeModules: string): RuleSetRule => ({
  test: /\.ts(x?)$/,
  exclude: [nodeModules, /.*node_modules.*/],
  use: {
    loader: 'ts-loader',
    options: {
      transpileOnly: true
    }
  }
});

/**
 * Mjs loader rule set.
 *
 * @param paths the paths
 */
const mjsLoader = (): RuleSetRule => ({
  test: /\.mjs$/,
  type: 'javascript/auto'
});

/**
 * Typescript plugins.
 *
 * @param tsconfig the path to the tsconfig file
 * @param tslint the path to the tslint file
 * @param src the path to the src file
 */
const tsPlugins = (tsconfig: string, tslint: string, src: string): Plugin[] | any[] => [
  new ForkTsCheckerWebpackPlugin({
    tslint,
    tsconfig,
    watch: src,
    checkSyntacticErrors: true,
    tslintAutoFix: true,
    memoryLimit: 2048,
    workers: ForkTsCheckerWebpackPlugin.ONE_CPU,
    useTypescriptIncrementalApi: true,
    measureCompilationTime: false,
    async: true
  })
];

/**
 * The webpack typescript configuration.
 *
 * @param paths the paths
 * @param isProduction true if production, false otherwise
 */
export const tsConfig = (paths: WebpackPaths, isProduction: boolean): Configuration | any => ({
  module: {
    rules: [tsLoader(paths.nodeModules), mjsLoader()]
  },
  plugins: [...tsPlugins(paths.tsconfig, paths.tslint, paths.src)],
  resolve: {
    extensions: ['.mjs', '.js', '.ts', '.tsx'],
    plugins: [
      new TsconfigPathsPlugin({
        baseUrl: paths.baseDir,
        configFile: paths.tsconfig
      })
    ]
  }
});
