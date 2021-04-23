import { Configuration, RuleSetRule } from 'webpack';
import { WebpackPaths } from '../types';

/**
 * Plotly loader rule set.
 *
 * @param paths the paths
 */
const plotlyLoader = (paths: WebpackPaths): RuleSetRule => ({
  test: /\.js$/,
  include: [paths.resolveModule('plotly.js'), paths.resolveModule('mapbox-gl')],
  use: ['ify-loader', 'transform-loader?plotly.js/tasks/util/compress_attributes.js']
});

/**
 * The webpack plotly configuration.
 *
 * @param paths the paths
 */
export const plotlyConfig = (paths: WebpackPaths): Configuration | any => ({
  module: {
    rules: [plotlyLoader(paths)]
  },
  resolve: {
    extensions: ['.js']
  }
});
