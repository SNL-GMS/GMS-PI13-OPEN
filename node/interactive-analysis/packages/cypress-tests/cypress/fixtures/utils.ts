import { SHORT_WAIT_TIME_MS } from './common';

/**
 * Takes a url for the UI and gets the proper url for the gateway health checks
 * @param url url to transform
 *
 * @returns url for gateway health checks
 */
export const getURLForGateway = (url: string): string => {
  // localhost:8080 needs to become localhost:3000
  if (url.includes('localhost')) {
    return 'localhost:3000';
  }
  // ui server url needs to become gateway server url
  const firstPeriod = url.indexOf('.');
  if (!firstPeriod) {
    cy.log('WARNING: Invalid URL');
    return '';
  }
  const baseURL = url.substring(firstPeriod + 1);
  return `http://graphql.interactive-analysis-api-gateway.${baseURL}`;
};

export const testEndpoint = (endpoint: string, testMessage?: string) => {
  if (testMessage) {
    cy.log('Test ui ready');
  }
  cy.visit('/ready');
  cy.wait(SHORT_WAIT_TIME_MS);
};
