// CSS class selectors, should not be exported and added here
// !WARNING: CSS class selectors should be AVOIDED and NOT USED WHEN POSSIBLE
const button = '.bp3-button';
const barChart = '.chart-bar';
const contextMenuItem = '.bp3-menu-item';
const contextMenuItemDismiss = '.bp3-menu-item.bp3-popover-dismiss';
const drilldownDisplayTitle = '.display-title';
const hiddenIndicator = '.hidden-indicator';
const nonIdealState = '.bp3-non-ideal-state';
const quietIndicatorPieSlice = '.quiet-indicator__pie-slice';
const tableRow = '.ag-row';
const tableRowId = 'row-id';
const tableRowSelected = '.ag-row-selected';
const tableHeaderCellLabel = '.ag-header-cell-label';
const toolbarCollapsed = '.toolbar-overflow-menu-button';
const toolbarCollapsedTrashIcon = '.bp3-icon-trash';
const tableViewport = '.ag-body-viewport';
const tablePaging = '.ag-paging-button';

export const selectors = {
  acknowledge: {
    soh: '[data-cy="soh-acknowledged"]',
    withComment: '[data-cy="acknowledge-with-comment"]',
    withoutComment: '[data-cy="acknowledge-without-comment"]',
    cancel: '[data-cy="cancel-acknowledge"]',
    commentTextArea: '[data-cy="acknowledge-comment-textarea"]',
    submit: '[data-cy="submit-acknowledge"]'
  },
  barChart,
  button,
  channelCell: '[data-cy="channel-cell"]',
  checkbox: {
    all_1: '[data-cy="checkbox-list-All_1"]',
    bad: '[data-cy="checkbox-list-BAD"]',
    good: '[data-cy="checkbox-list-GOOD"]',
    marginal: '[data-cy="checkbox-list-MARGINAL"]'
  },
  contextMenuItem,
  contextMenuItemDismiss,
  stationStatisticsCell: `[data-cy="table-cell"]`,
  stationStatisticsIsSelected: tableRowSelected,
  stationStatisticsTitleCell: `[data-cy="soh-cell"].soh-cell__title`,
  stationStatisticsRow: tableRow,
  stationStatisticsTable: '.station-statistics-table__wrapper',
  displayBackground: '[data-cy="station-statistics-table__wrapper"]',
  displayTitleBar: {
    container: '.lm_header',
    minimizeButton: '[title="minimize"]',
    maximizeButton: '[title="maximize"]'
  },
  drilldownDisplayTitle,
  goodAcknowledgedTable: '[data-cy="soh-acknowledged"]',
  groupDropdown: '[data-cy="station-statistics-group-selector"]',
  groupName: 'CD1.1',
  hiddenIndicator,
  needsAttentionTable: '[data-cy="soh-unacknowledged"]',
  nonIdealState,
  newMessagesButton: '[data-cy="new-messages-button"]',
  overviewContainer: '.soh-overview-cell__container',
  overviewIsSelected: `.soh-overview-cell--selected`,
  quieting: {
    commentTextarea: '[data-cy="quiet-comment-textarea"]',
    cancel: '[data-cy="cancel-quiet"]',
    submit: '[data-cy="submit-quiet"]'
  },
  quietIndicatorPieSlice,
  tableCell: `[data-cy="table-cell"]`,
  sohFilter: '[data-cy="filter-soh"]',
  sohFilterByStationGroup: '[data-cy="filter-by-station-group-soh"]',
  sohNameCell: '[data-cy="soh-name-cell"]',
  sohOverviewCell: '[data-cy="soh-overview-cell"]',
  sohOverviewHeader: '[data-cy="soh-overview-header"]',
  sohOverviewToolbar: '[data-cy="soh-overview-toolbar"]',
  sohToolbar: '.soh-toolbar-container',
  systemMessageAutoScroll: '[data-cy="system-message-auto-scroll"]',
  systemMessageDisplay: '[data-cy="system-message-table"]',
  systemMessageDisplayClear: '[data-cy="system-message-clear"]',
  systemMessageRow: `[data-cy="system-message-table"] ${tableRow}`,
  systemMessageCell: `[data-cy="system-message-table"] [data-cy="table-cell"]`,
  systemMessageTableViewport: `[data-cy="system-message-table"] ${tableViewport}`,
  stationColumnHeader: tableHeaderCellLabel,
  tableContainer: '[data-cy="core-table__container]',
  tableRowId,
  tablePaging,
  toolbarCollapsed,
  toolbarCollapsedTrashIcon,
  unacknowledged: '[data-cy="soh-unacknowledged"]',
  workspace: '[data-cy="workspace"]'
};
