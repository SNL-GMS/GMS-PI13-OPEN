import { Button, Classes, Intent, Tooltip } from '@blueprintjs/core';
import { createDropdownItems } from '@gms/ui-core-components';
import classNames from 'classnames';
import React from 'react';
import { gmsColors } from '~scss-config/color-preferences';
import { StationInformationDirtyDotPopup } from '../station-information-dirty-dot-popup';
import {
  AutomaticProcessingStatus,
  DataAcquisitionStatus,
  InteractiveProcessingStatus
} from '../types';

// TODO: may remove this cell renderer pending decision on column
export class ConfigureCellRenderer extends React.Component<any, {}> {
  public constructor(props) {
    super(props);
  }

  public render() {
    return (
      <Button
        className={classNames(Classes.SMALL, 'list__button')}
        text="Configure..."
        intent={Intent.PRIMARY}
        onClick={e => alert('Station configuration WIP')}
      />
    );
  }
}

/**
 * Renders the modified dot.
 */
// tslint:disable-next-line:max-classes-per-file
export class StationInformationModifiedDot extends React.Component<any, {}> {
  public constructor(props) {
    super(props);
  }

  /**
   * react component lifecycle
   */
  public render() {
    return (
      <Tooltip
        content={<StationInformationDirtyDotPopup stationInformation={this.props.data} />}
        className="dirty-dot-wrapper"
      >
        <div
          style={{
            backgroundColor: this.props.data.modified ? gmsColors.gmsMain : 'transparent'
          }}
          className="list-entry-dirty-dot"
        />
      </Tooltip>
    );
  }
}
export class DataAcquisitionDropdown extends React.PureComponent<any, {}> {
  public constructor(props) {
    super(props);
  }
  public render() {
    return (
      <select
        className={'list__drop-down'}
        value={this.props.data.dataAcquisition}
        onChange={(event: React.FormEvent<HTMLSelectElement>) => {
          const targetValue = event.currentTarget.value as DataAcquisitionStatus;
          this.props.context.updateDataAcquisition(this.props.data.id, targetValue);
        }}
      >
        {createDropdownItems(DataAcquisitionStatus)}
      </select>
    );
  }
}

export class InteractiveProcessingDropdown extends React.PureComponent<any, {}> {
  public constructor(props) {
    super(props);
  }
  public render() {
    return (
      <select
        className={'list__drop-down'}
        value={this.props.data.interactiveProcessing}
        onChange={(event: React.FormEvent<HTMLSelectElement>) => {
          const targetValue = event.currentTarget.value as InteractiveProcessingStatus;
          this.props.context.updateInteractiveProcessing(this.props.data.id, targetValue);
        }}
      >
        {createDropdownItems(InteractiveProcessingStatus)}
      </select>
    );
  }
}

export class AutomaticProcessingDropdown extends React.PureComponent<any, {}> {
  public constructor(props) {
    super(props);
  }
  public render() {
    return (
      <select
        className={'list__drop-down'}
        value={this.props.data.automaticProcessing}
        onChange={(event: React.FormEvent<HTMLSelectElement>) => {
          const targetValue = event.currentTarget.value as AutomaticProcessingStatus;
          this.props.context.updateAutomaticProcessing(this.props.data.id, targetValue);
        }}
      >
        {createDropdownItems(AutomaticProcessingStatus)}
      </select>
    );
  }
}
