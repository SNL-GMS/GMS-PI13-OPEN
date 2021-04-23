import { Button, ContextMenu, Menu } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import isEqual from 'lodash/isEqual';
import uniqueId from 'lodash/uniqueId';
import React from 'react';
import { PopoverButton } from '../popover-button';
import { ToolbarItemRenderer } from './toolbar-item';
import { ToolbarMenuItemRenderer } from './toolbar-menu-item';
import * as ToolbarTypes from './types';

const WIDTH_OF_OVERFLOW_BUTTON_PX = 46;
const AMOUNT_OF_SPACE_TO_RESERVE_PX = 16;
export class ToolbarComponent extends React.Component<
  ToolbarTypes.ToolbarProps,
  ToolbarTypes.ToolbarState
> {
  private toolbarItemRefs: HTMLElement[] = [];
  private toolbarItemLeftRefs: HTMLElement[] = [];
  private overflowButtonRef: HTMLElement;
  private popoverButtonMap: Map<number, PopoverButton>;
  private toolbarRef: HTMLElement;
  private constructor(props) {
    super(props);
    this.props.items.forEach((item, index) => {
      this.props.items.forEach((itemB, indexB) => {
        if (index !== indexB && item.rank === itemB.rank) {
          console.warn('Toolbar Error: Item ranks must be unique - change item ranks to be unique');
        }
      });
    });
    this.state = {
      checkSizeOnNextDidMountOrDidUpdate: true,
      indicesToOverflow: [],
      leftIndicesToOverflow: [],
      whiteSpaceAllotmentPx: this.props.minWhiteSpacePx ? this.props.minWhiteSpacePx : 0
    };
    this.popoverButtonMap = new Map<number, PopoverButton>();
  }

  public componentDidMount() {
    if (this.state.checkSizeOnNextDidMountOrDidUpdate) {
      this.handleResize();
    }
  }
  public componentDidUpdate(prevProps: ToolbarTypes.ToolbarProps, prevState) {
    const haveLeftItemsChanged =
      prevProps.itemsLeft &&
      this.props.itemsLeft &&
      !isEqual(prevProps.itemsLeft, this.props.itemsLeft);
    const haveRightItemsChanged = !isEqual(prevProps.items, this.props.items);
    if (
      this.state.checkSizeOnNextDidMountOrDidUpdate ||
      prevProps.toolbarWidthPx !== this.props.toolbarWidthPx ||
      haveRightItemsChanged ||
      haveLeftItemsChanged
    ) {
      this.handleResize();
    }
  }

  /**
   * React component lifecycle.
   */
  public render() {
    const sortedItems = [...this.props.items].sort((a, b) => a.rank - b.rank);
    this.toolbarItemRefs = [];
    this.toolbarItemLeftRefs = [];
    this.popoverButtonMap = new Map<number, PopoverButton>();
    return (
      <div
        className="toolbar"
        ref={ref => {
          if (ref) {
            this.toolbarRef = ref;
          }
        }}
        style={{
          width: `${this.props.toolbarWidthPx}px`
        }}
      >
        <div className="toolbar__left-group">
          {this.props.itemsLeft
            ? this.props.itemsLeft.map((item, index) => {
                if (this.state.leftIndicesToOverflow.indexOf(index) < 0) {
                  return (
                    <div
                      key={item.rank}
                      className="toolbar-item toolbar-item__left"
                      ref={ref => {
                        if (ref) {
                          this.toolbarItemLeftRefs.push(ref);
                        }
                      }}
                    >
                      {this.renderItem(item)}
                    </div>
                  );
                }
                return undefined;
              })
            : null}
        </div>
        <div className="toolbar__center-group">
          <div
            className="toolbar__whitespace"
            style={{
              width: `${this.state.whiteSpaceAllotmentPx}px`
            }}
          />
        </div>
        <div className="toolbar__right-group">
          {sortedItems.map((item: ToolbarTypes.ToolbarItem, index: number) => {
            if (this.state.indicesToOverflow.indexOf(index) < 0) {
              return (
                <div
                  key={item.rank}
                  className={`toolbar-item
                    ${item.hasIssue ? 'toolbar-item--issue' : ''}`}
                  ref={ref => {
                    if (ref) {
                      this.toolbarItemRefs.push(ref);
                    }
                  }}
                >
                  {this.renderItem(item, item.hasIssue)}
                </div>
              );
            }
            return undefined;
          })}
          {this.state.indicesToOverflow.length > 0 ? (
            <div
              ref={ref => {
                if (ref) {
                  this.overflowButtonRef = ref;
                }
              }}
            >
              <Button
                icon={
                  this.props.overflowIcon ? this.props.overflowIcon : IconNames.DOUBLE_CHEVRON_RIGHT
                }
                className={`toolbar-overflow-menu-button ${
                  // Checks if any of the overflowed items have an issue
                  this.state.indicesToOverflow
                    .map(index => sortedItems[index].hasIssue)
                    .reduce((accum, val) => accum || val, false)
                    ? 'toolbar-overflow-menu-button--issue'
                    : ''
                }`}
                style={{ width: '30px' }}
                onClick={e => {
                  this.showOverflowMenu();
                }}
              />
            </div>
          ) : (
            undefined
          )}
        </div>
      </div>
    );
  }

  /**
   * hides active toolbar popup
   */
  public readonly hidePopup = () => ContextMenu.hide();

  /**
   * Enables the display of a specific popover in the toolbar -or- in the overflow menu
   *
   * @param rank the rank of the item to toggle. If the item isn't a popover nothing happens
   * @param left if given sets a manual position for the popup
   * @param right if given sets a manual position for the popup
   */
  public readonly togglePopover = (rank: number) => {
    const maybeItem: ToolbarTypes.ToolbarItem | undefined = this.props.items.find(
      i => i.rank === rank
    );
    if (maybeItem !== undefined) {
      if (maybeItem.type === ToolbarTypes.ToolbarItemType.Popover) {
        const popoverButton = this.popoverButtonMap.get(maybeItem.rank);
        if (popoverButton) {
          ContextMenu.hide();
          if (!popoverButton.isExpanded()) {
            popoverButton.togglePopover();
          }
        } else {
          ContextMenu.hide();
          const itemAsPopoverItem = maybeItem as ToolbarTypes.PopoverItem;
          if (itemAsPopoverItem.popoverContent) {
            this.toolbarRef.getBoundingClientRect();
            ContextMenu.show(
              itemAsPopoverItem.popoverContent,
              {
                left: this.toolbarRef.getBoundingClientRect().right,
                top: this.toolbarRef.getBoundingClientRect().bottom
              },
              undefined,
              true
            );
          }
        }
      }
    }
  }
  // TODO split this out into its own component
  private readonly showOverflowMenu = () => {
    const sortedItems = [...this.props.items].sort((a, b) => a.rank - b.rank);
    const overflowItems = sortedItems.filter(
      (item, index) => this.state.indicesToOverflow.indexOf(index) >= 0
    );
    const overFlowMenu = (
      <Menu>{overflowItems.map(item => this.renderMenuItem(item, item.hasIssue, uniqueId()))}</Menu>
    );
    if (this.overflowButtonRef) {
      const left = this.overflowButtonRef.getBoundingClientRect().right;
      const top =
        this.overflowButtonRef.getBoundingClientRect().top +
        this.overflowButtonRef.scrollHeight +
        4;
      ContextMenu.show(overFlowMenu, { left, top }, undefined, true);
    }
  }

  private readonly renderMenuItem = (
    item: ToolbarTypes.ToolbarItem,
    hasIssue: boolean,
    menuKey: string
  ): JSX.Element => (
    <ToolbarMenuItemRenderer item={item} hasIssue={hasIssue} menuKey={menuKey} key={menuKey} />
  )
  /**
   * Generate the items for the toolbar
   *
   * @param item Item to be rendered as widget
   * @param hasIssue rendered item has an issue to indicate
   */
  private readonly renderItem = (
    item: ToolbarTypes.ToolbarItem,
    hasIssue: boolean = false
  ): JSX.Element => (
    <ToolbarItemRenderer
      addToPopoverMap={(key, val) => this.popoverButtonMap.set(key, val)}
      item={item}
      key={item.rank}
      hasIssue={hasIssue}
    />
  )

  private getSizeOfItems() {
    const widthPx =
      this.toolbarItemRefs.length > 0
        ? this.toolbarItemRefs
            .map(ref => ref.getBoundingClientRect().width)
            .reduce((accumulator, currentValue) => accumulator + currentValue)
        : 0;
    return widthPx;
  }

  private getSizeOfLeftItems() {
    const widthPx =
      this.toolbarItemLeftRefs.length > 0
        ? this.toolbarItemLeftRefs
            .map(ref => ref.getBoundingClientRect().width)
            .reduce((accumulator, currentValue) => accumulator + currentValue)
        : 0;
    return widthPx;
  }

  private getSizeOfAllRenderedItems() {
    const totalWidth =
      this.getSizeOfLeftItems() + this.state.whiteSpaceAllotmentPx + this.getSizeOfItems();
    return totalWidth;
  }

  /* Handles toolbar re-sizing to ensure elements are always accessible */
  private readonly handleResize = () => {
    // Calculate the width of all rendered elements in the toolbar - our 'pixel debt'
    const totalWidth = this.getSizeOfAllRenderedItems();
    // Check to see how many pixels "over budget" the toolbar is
    let overflowWidthPx = totalWidth - this.props.toolbarWidthPx + AMOUNT_OF_SPACE_TO_RESERVE_PX;
    let reduceWhiteSpaceTo = this.props.minWhiteSpacePx ? this.props.minWhiteSpacePx : 0;

    if (overflowWidthPx > 0) {
      // The first priority is to sacrifice whitespace, until the whitespace allocation === minWhiteSpacePx
      if (this.state.whiteSpaceAllotmentPx > reduceWhiteSpaceTo) {
        // The maximum amount of whitespace we can get rid of
        const reducibleWhiteSpacePx = this.state.whiteSpaceAllotmentPx - reduceWhiteSpaceTo;
        const reduceWhiteSpaceByPx =
          reducibleWhiteSpacePx <= overflowWidthPx ? reducibleWhiteSpacePx : overflowWidthPx;
        overflowWidthPx -= reduceWhiteSpaceByPx;
        reduceWhiteSpaceTo = this.state.whiteSpaceAllotmentPx - reduceWhiteSpaceByPx;
        this.setState({
          whiteSpaceAllotmentPx: reduceWhiteSpaceTo,
          checkSizeOnNextDidMountOrDidUpdate: true
        });
      } else {
        // The next priority is to overflow right-aligned menu items into an overflow button
        // When we create an overflow button, it also takes up space, so we account for that
        overflowWidthPx += WIDTH_OF_OVERFLOW_BUTTON_PX;
        const indicesToOverflow: number[] = this.state.indicesToOverflow;
        // Loop backwards through our toolbar (higher rank = lower priority to render)
        for (let i = this.toolbarItemRefs.length - 1; i >= 0; i -= 1) {
          // If the item is already overflowed, then removing it won't reduce our 'debt'
          if (this.state.indicesToOverflow.indexOf(i) >= 0) {
            continue;
          }
          const item = this.toolbarItemRefs[i];
          overflowWidthPx = overflowWidthPx - item.getBoundingClientRect().width;
          // Push item to overflow list
          indicesToOverflow.push(i);
          if (overflowWidthPx <= 0) {
            break;
          }
          continue;
        }
        this.setState({
          indicesToOverflow,
          checkSizeOnNextDidMountOrDidUpdate: false
        });
      }
    } else {
      // If nothing is overflowed and the overflow is negative, add whitespace
      if (overflowWidthPx < 0 && this.state.indicesToOverflow.length === 0) {
        // If we have excess overflow to start, then we add to whitespace and end
        const surplus = Math.floor(Math.abs(overflowWidthPx));
        const increaseWhiteSpaceTo = this.state.whiteSpaceAllotmentPx + surplus;
        this.setState({
          indicesToOverflow: [],
          whiteSpaceAllotmentPx: increaseWhiteSpaceTo,
          checkSizeOnNextDidMountOrDidUpdate: false
        });
      } else if (overflowWidthPx !== 0) {
        const resizeNextTime = !this.state.checkSizeOnNextDidMountOrDidUpdate;
        this.setState({
          indicesToOverflow: [],
          whiteSpaceAllotmentPx: reduceWhiteSpaceTo,
          checkSizeOnNextDidMountOrDidUpdate: resizeNextTime
        });
      } else {
        this.setState({ checkSizeOnNextDidMountOrDidUpdate: false });
      }
    }
  }
}
