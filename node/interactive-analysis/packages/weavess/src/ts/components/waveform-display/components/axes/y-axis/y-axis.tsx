import * as d3 from 'd3';
import mean from 'lodash/mean';
import React from 'react';
import { YAxisProps, YAxisState } from './types';

/**
 * Y axis for an individual waveform
 */
export class YAxis extends React.Component<YAxisProps, YAxisState> {
  /** Handle to the axis wrapper HTMLElement */
  private axisRef: HTMLElement | null;

  /** Handle to the d3 svg selection, where the axis will be created. */
  private svgAxis: d3.Selection<SVGGElement, unknown, null, undefined>;

  /**
   * Constructor
   *
   * @param props Y Axis props as YAxisProps
   */
  public constructor(props: YAxisProps) {
    super(props);
    this.state = {};
  }

  // ******************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ******************************************

  /**
   * Invoked right before calling the render method, both on the initial mount
   * and on subsequent updates. It should return an object to update the state,
   * or null to update nothing.
   *
   * @param nextProps the next props
   * @param prevState the previous state
   */
  public static getDerivedStateFromProps(nextProps: YAxisProps, prevState: YAxisState) {
    return null; /* no-op */
  }

  /**
   * React lifecycle
   *
   * @param nextProps props for the axis of type YAxisProps
   *
   * @returns boolean
   */
  public shouldComponentUpdate(nextProps: YAxisProps) {
    const hasChanged = !(
      this.props.maxAmplitude === nextProps.maxAmplitude &&
      this.props.minAmplitude === nextProps.minAmplitude &&
      this.props.heightInPercentage === nextProps.heightInPercentage
    );
    return hasChanged;
  }

  /**
   * Catches exceptions generated in descendant components.
   * Unhandled exceptions will cause the entire component tree to unmount.
   *
   * @param error the error that was caught
   * @param info the information about the error
   */
  public componentDidCatch(error, info) {
    // tslint:disable-next-line:no-console
    console.error(`Weavess YAxis Error: ${error} : ${info}`);
  }

  /**
   * Called immediately after a compoment is mounted.
   * Setting state here will trigger re-rendering.
   */
  public componentDidMount() {
    if (!this.axisRef) return;

    const svg = d3.select(this.axisRef).append('svg');
    svg
      .attr('height', this.axisRef.clientHeight)
      // tslint:disable-next-line:no-magic-numbers
      .attr('width', 50);
    this.svgAxis = svg
      .append('g')
      .attr('class', this.props.yAxisTicks ? 'y-axis-axis' : 'y-axis-axis y-axis-even-tick')
      .attr('height', this.axisRef.clientHeight)
      // tslint:disable-next-line:no-magic-numbers
      .attr('transform', `translate(${49},0)`);
    this.display();
  }

  /**
   * Called immediately after updating occurs. Not called for the initial render.
   *
   * @param prevProps the previous props
   * @param prevState the previous state
   */
  public componentDidUpdate(prevProps: YAxisProps, prevState: YAxisState) {
    this.display();
  }

  /**
   * Called immediately before a component is destroyed. Perform any necessary
   * cleanup in this method, such as cancelled network requests,
   * or cleaning up any DOM elements created in componentDidMount.
   */
  public componentWillUnmount() {
    /* no-op */
  }

  // ******************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ******************************************

  public render() {
    return (
      <div
        className="y-axis"
        ref={axisRef => {
          this.axisRef = axisRef;
        }}
        style={{
          height: `${this.props.heightInPercentage}%`
        }}
      />
    );
  }

  /**
   * Draw the axis
   */
  public readonly display = () => {
    if (!this.axisRef) return;

    const totalTicks = this.props.yAxisTicks ? this.props.yAxisTicks.length : 3;
    const heightPx = this.axisRef.clientHeight;

    d3.select(this.axisRef)
      .select('svg')
      .attr('height', heightPx)
      // tslint:disable-next-line:no-magic-numbers
      .attr('width', 50);

    // adjust the min/max with a pixel padding to ensure that all of the number are visible
    const min = this.props.minAmplitude;
    const max = this.props.maxAmplitude;

    const yAxisScale = d3
      .scaleLinear()
      .domain([min, max])
      .range([heightPx, 0]);

    let tickValues: number[] = [];

    if (this.props.yAxisTicks) {
      tickValues = this.props.yAxisTicks;
    } else {
      let meanValue = mean([min, max]);
      const meanValueZeroMin = -0.099;
      const meanValueZeroMax = 0.099;
      // make sure we don't display -0
      if (meanValue && meanValue >= meanValueZeroMin && meanValue <= meanValueZeroMax) {
        meanValue = Math.abs(meanValue);
      }
      tickValues = [min, meanValue, max];
    }

    const tickFormatter: any = (value: number): any => value.toFixed(1);

    const yAxis = d3
      .axisLeft(yAxisScale)
      .ticks(totalTicks)
      .tickFormat(tickFormatter)
      .tickSizeOuter(0)
      .tickValues(tickValues);

    this.svgAxis.call(yAxis);

    // TODO allow the tick marks to be more customizable within WEAVESS
    // TODO allow props to specify whether or not tick marks should be rendered (none, all, odd, or even)
    if (!this.props.yAxisTicks) {
      const paddingPx = 6;
      const groups: any = this.svgAxis.selectAll('text');
      d3.select(groups._groups[0][0]).attr('transform', `translate(0,${-paddingPx})`);
      d3.select(groups._groups[0][2]).attr('transform', `translate(0,${paddingPx})`);
    }
  }
}
