import * as React from 'react';
import { DragHandleDivider } from './drag-handle-divider';
import { HorizontalDividerProps, HorizontalDividerState } from './types';

/**
 * Takes two components and will wrap them so that they can be resized with a horizontal marker between them
 */
export class HorizontalDivider extends React.Component<
  HorizontalDividerProps,
  HorizontalDividerState
> {
  private wrapper: HTMLDivElement;
  private readonly handleHeight: number = 7;
  private readonly defaultHeight: number = 200;
  private readonly minimumHeight: number = 100;
  private maxHeightPx: number = Infinity;

  /**
   * constructor
   */
  public constructor(props: HorizontalDividerProps) {
    super(props);
    this.state = {
      topComponentHeightPx: props.topHeightPx ? props.topHeightPx : this.defaultHeight
    };
  }

  /**
   * Cleans up any event listeners on unmount
   */
  public componentWillUnmount() {
    this.cleanUpEventListeners();
  }

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Renders the component.
   */
  public render() {
    this.verifyConfiguration();
    const height = `${this.getTopContainerHeight()}px`;
    return (
      <div
        className={'divider-container'}
        ref={ref => {
          if (ref) {
            this.wrapper = ref;
          }
        }}
      >
        <div className={'top-component'} style={{ height }}>
          {this.props.top}
        </div>
        <DragHandleDivider
          handleHeight={this.handleHeight}
          onDrag={this.onThumbnailHorizontalDividerDrag}
        />
        <div className={'bottom-component'} style={{ height: '1px' }}>
          {this.props.bottom}
        </div>
      </div>
    );
  }
  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Overrides default / calculated height of top table
   */
  public readonly setTopContainerHeight = (newHeightPx: number) =>
    this.setState({ topComponentHeightPx: newHeightPx })

  /**
   * Returns the height of the top container, or a default if it set to 0
   */
  public readonly getTopContainerHeight = () =>
    this.state.topComponentHeightPx === 0
      ? this.props.topHeightPx
        ? this.props.topHeightPx
        : this.defaultHeight
      : this.state.topComponentHeightPx

  /**
   * @returns the height of the bottom container
   */
  public readonly getBottomContainerHeight = () =>
    this.getTotalHeight() - (this.getTopContainerHeight() + this.getHandleHeight())

  /**
   * @returns the height of the wrapper
   */
  public readonly getTotalHeight = () => this.wrapper?.clientHeight;

  /**
   * @returns the height of the drag handle
   */
  public readonly getHandleHeight = () => this.handleHeight;

  /**
   * Gets the min top height if set in props. Otherwise, returns the default minimum height
   */
  private readonly getMinTopHeightPx = () =>
    this.props.sizeRange?.minimumTopHeightPx ?? this.minimumHeight

  /**
   * Gets the max top height if set in props. Otherwise, returns the default of 80% of the space.
   */
  private readonly getMaxTopHeightPx = () => {
    const maxHPct = 0.8;
    return this.props.sizeRange?.minimumBottomHeightPx
      ? this.wrapper.clientHeight - this.props.sizeRange.minimumBottomHeightPx
      : this.wrapper.clientHeight * maxHPct;
  }

  private readonly verifyConfiguration = () => {
    this.verifyNonZeroHeight();
    this.verifyMinDefinedHeights();
  }

  private readonly verifyNonZeroHeight = () => {
    if (this.state.topComponentHeightPx === 0) {
      console.warn('Top table got a height of 0, restoring height to default or props height');
    }
  }

  private readonly verifyMinDefinedHeights = () => {
    const minDefinedHeight =
      this.props.sizeRange?.minimumBottomHeightPx +
      this.props.sizeRange?.minimumTopHeightPx +
      this.handleHeight;
    if (this.wrapper && minDefinedHeight > this.wrapper.clientHeight) {
      console.warn(
        'Horizontal divider is smaller than defined minimum heights. Try decreasing the minimum heights in the range, or increasing the size of the container.'
      );
    }
  }

  /**
   * Handles mouse move while dragging.
   * Resizes the top and bottom containers based on the mouse position.
   * If onResize is provided, calls the onResize function from props, and passes
   * it the topComponentHeightPx value.
   */
  private readonly onMouseMove = (e2: MouseEvent) => {
    let curPos = e2.clientY - this.wrapper.getBoundingClientRect().top;
    curPos = Math.max(curPos, this.getMinTopHeightPx());
    curPos = Math.min(curPos, this.maxHeightPx);
    if (curPos < this.maxHeightPx && curPos > this.getMinTopHeightPx()) {
      this.setState({ topComponentHeightPx: curPos }, () => {
        this.props.onResize && this.props.onResize(this.state.topComponentHeightPx);
      });
    }
  }

  /**
   * The handler for mouseUp.
   * Cleans up event listeners and calls the onResizeEnd function from props, if provided.
   */
  private readonly onMouseUp = (e2: MouseEvent) => {
    this.cleanUpEventListeners();
    this.props.onResizeEnd && this.props.onResizeEnd(this.state.topComponentHeightPx);
  }

  /**
   * Cleans up the event listeners for mouseup and mousemove that are attached to the body.
   */
  private readonly cleanUpEventListeners = () => {
    document.body.removeEventListener('mousemove', this.onMouseMove);
    document.body.removeEventListener('mouseup', this.onMouseUp);
  }

  /**
   * Start a drag on mouse down on the horizontal divider. Calls the onResize callback from props
   * if provided
   *
   * @param e mouse event
   */
  private readonly onThumbnailHorizontalDividerDrag = (e: React.MouseEvent<HTMLDivElement>) => {
    e.preventDefault();
    this.maxHeightPx = this.getMaxTopHeightPx();
    document.body.addEventListener('mousemove', this.onMouseMove);
    document.body.addEventListener('mouseup', this.onMouseUp);
  }
}
