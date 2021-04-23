import { IS_NODE_ENV_DEVELOPMENT } from '@gms/common-util';

/**
 * Configures the React Performance Dev Tool.
 * ONLY ENABLED FOR NODE_ENV === 'development'
 */
export const configureReactPerformanceDevTool = () => {
  // REACT Performance DEV Tool, enabled only in develop
  if (IS_NODE_ENV_DEVELOPMENT) {
    // tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
    const { registerObserver } = require('react-perf-devtool');
    const options = {};

    const callback = () => {
      /* no-op */
    };

    // assign the observer to the global scope, as the GC will delete it otherwise
    if (window) {
      (window as any).observer = registerObserver(options, callback);
    }
  }
};
