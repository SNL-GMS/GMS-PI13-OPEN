import { PercentBar } from '@gms/ui-core-components';
import * as React from 'react';
import { dataAcquisitionUserPreferences } from '~components/data-acquisition-ui/config';
import { ChannelCellRendererParams, EnvironmentalSoh, EnvironmentTableDataContext } from '../types';
import { ChannelCellBaseRenderer } from './channel-cell-base-renderer';
import { EnvironmentCellValue } from './environment-cell-value';
import { MaybeQuietIndicator } from './maybe-quiet-indicator';

const diameterPx = Math.round(dataAcquisitionUserPreferences.tableRowHeightPx / 3);

/**
 * Renders a `div` around the channel cell which is used to style the cell to indicate that it is selected or not.
 * @param isSelected true if the cell should be marked as selected; false otherwise
 */
const SelectedCellRenderer: React.FunctionComponent<{
  environmentSoh: EnvironmentalSoh;
}> = React.memo(props => {
  // determine if the cell is selected
  const isSelected: boolean = props.environmentSoh?.isSelected;
  return <div className={isSelected ? 'is-selected' : ''}>{props.children}</div>;
});

/**
 * Renders an environment table cell either with data or unknown if no data exists
 */
export const ChannelCellRenderer: React.FunctionComponent<ChannelCellRendererParams> = React.memo(
  props => (
    <EnvironmentTableDataContext.Consumer>
      {context => {
        const data = context.data.find(d => d.id === props.data.id);
        const environmentSoh = data?.valueAndStatusByChannelName.get(props.colDef.colId);
        return (
          <div data-cy="channel-cell">
            <SelectedCellRenderer environmentSoh={environmentSoh}>
              <ChannelCellBaseRenderer environmentSoh={environmentSoh}>
                <MaybeQuietIndicator data={environmentSoh} diameterPx={diameterPx} />
                <PercentBar percentage={environmentSoh?.value} />
                <EnvironmentCellValue
                  value={environmentSoh?.value}
                  hasUnacknowledgedChanges={environmentSoh?.hasUnacknowledgedChanges}
                />
              </ChannelCellBaseRenderer>
            </SelectedCellRenderer>
          </div>
        );
      }}
    </EnvironmentTableDataContext.Consumer>
  )
);
