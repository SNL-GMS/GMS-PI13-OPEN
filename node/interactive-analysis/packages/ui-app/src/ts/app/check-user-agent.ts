import { UILogger } from '@gms/ui-apollo';

/**
 * Checks the user agent.
 */
export const checkUserAgent = () => {
  if (navigator) {
    // log user client info to the gateway
    UILogger.Instance().info(`client connected ${navigator.userAgent.toLowerCase()}`);
  }

  if (
    !window.navigator.userAgent.includes('Chrome') &&
    !window.navigator.userAgent.includes('Firefox')
  ) {
    window.alert(`GMS Interactive Analysis currently supports
            Google Chrome > v59 and Firefox > v. You will likely experience degraded performance`);
  }
};
