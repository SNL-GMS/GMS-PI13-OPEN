import TerserPlugin from 'terser-webpack-plugin';
import { Configuration, DefinePlugin, LoaderOptionsPlugin } from 'webpack';
import merge from 'webpack-merge';

/**
 * Returns the webpack production configuration.
 *
 * @param paths the paths
 */
const productionConfig = (splitChunks: boolean): Configuration | any => {
  const splitChunksConfig: Configuration | any = splitChunks
    ? {
        optimization: {
          splitChunks: {
            cacheGroups: {
              styles: {
                name: 'styles',
                test: /\.css$/,
                chunks: 'all',
                maxSize: 249856,
                reuseExistingChunk: true,
                enforce: true
              },
              // vendor chunk
              vendor: {
                test: /[\\/]node_modules[\\/](?!cesium)/,
                name: 'vendor',
                chunks: 'all',
                maxSize: 249856,
                reuseExistingChunk: true,
                enforce: true
              },
              // cesium chunk
              cesium: {
                test: /[\\/]node_modules[\\/]cesium[\\/]/,
                name: 'cesium',
                chunks: 'all',
                maxSize: 249856,
                reuseExistingChunk: true,
                enforce: true
              },
              // common chunk
              common: {
                name: 'common',
                minChunks: 2,
                chunks: 'all',
                maxSize: 249856,
                reuseExistingChunk: true,
                enforce: true
              }
            }
          }
        }
      }
    : {};

  return merge(
    {
      mode: 'production',
      performance: {
        hints: 'warning',
        maxAssetSize: 10000000,
        maxEntrypointSize: 10000000
      },
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
      optimization: {
        runtimeChunk: false,
        usedExports: true,
        mergeDuplicateChunks: true,
        minimize: true,
        removeEmptyChunks: true,
        removeAvailableModules: true,
        sideEffects: true,
        minimizer: [
          new TerserPlugin({
            cache: true,
            exclude: [/\.min\.js$/gi], // skip pre-minified libs
            extractComments: true,
            parallel: true,
            sourceMap: false,
            terserOptions: {
              compress: false,
              mangle: false,
              output: {
                comments: false
              },
              sourceMap: false
            }
          })
        ]
      },
      plugins: [
        new DefinePlugin({
          'process.env.NODE_ENV': JSON.stringify('production')
        }) as any,

        // Some loaders accept configuration through webpack internals
        new LoaderOptionsPlugin({
          debug: false,
          minimize: true
        }) as any
      ]
    },
    splitChunksConfig
  );
};

/**
 * Returns the 'web' webpack production configuration.
 *
 * @param paths the paths
 */
export const webProductionConfig = (): Configuration | any => ({
  ...productionConfig(true)
});

/**
 * Returns the 'node' webpack production configuration.
 *
 * @param paths the paths
 */
export const nodeProductionConfig = (): Configuration | any => ({
  ...productionConfig(false)
});
