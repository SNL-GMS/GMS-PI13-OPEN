import { Checkbox } from '@blueprintjs/core';
import { DropDown } from '@gms/ui-core-components';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import isEqual from 'lodash/isEqual';
import React from 'react';
import { MagnitudeCategory, systemConfig } from '~analyst-ui/config/system-config';

export interface MagnitudeConfigurationProps {
  displayedMagnitudeTypes: AnalystWorkspaceTypes.DisplayedMagnitudeTypes;
  setCategoryAndTypes(types: AnalystWorkspaceTypes.DisplayedMagnitudeTypes): void;
}
export interface MagnitudeConfigurationState {
  displayedMagnitudeTypes: AnalystWorkspaceTypes.DisplayedMagnitudeTypes;
}

export class MagnitudeConfiguration extends React.Component<
  MagnitudeConfigurationProps,
  MagnitudeConfigurationState
> {
  /**
   * constructor
   */
  public constructor(props: MagnitudeConfigurationProps) {
    super(props);
    this.state = {
      displayedMagnitudeTypes: props.displayedMagnitudeTypes
    };
  }

  public render() {
    return (
      <div className="magnitude-configuration-popover">
        <div className="magnitude-configuration-popover__dropdown">
          <DropDown
            dropDownItems={MagnitudeCategory}
            value={
              this.isCustom()
                ? ''
                : this.isBodyWave()
                ? MagnitudeCategory.BODY
                : this.isSurfaceWave()
                ? MagnitudeCategory.SURFACE
                : ''
            }
            custom={this.isCustom()}
            onMaybeValue={val => {
              this.setState(
                {
                  displayedMagnitudeTypes: systemConfig.displayedMagnitudesForCategory.get(val)
                },
                this.callback
              );
            }}
          />
          <div className="magnitude-configuration-popover__label">Customize Magnitude Types:</div>
          <div className="magnitude-configuration-checkboxes">
            {this.state.displayedMagnitudeTypes.toArray().map((entry, index) => (
              <Checkbox
                key={index}
                label={entry[0]}
                checked={entry[1]}
                onClick={e => {
                  const displayed = this.state.displayedMagnitudeTypes.set(entry[0], !entry[1]);
                  this.setState(
                    {
                      displayedMagnitudeTypes: displayed
                    },
                    this.callback
                  );
                }}
              />
            ))}
          </div>
        </div>
      </div>
    );
  }

  private readonly callback = () =>
    this.props.setCategoryAndTypes(this.state.displayedMagnitudeTypes)

  /**
   * Returns true if the selected state is a custom configuration,
   * does not match the configuration for body wave or surface wave.
   */
  private readonly isCustom = (): boolean => !(this.isBodyWave() || this.isSurfaceWave());

  /**
   * Returns true if the selected state matches the state for body waves.
   */
  private readonly isBodyWave = (): boolean =>
    isEqual(
      this.state.displayedMagnitudeTypes,
      systemConfig.displayedMagnitudesForCategory.get(MagnitudeCategory.BODY)
    )

  /**
   * Returns true if the selected state matches the state for surface waves.
   */
  private readonly isSurfaceWave = (): boolean =>
    isEqual(
      this.state.displayedMagnitudeTypes,
      systemConfig.displayedMagnitudesForCategory.get(MagnitudeCategory.SURFACE)
    )
}
