import { Configuration, DefinePlugin } from 'webpack';

/**
 * Returns the webpack development configuration.
 *
 * @param paths the paths
 */
export const developmentConfig = (): Configuration | any => ({
  mode: 'development',
  devtool: 'inline-cheap-module-source-map',
  stats: {
    colors: false,
    hash: false,
    timings: true,
    assets: false,
    chunks: false,
    chunkModules: false,
    modules: false,
    children: false
  },
  plugins: [
    new DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('development')
    })
  ]
});
