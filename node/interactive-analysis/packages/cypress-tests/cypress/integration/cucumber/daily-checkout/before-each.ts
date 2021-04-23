beforeEach(() => {
  cy.log('This will run before every scenario of daily-check.feature test');
  /*  cy.log('Test gateway healthy');
  const url = getURLForGateway(Cypress.config().baseUrl);
  cy.visit(`${url}/health-check`);
  cy.wait(SHORT_WAIT_TIME_MS);

  cy.log('Test gateway ready');
  cy.visit(`${url}/ready`);
  cy.wait(SHORT_WAIT_TIME_MS);

  cy.log('Test gateway healthy');
  cy.visit(`${url}/health-check`);
  cy.wait(SHORT_WAIT_TIME_MS);*/
});
