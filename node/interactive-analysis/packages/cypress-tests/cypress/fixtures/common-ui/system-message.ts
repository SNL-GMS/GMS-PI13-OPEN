/// <reference types="Cypress" />
import * as Common from '../common';
import { selectors } from '../query-selectors';

/**
 * Clicks the auto scroll button toggling on/off auto scroll
 */
const pressSystemMessageScroll = () =>
  cy
    .get(selectors.systemMessageAutoScroll)
    .first()
    .click({ force: true });

/**
 * Returns the length of all the system messages table rows
 */
const getCurrentSystemMessagesCount = () =>
  cy
    .get(selectors.systemMessageRow, {
      timeout: Common.DATA_REFRESH_TIME_WITH_EXTRA_TIME + Common.LONG_WAIT_TIME
    })
    .its('length')
    .then(messages => {
      // tslint:disable-next-line: no-console
      console.log(`number of system messages: ${String(messages)}`);
      return messages;
    });

/**
 * Clears all the system messages by clicking clear messages button
 */
const clearSystemMessages = () => {
  cy.get(selectors.systemMessageDisplayClear).click();
};

/**
 * Clicks the previous page button on the table
 */
const goToPreviousPage = () => {
  cy.get(selectors.tablePaging)
    .contains('Previous')
    .click();
};

/**
 * Verifies the system messages have been cleared
 */
const verifyMessagesCleared = (messages: number) => {
  cy.get(selectors.systemMessageRow, { timeout: Common.LONG_WAIT_TIME * 2 }).should(
    Common.cypressShouldOptions.HAVE_LENGTH_LESS_THAN,
    messages
  );
};

const verifyNewMessageButtonExists = () => {
  cy.get(selectors.newMessagesButton, {
    timeout: Common.DATA_REFRESH_TIME_WITH_EXTRA_TIME * 2
  }).should(Common.cypressShouldOptions.EXIST);
};

const verifyNewMessageButtonDoesNotExists = () => {
  cy.get(selectors.newMessagesButton).should(Common.cypressShouldOptions.NOT_EXIST);
};

const getTableViewport = () => cy.get(selectors.systemMessageTableViewport);

const verifyScrolledToBottom = () => {
  getTableViewport().then(viewport => {
    const element = viewport.get(0);
    const amountScrolled = element?.scrollTop ?? 0;
    const totalHeightOfScrollContainer = element?.scrollHeight || 0;
    const visibleHeightOfContainer = element?.clientHeight ?? 0;
    const result = totalHeightOfScrollContainer - amountScrolled - visibleHeightOfContainer;
    const userScrollPauseSensitivityPx = 60;
    cy.wrap(userScrollPauseSensitivityPx).should(
      Common.cypressShouldOptions.BE_GREATER_THAN,
      result
    );
  });
};

export const pressPreviousPageButton = () => {
  goToPreviousPage();
  verifyNewMessageButtonExists();
};

export const pressNewMessagesButton = () => {
  cy.get(selectors.newMessagesButton)
    .first()
    .click();
  verifyNewMessageButtonDoesNotExists();
  cy.wait(Common.LONG_WAIT_TIME);
  verifyScrolledToBottom();
};

export const pressScrolling = () => {
  pressSystemMessageScroll();
  verifyNewMessageButtonExists();
};

/**
 * Clears the system messages display by clicking clear messages button
 */
export const pressClearSystemMessages = () => {
  getCurrentSystemMessagesCount().then((messages: number) => {
    clearSystemMessages();
    verifyMessagesCleared(messages);
  });
};
