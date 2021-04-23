import { Checkbox } from '@blueprintjs/core';
import Immutable from 'immutable';
import * as React from 'react';
import { CheckboxListEntry, SimpleCheckboxListProps, SimpleCheckboxListState } from './types';

/**
 * Creates a list of checkboxes with a label and optional color
 */
export class SimpleCheckboxList extends React.Component<
  SimpleCheckboxListProps,
  SimpleCheckboxListState
> {
  public constructor(props: SimpleCheckboxListProps) {
    super(props);
    this.state = {
      checkboxEntriesMap: Immutable.Map<string, CheckboxListEntry>()
    };
  }

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * React lifecycle method that triggers on mount, populates the map state class variable
   */
  public componentDidMount() {
    let tempCheckboxEntriesMap = Immutable.Map<string, CheckboxListEntry>();
    this.props.checkBoxListEntries.forEach(entry => {
      tempCheckboxEntriesMap = tempCheckboxEntriesMap.set(entry.name, entry);
    });
    this.setState({ checkboxEntriesMap: tempCheckboxEntriesMap });
  }

  public render() {
    return (
      <div className="checkbox-list__body">
        {this.props.checkBoxListEntries.map(entry => (
          <React.Fragment key={entry.name}>
            <div className="checkbox-list__row">
              <div className="checkbox-list__box-and-label">
                <Checkbox
                  className={'checkbox-list__checkbox'}
                  data-cy={`checkbox-item-${entry.name}`}
                  onChange={() => this.updateCheckboxEntriesMap(entry.name)}
                  checked={
                    this.state.checkboxEntriesMap.has(entry.name)
                      ? this.state.checkboxEntriesMap.get(entry.name).isChecked
                      : entry.isChecked
                  }
                >
                  <div className="checkbox-list__label">{entry.name}</div>
                  {entry.color ? (
                    <div
                      className={`checkbox-list__legend-box`}
                      style={{
                        backgroundColor: entry.color
                      }}
                    />
                  ) : (
                    undefined
                  )}
                </Checkbox>
              </div>
            </div>
          </React.Fragment>
        ))}
      </div>
    );
  }
  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Updates the state and triggers a on change call back to the parent
   * @param entryName name of the text for the checkbox
   * @returns void
   */
  private readonly updateCheckboxEntriesMap = (entryName: string): void => {
    const entry = this.state.checkboxEntriesMap.get(entryName);
    entry.isChecked = !entry.isChecked;
    this.props.onChange(entryName);
    this.setState({ checkboxEntriesMap: this.state.checkboxEntriesMap.set(entryName, entry) });
  }
}
