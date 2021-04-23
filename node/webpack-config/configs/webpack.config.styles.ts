import MiniCssExtractPlugin from 'mini-css-extract-plugin';
import OptimizeCSSAssetsPlugin from 'optimize-css-assets-webpack-plugin';
import { Configuration, Plugin, RuleSetRule } from 'webpack';

/**
 * CSS webpack loader rule set.
 *
 * @param isProduction true if production; false if development
 */
const cssLoader = (isProduction: boolean): RuleSetRule => ({
  test: /\.css$/,
  use: [
    isProduction
      ? {
          loader: MiniCssExtractPlugin.loader.toString(),
          options: {
            hmr: !isProduction,
            sourceMap: !isProduction
          }
        }
      : 'style-loader',
    {
      loader: 'css-loader',
      options: {
        // TODO causes warnings with current version: https://github.com/zeit/next-plugins/issues/392
        // minimize: isProduction,
        sourceMap: !isProduction
      }
    },
    {
      loader: 'resolve-url-loader'
    }
  ]
});

/**
 * SCSS/SASS webpack loader rule set.
 *
 * @param isProduction true if production; false if development
 */
const scssLoader = (isProduction: boolean): RuleSetRule => ({
  test: /\.s[ac]ss$/,
  use: [
    isProduction
      ? {
          loader: MiniCssExtractPlugin.loader.toString(),
          options: {
            hmr: !isProduction,
            sourceMap: !isProduction
          }
        }
      : {
          loader: 'style-loader'
        },
    {
      loader: 'css-loader',
      options: {
        sourceMap: !isProduction
      }
    },
    {
      loader: 'resolve-url-loader'
    },
    {
      loader: 'sass-loader',
      options: {
        sourceMap: !isProduction
      }
    }
  ]
});

/**
 * Style plugins.
 *
 * @param isProduction true if production; false if development
 */
const stylePlugins = (isProduction: boolean): Plugin[] | any[] =>
  isProduction
    ? [
        new MiniCssExtractPlugin({
          filename: '[name].css',
          chunkFilename: !isProduction ? '[name].css' : '[name].[contenthash:8].css'
        }),
        new OptimizeCSSAssetsPlugin()
      ]
    : [];

/**
 * The webpack styles configuration.
 *
 * @param isProduction true if production; false if development
 */
export const styleConfig = (isProduction: boolean): Configuration | any => ({
  module: {
    rules: [cssLoader(isProduction), scssLoader(isProduction)]
  },
  plugins: stylePlugins(isProduction),
  resolve: {
    extensions: ['.css', '.scss', '.sass']
  }
});
