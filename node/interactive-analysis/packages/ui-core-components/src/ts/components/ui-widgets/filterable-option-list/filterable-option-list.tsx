import React from 'react';
import { FilterableOptionListProps, FilterableOptionListState } from './types';

// Timeout to prevent double click handlers from firing in other parts of UI
export class FilterableOptionList extends React.Component<
  FilterableOptionListProps,
  FilterableOptionListState
> {
  /** Internal reference to list of options */
  private optionRefs: HTMLDivElement[] = [];
  /** Internal reference to search input */
  private searchRef: HTMLInputElement | null;
  private constructor(props) {
    super(props);
    this.state = {
      currentlySelected: this.props.defaultSelection ? this.props.defaultSelection : '',
      currentFilter: this.props.defaultFilter ? this.props.defaultFilter : ''
    };
  }

  // TODO remove the use of this function: deprecated and unsafe with REACT
  public UNSAFE_componentWillUpdate() {
    this.optionRefs = [];
  }

  public componentDidUpdate(
    prevProps: FilterableOptionListProps,
    prevState: FilterableOptionListState
  ) {
    if (prevProps.disabled && !this.props.disabled) {
      if (this.searchRef) {
        this.searchRef.focus();
      }
    }
    // Focus on an option if the user changed selection via the arrow keys,
    // but NOT if the selection was made by filtering the lsit
    if (
      prevState.currentlySelected !== this.state.currentlySelected &&
      prevState.currentFilter === this.state.currentFilter
    ) {
      const option = this.optionRefs.find(
        ref => ref.getAttribute('id') === this.state.currentlySelected
      );
      if (option) {
        option.focus();
      }
    }
  }

  /**
   * React component lifecycle.
   */
  public render() {
    const prioritySorted = this.props.prioriotyOptions
      ? [...this.props.prioriotyOptions].sort((a, b) =>
          a.localeCompare(b, 'en', { sensitivity: 'base' })
        )
      : [];
    const filteredPriority = prioritySorted.filter(opt =>
      opt.toLowerCase().includes(this.state.currentFilter.toLowerCase())
    );
    const sortedOptions = [...this.props.options].sort((a, b) =>
      a.localeCompare(b, 'en', { sensitivity: 'base' })
    );
    const filteredOptions = sortedOptions.filter(
      opt =>
        opt.toLowerCase().includes(this.state.currentFilter.toLowerCase()) &&
        !(prioritySorted.indexOf(opt) > -1)
    );

    const widths = this.props.widthPx ? `${this.props.widthPx}px` : '';
    const altStyle = {
      width: widths,
      marginLeft: '0px'
    };
    return (
      <div className="filterable-option-list">
        <input
          className={
            this.props.disabled
              ? 'filterable-option-list__search filterable-option-list__search--disabled'
              : 'filterable-option-list__search'
          }
          ref={ref => (this.searchRef = ref)}
          type="search"
          disabled={this.props.disabled}
          placeholder="Search input"
          tabIndex={0}
          onChange={e => {
            this.onFilterInput(e);
          }}
          autoFocus={true}
          onKeyDown={this.onKeyPress}
          value={this.state.currentFilter}
          style={this.props.widthPx ? altStyle : undefined}
        />
        <div
          className="fitlerable-option-list__list"
          style={this.props.widthPx ? altStyle : undefined}
        >
          {filteredPriority.length > 0
            ? filteredPriority.map(pOpt => this.renderOption(pOpt))
            : null}
          {filteredPriority.length > 0 ? <div className="fitlerable-option-list__divider" /> : null}
          {filteredOptions.map(opt => this.renderOption(opt))}
        </div>
      </div>
    );
  }

  private readonly renderOption = (label: string): JSX.Element => {
    let className =
      this.state.currentlySelected === label
        ? 'filterable-option-list-item__selected filterable-option-list-item'
        : 'filterable-option-list-item';
    if (this.props.disabled) {
      className = className + ' filterable-option-list-item--disabled';
    }
    return (
      <div
        className={className}
        data-cy={`filterable-option-${label}`}
        onClick={
          this.props.disabled
            ? undefined
            : e => {
                e.preventDefault();
                e.stopPropagation();
                this.selectOption(label, false);
              }
        }
        id={label}
        key={label}
        tabIndex={0}
        onKeyDown={this.onKeyPress}
        ref={ref => {
          if (ref) {
            this.optionRefs.push(ref);
          }
        }}
        onDoubleClick={
          this.props.disabled
            ? undefined
            : e => {
                e.preventDefault();
                e.stopPropagation();
                this.selectOption(label, true);
              }
        }
      >
        {label}
      </div>
    );
  }

  private readonly onArrowUpDown = (downArrow: boolean) => {
    // Re-create the filtered lists of options
    const prioritySorted = this.props.prioriotyOptions
      ? [...this.props.prioriotyOptions].sort((a, b) =>
          a.localeCompare(b, 'en', { sensitivity: 'base' })
        )
      : [];
    const filteredPriority = prioritySorted.filter(opt =>
      opt.toLowerCase().includes(this.state.currentFilter.toLowerCase())
    );
    const sortedOptions = [...this.props.options].sort((a, b) =>
      a.localeCompare(b, 'en', { sensitivity: 'base' })
    );
    const filteredOptions = sortedOptions.filter(
      opt =>
        opt.toLowerCase().includes(this.state.currentFilter.toLowerCase()) &&
        !(prioritySorted.indexOf(opt) > -1)
    );
    const selectedInPriority = filteredPriority.indexOf(this.state.currentlySelected) > -1;
    const selectedInList = filteredOptions.indexOf(this.state.currentlySelected) > -1;
    const indexOfSelected = selectedInPriority
      ? filteredPriority.indexOf(this.state.currentlySelected)
      : selectedInList
      ? filteredOptions.indexOf(this.state.currentlySelected)
      : -1;
    if (downArrow) {
      this.moveSelectionDown(
        selectedInPriority,
        selectedInList,
        indexOfSelected,
        filteredPriority,
        filteredOptions
      );
    } else {
      this.moveSelectionUp(selectedInPriority, indexOfSelected, filteredPriority, filteredOptions);
    }
  }

  private readonly moveSelectionDown = (
    isSelectedInPriorityList: boolean,
    isSelectedInOtherList: boolean,
    indexOfSelected: number,
    filteredPriority: string[],
    filteredOptions: string[]
  ) => {
    // tries to navigate down in priority list
    if (isSelectedInPriorityList) {
      const provisionalIndex = indexOfSelected + 1;
      if (provisionalIndex < filteredPriority.length) {
        // Navigates down in priority list
        this.setSelectionToEntry(filteredPriority[provisionalIndex]);
      } else {
        this.setPositionToStartOfList(filteredOptions, []);
      }
      // tries to navigate down in regular list
    } else if (isSelectedInOtherList) {
      const provisionalIndex = indexOfSelected + 1;
      if (provisionalIndex < filteredOptions.length) {
        // Navigates down in regular list
        this.setSelectionToEntry(filteredOptions[provisionalIndex]);
      } else {
        this.setPositionToStartOfList(filteredOptions, filteredPriority);
      }
      // starts selection from top of first available list
    } else {
      this.setPositionToStartOfList(filteredOptions, filteredPriority);
    }
  }

  private readonly moveSelectionUp = (
    isSelectedInPriorityList: boolean,
    indexOfSelected: number,
    filteredPriority: string[],
    filteredOptions: string[]
  ) => {
    // Tries to navigate up in priority list
    if (isSelectedInPriorityList) {
      const provisionalIndex = indexOfSelected - 1;
      if (provisionalIndex >= 0) {
        this.setSelectionToEntry(filteredPriority[provisionalIndex]);
      } else {
        this.setPositionToEndOfList(filteredOptions, filteredPriority);
      }
      // tries to navigate up in regular list
    } else {
      const provisionalIndex = indexOfSelected - 1;
      if (provisionalIndex >= 0) {
        this.setSelectionToEntry(filteredOptions[provisionalIndex]);
      } else {
        this.setPositionToEndOfPriorityList(filteredPriority, filteredOptions);
      }
    }
  }

  private readonly setPositionToStartOfList = (
    filteredOptions: string[],
    filteredPriority: string[]
  ) => {
    if (filteredPriority.length > 0) {
      this.setPositionToStartOfPriorityList(filteredPriority);
    } else {
      this.setPositionToStartOfOtherList(filteredOptions);
    }
  }

  private readonly setPositionToEndOfList = (
    filteredOptions: string[],
    filteredPriority: string[]
  ) => {
    if (filteredOptions.length > 0) {
      this.setPositionToEndOfOtherList(filteredOptions);
    } else {
      this.setPositionToEndOfPriorityList(filteredPriority, filteredOptions);
    }
  }
  private readonly setPositionToStartOfPriorityList = (filteredPriority: string[]) => {
    this.setSelectionToEntry(filteredPriority[0]);
  }

  private readonly setPositionToStartOfOtherList = (filteredOptions: string[]) => {
    this.setSelectionToEntry(filteredOptions[0]);
  }

  private readonly setPositionToEndOfPriorityList = (
    filteredPriority: string[],
    filteredOptions: string[]
  ) => {
    if (filteredPriority.length >= 1) {
      this.setSelectionToEntry(filteredPriority[filteredPriority.length - 1]);
    } else {
      this.setSelectionToEntry(filteredOptions[filteredOptions.length - 1]);
    }
  }

  private readonly setPositionToEndOfOtherList = (filteredOptions: string[]) => {
    this.setSelectionToEntry(filteredOptions[filteredOptions.length - 1]);
  }

  private readonly setSelectionToEntry = (entry: string) => {
    this.setState({ currentlySelected: entry });
    this.props.onSelection(entry);
  }

  private readonly onKeyPress = (e: React.KeyboardEvent) => {
    switch (e.key) {
      // down arrow
      case 'ArrowDown':
        e.preventDefault();
        this.onArrowUpDown(true);
        break;
      // up arrow
      case 'ArrowUp':
        e.preventDefault();
        this.onArrowUpDown(false);
        break;
      // enter/return
      case 'Enter':
        e.preventDefault();
        if (this.props.onEnter) {
          this.props.onEnter(this.state.currentlySelected);
        }
        break;
      default:
        return;
    }
  }

  private readonly onFilterInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    // if the currently selected option would be excluded from the search
    // set currently selected to first visible option
    let selected = this.state.currentlySelected;
    if (!this.state.currentlySelected.includes(e.currentTarget.value)) {
      selected = this.getFirstVisibleOption(
        this.props.options,
        this.props.prioriotyOptions ? this.props.prioriotyOptions : [],
        e.currentTarget.value
      );
    }
    this.setState({ currentFilter: e.currentTarget.value, currentlySelected: selected });
  }

  private readonly getFirstVisibleOption = (
    options: string[],
    prioriotyOptions: string[],
    filter: string
  ) => {
    const sortedPriority = prioriotyOptions.filter(opt =>
      opt.toLowerCase().includes(filter.toLowerCase())
    );
    sortedPriority.sort((a, b) => a.localeCompare(b, 'en', { sensitivity: 'base' }));

    const sortedRegular = options.filter(opt => opt.toLowerCase().includes(filter.toLowerCase()));
    sortedRegular.sort((a, b) => a.localeCompare(b, 'en', { sensitivity: 'base' }));

    if (sortedPriority.length > 0) {
      return sortedPriority[0];
    }

    if (sortedRegular.length > 0) {
      return sortedRegular[0];
    }

    return '';
  }

  private readonly selectOption = (value: string, isDoubleClick: boolean) => {
    this.setState({ currentlySelected: value });
    this.props.onSelection(value);
    if (this.props.onClick && !isDoubleClick) {
      this.props.onClick(value);
    } else if (this.props.onDoubleClick && isDoubleClick) {
      this.props.onDoubleClick(value);
    }
  }
}
