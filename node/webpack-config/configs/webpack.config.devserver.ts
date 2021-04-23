import { Configuration } from 'webpack';

/**
 * Returns the development server arguements `HOST:PORT`.
 */
const getDevServerArgs = () => {
  if (process.env.HOST != null && process.env.PORT != null) {
    return {
      host: process.env.HOST,
      port: +process.env.PORT
    };
  }
  return {
    host: 'localhost',
    port: 8080
  };
};

/**
 * Returns the webpack development server configuration.
 *
 * @param isProduction true if production; false if development
 * @param host the host of the development server
 * @param port the port of the development server
 */
export const devServerConfig = (
  isProduction: boolean,
  { host, port }: { host: string; port: number } = getDevServerArgs()
): Configuration | any => ({
  devServer: {
    stats: 'errors-only',
    host,
    port,
    compress: false,
    disableHostCheck: false,
    clientLogLevel: 'warn',
    open: false,
    overlay: {
      warnings: false,
      errors: true
    }
  }
});
