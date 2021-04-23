import { Given, Then, When } from 'cypress-cucumber-preprocessor/steps';
import * as Common from '../../../../fixtures/common';
import { selectors } from '../../../../fixtures/query-selectors';
import * as AllDisplayActions from '../../../../fixtures/soh/soh-display-all';
import { getTitleOfFirstSelectedStationInOverview } from '../../../../fixtures/soh/soh-overview';
import { selectRows, verifyNumRowsSelected } from '../../../../fixtures/soh/table/table-selection';
/**
 * Opens the SOH Overview Display, Station Statistics Display, SOH Lag Display, SOH Missing Display
 */
Given(
  'The UI is opened to the SOH Overview Display, Station Statistics Display, SOH Lag Display, SOH Missing Display',
  () => {
    Common.visitApp(); // already opens 'SOH Overview', 'Station Statistics'
  }
);

/**
 * Opens the soh overview display and the station statistics display
 * (note we have to call cy.wait due to a timing bug)
 */
Given('The UI is opened to the SOH Overview Display and Station Statistics Display', () => {
  Common.visitApp(); // already opens 'SOH Overview', 'Station Statistics'
  cy.wait(Common.COMMON_WAIT_TIME_MS);
});

When('We select a station in the overview display', () => {
  cy.get(selectors.overviewContainer)
    .children()
    .first()
    .click();
});

When('We scroll to the bottom of the station statistics display', () => {
  cy.get(selectors.stationStatisticsTable)
    .get('.ag-body-viewport')
    .first()
    .scrollTo('bottom');
});

When('We select two stations in the overview display', () => {
  selectRows([0, 1], Common.ModifierKeys.SHIFT);
});

When('Station Statistics Display has three selected non adjacent rows', () => {
  Common.getOS().includes(Common.OSTypes.MAC)
    ? // tslint:disable-next-line: no-magic-numbers
      selectRows([2, 3, 6], Common.ModifierKeys.META)
    : // tslint:disable-next-line: no-magic-numbers
      selectRows([2, 3, 6], Common.ModifierKeys.CTRL);
});

Then('All displays have selected the same channel', () => {
  cy.wait(Common.COMMON_WAIT_TIME_MS);
  // get original selection from the overview display
  getTitleOfFirstSelectedStationInOverview().then((strOverview: any) =>
    AllDisplayActions.verifyAllDisplaysMatchSelection(strOverview)
  );
});

Then('Station Statistics Display has {int} selected rows', (num: number) => {
  cy.wait(Common.COMMON_WAIT_TIME_MS);
  verifyNumRowsSelected(num);
});
