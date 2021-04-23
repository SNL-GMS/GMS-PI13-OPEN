import { Given, Then } from 'cypress-cucumber-preprocessor/steps';
import * as Common from '../../../../fixtures/common';
import * as SystemMessagesActions from '../../../../fixtures/common-ui/system-message';
import { selectors } from '../../../../fixtures/query-selectors';

/**
 * Opens the System Message Display
 */
Given('the UI is opened to the System Messages Display', () => {
  Common.visitApp();
  Common.openDisplay(Common.CommonDisplays.SYSTEM_MESSAGES);
});

Then('a system message exists', () => {
  cy.get(selectors.systemMessageRow, { timeout: 40000 }).should(Common.cypressShouldOptions.EXIST);
});

Then('auto scroll can be turned off and New Message button appears', () => {
  SystemMessagesActions.pressScrolling();
});

Then(
  'the New Message button can be clicked, scrolling to the bottom and enabling auto scroll',
  () => {
    SystemMessagesActions.pressNewMessagesButton();
  }
);

Then(
  'table previous button can be pressed, disabling auto scroll and New Message button appears',
  () => {
    SystemMessagesActions.pressPreviousPageButton();
  }
);

/**
 * Opens up the collapsed toolbar, and selects the clear button
 * !With data being random, no matter what there exists an edge case for data to break the test
 * !With the current flow of system message producer pushing out so many messages
 * !The least likely edge case once a clear happens is there to be more messages
 * !Once we have more control over the system message producer can change test from a less equal to an equals
 */
Then('system messages can be cleared', () => {
  SystemMessagesActions.pressClearSystemMessages();
});
