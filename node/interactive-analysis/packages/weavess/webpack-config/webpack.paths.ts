import { resolve } from 'path';
import { WebpackPaths } from './types';

/**
 * Returns the Webpack build paths for the provided `baseDir` directory.
 */
export const getWebpackPaths = (baseDir: string, useWorkspace: boolean = false): WebpackPaths => {
  const nodeModules = resolve(baseDir, 'node_modules/');
  const tslint = resolve(baseDir, 'tslint.json');
  const tsconfig = resolve(baseDir, 'tsconfig.json');
  const packageJson = resolve(baseDir, 'package.json');
  const cesium = useWorkspace
    ? // path to the workspace node_modules (support for hoisted packages)
      resolve(baseDir, '../../node_modules/cesium/Source/')
    : resolve(nodeModules, 'cesium/Source/');
  const src = resolve(baseDir, 'src');
  const resources = resolve(src, 'resources');
  const dist = resolve(baseDir, 'dist');
  const bundleAnalyze = resolve(baseDir, 'bundle-analyzer');

  return {
    baseDir,
    nodeModules,
    tslint,
    tsconfig,
    packageJson,
    cesium,
    src,
    resources,
    dist,
    bundleAnalyze,

    resolveModule: (module: string) => resolve(nodeModules, module),
    resolveResource: (resource: string) => resolve(resources, resource)
  };
};
