/**
 * The Webpack build paths.
 */
export interface WebpackPaths {
  /** The base directory for the project path */
  baseDir: string;

  /** The node modules path */
  nodeModules: string;

  /** The tslint path */
  tslint: string;

  /** The tsconfig file path */
  tsconfig: string;

  /** The project package JSON path */
  packageJson: string;

  /** The project dist path */
  dist: string;

  /** The project source path */
  src: string;

  /** The project resources */
  resources: string;

  /** The path to cesium */
  cesium: string;

  /** The output path for the bundle analyzer */
  bundleAnalyze: string;

  /**
   * Resolves the path directory for the given module.
   *
   * @param nodeModules the node modules directory
   * @param module the module to resolve
   */
  resolveModule(module: string): string;

  /**
   * Resolves the path directory for the given resource.
   *
   * @param resources the resources directory
   * @param resource the resource to resolve
   */
  resolveResource(resource: string): string;
}

/**
 * The Webpack configuration.
 */
export interface WebpackConfig {
  /** The name of the module/project */
  name: string;

  /** */
  title: string;

  /** The webpack project paths */
  paths: WebpackPaths;

  /**
   * Flag indicating if the build is for production or development
   * true if production; false if development
   */
  isProduction: boolean;

  /** The webpack project entry */
  entry: string | { [key: string]: string };

  /** The webpack project aliases */
  alias: any;

  // additional html webpack plugin options
  htmlWebpackPluginOptions?: any;

  /**
   * True to fail with on error if a circular dependency is discovered, false
   * to allow circular dependencies to pass but issue warnings.
   *
   * This should always be set to true to prevent circular dependencies
   */
  circularDependencyCheckFailOnError: boolean;
}
