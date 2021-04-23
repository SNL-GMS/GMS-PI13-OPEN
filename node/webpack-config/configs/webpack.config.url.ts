import { Configuration, RuleSetRule } from 'webpack';

/**
 * URL loader rule set.
 */
const urlLoader: RuleSetRule = {
  loader: 'url-loader',
  options: {
    limit: 100000,
    name: 'resources/[name].[ext]'
  },
  test: /\.(png|gif|jpg|jpeg|svg|xml|woff|woff2|eot|ttf)$/i
};

/**
 * The webpack url configuration.
 */
export const urlConfig = (isProduction: boolean): Configuration | any => ({
  module: {
    rules: [urlLoader]
  }
});
