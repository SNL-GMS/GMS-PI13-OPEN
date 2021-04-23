import { Configuration, RuleSetRule } from 'webpack';

/**
 * Source map loader rule set.
 *
 * @param paths the paths
 */
const sourceMapLoader = (): RuleSetRule => ({
  test: /\.js$/,
  use: ['source-map-loader'],
  enforce: 'pre',
  exclude: [
    // these packages have problems with their sourcemaps
    /node_modules\/@apollographql/,
    /node_modules\/apollo-client/,
    /node_modules\/apollo-cache-hermes/,
    /node_modules\/apollo-server-core/,
    /node_modules\/apollo-server-express/,
    /node_modules\/cesium/,
    /node_modules\/deprecated-decorator/,
    /node_modules\/subscriptions-transport-ws/,
    /node_modules\/graphql-tools/,
    /node_modules\/graphql-subscriptions/
  ]
});

/**
 * The webpack load source maps config.
 *
 * @param isProduction true if production, false otherwise
 */
export const sourceMapConfig = (isProduction: boolean): Configuration | any => ({
  module: {
    rules: isProduction
      ? []
      : // development load source maps
        [sourceMapLoader()]
  },
  resolve: {
    extensions: ['.js.map']
  }
});
