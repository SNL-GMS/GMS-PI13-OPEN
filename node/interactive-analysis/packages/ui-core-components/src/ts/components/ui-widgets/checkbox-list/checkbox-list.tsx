import { Checkbox } from '@blueprintjs/core';
import cloneDeep from 'lodash/cloneDeep';
import React from 'react';
import { CheckboxListProps } from './types';

interface CheckboxListState {
  enumToCheckedMap: Map<any, boolean>;
}

export const NULL_CHECKBOX_COLOR_SWATCH = 'NULL_CHECKBOX_COLOR_SWATCH';

export class CheckboxList extends React.Component<CheckboxListProps, CheckboxListState> {
  public constructor(props: CheckboxListProps) {
    super(props);
    this.state = {
      enumToCheckedMap: props.enumToCheckedMap
    };
  }

  public render() {
    return (
      <div className="checkbox-list__body">
        {Object.keys(this.props.checkboxEnum).map(key => (
          <div className="checkbox-list__row" key={key}>
            <div className="checkbox-list__box-and-label">
              <Checkbox
                className={'checkbox-list__checkbox'}
                data-cy={`checkbox-list-${
                  isNaN(key as any) ? String(key) : this.props.checkboxEnum[key]
                }`}
                checked={this.state.enumToCheckedMap.get(this.props.checkboxEnum[key])}
                onChange={() => this.onChange(key)}
              >
                <div className="checkbox-list__label">
                  {this.props.enumKeysToDisplayStrings
                    ? this.props.enumKeysToDisplayStrings.get(key)
                    : this.props.checkboxEnum[key]}
                </div>
                {this.props.enumToColorMap ? (
                  <div
                    className={`checkbox-list__legend-box${
                      this.shouldRenderNoColorSwatch(key) ? ' null-color-swatch' : ''
                    }`}
                    style={{
                      backgroundColor: !this.shouldRenderNoColorSwatch(key)
                        ? this.props.enumToColorMap.get(key)
                        : undefined
                    }}
                  />
                ) : null}
              </Checkbox>
            </div>
          </div>
        ))}
      </div>
    );
  }

  private readonly onChange = (key: string) => {
    const mapCopy = cloneDeep(this.state.enumToCheckedMap);
    mapCopy.set(
      this.props.checkboxEnum[key],
      !this.state.enumToCheckedMap.get(this.props.checkboxEnum[key])
    );
    this.setState({
      enumToCheckedMap: mapCopy
    });
    this.props.onChange(mapCopy);
  }

  private readonly shouldRenderNoColorSwatch = (key: string): boolean => {
    if (this.props.enumToColorMap) {
      return this.props.enumToColorMap.get(key) === NULL_CHECKBOX_COLOR_SWATCH;
    }
    return false;
  }
}
