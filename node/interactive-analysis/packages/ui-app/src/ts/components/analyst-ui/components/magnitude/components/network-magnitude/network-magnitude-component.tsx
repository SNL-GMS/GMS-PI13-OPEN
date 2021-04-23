import { EventTypes } from '@gms/common-graphql';
import { uuid } from '@gms/common-util';
import { Table, TableApi } from '@gms/ui-core-components';
import classNames from 'classnames';
import defer from 'lodash/defer';
import sortBy from 'lodash/sortBy';
import React from 'react';
import { getNetworkMagSolution } from '~analyst-ui/common/utils/magnitude-util';
import { generateNetworkMagnitudeColumnDefs } from './table-utils/column-defs';
import {
  NetworkMagnitudeData,
  NetworkMagnitudeProps,
  NetworkMagnitudeRow,
  NetworkMagnitudeState
} from './types';

/**
 * Network magnitude table, uses table from core-components
 */
export class NetworkMagnitude extends React.Component<
  NetworkMagnitudeProps,
  NetworkMagnitudeState
> {
  /** The ag-grid table reference */
  private mainTable: TableApi;

  /**
   * constructor
   */
  public constructor(props: NetworkMagnitudeProps) {
    super(props);
  }

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * React component lifecycle
   *
   * @param prevProps The previous properties available to this react component
   */
  public componentDidUpdate(prevProps: NetworkMagnitudeProps, prevState: NetworkMagnitudeState) {
    // If the selected solution has changed select it
    if (prevProps.selectedSolutionId !== this.props.selectedSolutionId) {
      this.selectRowsFromProps(this.props);
    }
  }

  /**
   * Renders the component.
   */
  public render() {
    const rowData = this.generateTableRows();
    return (
      <div className={classNames('ag-theme-dark', 'table-container')}>
        <div className="list-wrapper">
          <div className={'max'}>
            <Table
              id="table-network-magnitude"
              key="table-network-magnitude"
              columnDefs={generateNetworkMagnitudeColumnDefs(this.props.displayedMagnitudeTypes)}
              gridOptions={this.props.options}
              rowData={rowData}
              getRowNodeId={node => node.id}
              deltaRowDataMode={true}
              rowSelection="multiple"
              onGridReady={this.onMainTableReady}
              rowDeselection={true}
              suppressContextMenu={true}
              onRowClicked={this.onRowClicked}
              overlayNoRowsTemplate="No Data"
            />
          </div>
        </div>
      </div>
    );
  }
  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Event handler for ag-gird that is fired when the table is ready
   *
   * @param event event of the table action
   */
  private readonly onMainTableReady = (event: any) => {
    this.mainTable = event.api;
    this.mainTable.setSortModel([{ colId: 'count', sort: 'desc' }]);
  }

  /**
   * Generate NetworkMagnitudeRow[] based on query.
   *
   * @returns a NetworkMagnitudeRow[]
   */
  private readonly generateTableRows = (): NetworkMagnitudeRow[] => {
    if (!this.props.locationSolutionSet) return [];
    const rows = [];
    this.props.locationSolutionSet.locationSolutions.forEach(ls => {
      const dataForMagnitude = new Map<EventTypes.MagnitudeType, NetworkMagnitudeData>();
      Object.keys(EventTypes.MagnitudeType).forEach(key => {
        const defStations = this.calculateDefiningStationNumbers(ls, EventTypes.MagnitudeType[key]);
        const magSolution = getNetworkMagSolution(ls, EventTypes.MagnitudeType[key]);
        dataForMagnitude.set(EventTypes.MagnitudeType[key], {
          magnitude: magSolution ? magSolution.magnitude : undefined,
          stdDeviation: magSolution ? magSolution.uncertainty : undefined,
          numberOfDefiningStations: defStations ? defStations.numberOfDefining : undefined,
          numberOfNonDefiningStations: defStations ? defStations.numberOfNonDefining : undefined
        });
      });
      const locationSolutionRow: NetworkMagnitudeRow = {
        id: uuid.asString(),
        isPreferred: ls.id === this.props.preferredSolutionId,
        location: EventTypes.PrettyDepthRestraint[ls.locationRestraint.depthRestraintType],
        dataForMagnitude
      };
      rows.push(locationSolutionRow);
    });
    return sortBy(rows, row => row.location);
  }

  /**
   * Calculates the number of defining and non defining stations for a location solution
   *
   * @param locationSolution a location solution
   * @param magnitudeType a magnitude type
   *
   * @returns a object {numberOfDefining, numberOfNonDefining}
   */
  private readonly calculateDefiningStationNumbers = (
    locationSolution: EventTypes.LocationSolution,
    magnitudeType: string
  ): { numberOfDefining: number; numberOfNonDefining: number } => {
    const networkMagnitudeSolution = locationSolution.networkMagnitudeSolutions.find(
      netMagSol => netMagSol.magnitudeType === magnitudeType
    );
    let numberOfNetworkMagnitudeBehaviors;
    let numberOfDefining = 0;
    if (networkMagnitudeSolution) {
      numberOfNetworkMagnitudeBehaviors = networkMagnitudeSolution.networkMagnitudeBehaviors.length;
      networkMagnitudeSolution.networkMagnitudeBehaviors.forEach(netMagBehavior => {
        if (netMagBehavior.defining) {
          numberOfDefining = numberOfDefining + 1;
        }
      });
    }
    if (numberOfNetworkMagnitudeBehaviors) {
      return {
        numberOfDefining,
        numberOfNonDefining: numberOfNetworkMagnitudeBehaviors - numberOfDefining
      };
    }
    return { numberOfDefining: undefined, numberOfNonDefining: undefined };
  }

  /**
   * Update the selected location solution when the user clicks on a row in the table.
   */
  private readonly onRowClicked = (rowParams: any) => {
    if (this.mainTable) {
      defer(() => {
        const selectedId = this.mainTable.getSelectedNodes().map(node => node.data.id)[0];
        this.props.setSelectedLocationSolution(this.props.locationSolutionSet.id, selectedId);
      });
    }
  }

  /**
   * Select rows in the table based on the selected location solution id in the properties.
   *
   * @param props signal detection props
   */
  private readonly selectRowsFromProps = (props: NetworkMagnitudeProps) => {
    if (this.mainTable) {
      this.mainTable.deselectAll();
      this.mainTable.forEachNode(node => {
        if (node.data.id === props.selectedSolutionId) {
          node.setSelected(true);
          // Must pass in null here as ag-grid expects it
          this.mainTable.ensureNodeVisible(node, null);
        }
      });
    }
  }
}
