import { Button, Intent, NonIdealState, Spinner } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import { CommonTypes, DataAcquisitionTypes, ReferenceStationTypes } from '@gms/common-graphql';
import { createDropdownItems, Table } from '@gms/ui-core-components';
import classNames from 'classnames';
import isEqual from 'lodash/isEqual';
import sortBy from 'lodash/sortBy';
import React from 'react';
import { dataDestinationsDefs } from './data-destinations/table-utils/column-defs';
import { DataDestinationsRow } from './data-destinations/types';
import { dataSourcesDefs } from './data-sources/table-utils/column-defs';
import { DataSourcesRow } from './data-sources/types';
import { channelDefs } from './station-configuration-channels/table-utils/column-defs';
import { stationConfigDefs } from './station-configuration-main/table-utils/column-defs';
import { StationConfigurationRow } from './station-configuration-main/types';
import { StationConfigurationProps, StationConfigurationState } from './types';
import { generateDataDestinationsData, generateDataSourcesData } from './util/data-generation';

// ***************************************
// BEGIN REACT COMPONENT LIFECYCLE METHODS
// ***************************************

/**
 * The StationConfiguration component displays and configures station details from the StationInformation display.
 */
export class StationConfiguration extends React.Component<
  StationConfigurationProps,
  StationConfigurationState
> {
  // generate sources and destinations data as we don't currently have a source for it
  private readonly dataSourcesRowData: DataSourcesRow[] = generateDataSourcesData();
  private readonly dataDestinationsRowData: DataDestinationsRow[] = generateDataDestinationsData();

  /**
   * Constructor.
   */
  public constructor(props: StationConfigurationProps) {
    super(props);
    this.state = {
      station: '',
      description: '',
      sites: [],
      stationType: CommonTypes.StationType.UNKNOWN,
      latitude: 0,
      longitude: 0,
      elevation: 0
    };
  }

  /**
   * If the props changed, update the state to force a renrender.
   * @param prevProps the previous props
   * @param prevState the previous state
   */
  public componentDidUpdate(prevProps, prevState) {
    if (this.props.selectedProcessingStation) {
      const name = this.props.selectedProcessingStation.name;
      if (this.state.station !== name) {
        this.setState({ station: name });
      }
      const d = this.props.selectedProcessingStation.description;
      if (this.state.description !== d) {
        this.setState({ description: d });
      }
      const s = this.props.selectedProcessingStation.sites;
      if (!isEqual(this.state.sites, s)) {
        this.setState({ sites: s });
      }
      const lat = this.props.selectedProcessingStation.latitude;
      if (this.state.latitude !== lat) {
        this.setState({ latitude: lat });
      }
      const lon = this.props.selectedProcessingStation.longitude;
      if (this.state.longitude !== lon) {
        this.setState({ longitude: lon });
      }
      const elev = this.props.selectedProcessingStation.elevation;
      if (this.state.elevation !== elev) {
        this.setState({ elevation: elev });
      }
      const staType = this.props.selectedProcessingStation.stationType;
      if (this.state.stationType !== staType) {
        this.setState({ stationType: staType });
      }
    }
  }

  /**
   * Renders the component.
   */
  public render() {
    if (this.props.defaultStationsQuery.loading) {
      return (
        <NonIdealState
          action={<Spinner intent={Intent.PRIMARY} />}
          title="Loading:"
          description={'Station configuration...'}
        />
      );
    }
    if (!this.props.selectedStationIds || this.props.selectedStationIds.length <= 0) {
      return <NonIdealState icon={IconNames.HEAT_GRID} title="No Station Selected or Loaded" />;
    }
    if (this.props.selectedStationIds.length > 1) {
      return <NonIdealState icon={IconNames.HEAT_GRID} title="Select One Station" />;
    }

    return (
      <div className={classNames('ag-theme-dark', 'table-container')}>
        <div
          style={{
            position: 'absolute',
            display: 'flex',
            justifyContent: 'flex-beginning',
            left: '0.5%',
            top: '5%'
          }}
          className="data-acquisition-inputs"
        >
          <form>
            <label style={{ padding: '15px' }}>Station:</label>
            <input
              autoComplete="off"
              id="stationName"
              onChange={this.handleNameFormChange}
              value={this.state.station}
            />
            <label style={{ padding: '20px', display: 'inline' }}>Station Type:</label>
            <select value={this.state.stationType} onChange={this.handleDropDownChange}>
              {createDropdownItems(CommonTypes.StationType)}
            </select>
            <label style={{ padding: '21px' }}>Name:</label>
            <input
              autoComplete="off"
              id="description"
              onChange={this.handleDescriptionFormChange}
              value={this.state.description}
            />
          </form>
        </div>
        <br />
        <div
          style={{
            position: 'absolute',
            display: 'flex',
            justifyContent: 'flex-beginning',
            left: '0.7%',
            top: '13%'
          }}
          className="data-acquisition-inputs"
        >
          <form>
            <label style={{ padding: '10px' }}>Latitude:</label>
            <input
              autoComplete="off"
              id="latitude"
              onChange={this.handleLatitudeFormChange}
              value={this.state.latitude}
            />
            <label style={{ padding: '28px' }}>Longitude:</label>
            <input
              autoComplete="off"
              id="longitude"
              onChange={this.handleLongitudeFormChange}
              value={this.state.longitude}
            />
            <label style={{ padding: '14px' }}>Elevation:</label>
            <input
              autoComplete="off"
              id="elevation"
              onChange={this.handleElevationFormChange}
              value={this.state.elevation}
            />
          </form>
        </div>
        <div className={'duo-grid-left'}>
          <Table
            id="table-station-configuration-left"
            key="table-station-configuration-left"
            context={{}}
            columnDefs={dataSourcesDefs}
            rowData={this.dataSourcesRowData}
            getRowNodeId={node => node.id}
            deltaRowDataMode={true}
            rowSelection="multiple"
            rowDeselection={true}
            suppressContextMenu={true}
          />
        </div>
        <div className={'duo-grid-right'}>
          <Table
            id="table-station-configuration-right"
            key="table-station-configuration-right"
            context={{}}
            columnDefs={dataDestinationsDefs}
            rowData={this.dataDestinationsRowData}
            getRowNodeId={node => node.id}
            deltaRowDataMode={true}
            rowSelection="multiple"
            rowDeselection={true}
            suppressContextMenu={true}
          />
        </div>
        <div className={'station-configuration-inner-menu'}>
          <Table
            id="table-station-configuration-inner"
            key="table-station-configuration-inner"
            context={{}}
            columnDefs={stationConfigDefs}
            rowData={this.generateStationConfigurationData()}
            getRowNodeId={node => node.id}
            deltaRowDataMode={true}
            rowSelection="multiple"
            rowDeselection={true}
            suppressContextMenu={true}
          />
        </div>
        <div className={'station-configuration-bottom-menu'}>
          <Table
            id="table-station-configuration-bottom"
            key="table-station-configuration-bottom"
            context={{}}
            columnDefs={channelDefs}
            rowData={this.generateStationConfigurationChannelData()}
            getRowNodeId={node => node.id}
            deltaRowDataMode={true}
            rowSelection="multiple"
            rowDeselection={true}
            suppressContextMenu={true}
          />
        </div>
        <div className={'footer-button-row'}>
          <Button
            text="Save"
            onClick={e => this.uploadStation(this.props.selectedProcessingStation)}
          />
          <Button text="Discard" onClick={e => this.handleCancel(e)} />
        </div>
      </div>
    );
  }

  private readonly handleCancel = (event: any) => {
    this.props.setSelectedProcessingStation({
      ...this.props.selectedProcessingStation,
      latitude: this.props.unmodifiedProcessingStation.latitude,
      longitude: this.props.unmodifiedProcessingStation.longitude,
      name: this.props.unmodifiedProcessingStation.name,
      elevation: this.props.unmodifiedProcessingStation.elevation,
      sites: this.props.unmodifiedProcessingStation.sites,
      description: this.props.unmodifiedProcessingStation.description,
      stationType: this.props.unmodifiedProcessingStation.stationType
    });

    this.setState({
      ...this.state,
      station: this.props.unmodifiedProcessingStation.name,
      description: this.props.unmodifiedProcessingStation.description,
      sites: this.props.unmodifiedProcessingStation.sites,
      latitude: this.props.unmodifiedProcessingStation.latitude,
      longitude: this.props.unmodifiedProcessingStation.longitude,
      elevation: this.props.unmodifiedProcessingStation.elevation,
      stationType: this.props.unmodifiedProcessingStation.stationType
    });
  }

  private readonly handleDropDownChange = (event: any) => {
    const targetValue = event.currentTarget.value as CommonTypes.StationType;
    const currentStationValues: ReferenceStationTypes.ReferenceStation = this.props
      .selectedProcessingStation;
    currentStationValues.stationType = targetValue;
    this.setState({
      stationType: targetValue
    });
    this.props.setSelectedProcessingStation(currentStationValues);
  }

  /**
   * Methods to handle form input
   */
  private readonly handleElevationFormChange = (event: any) => {
    this.setState({ elevation: event.target.value });
    this.props.setSelectedProcessingStation({
      ...this.props.selectedProcessingStation,
      elevation: event.target.value
    });
  }

  private readonly handleLongitudeFormChange = (event: any) => {
    this.setState({ longitude: event.target.value });
    this.props.setSelectedProcessingStation({
      ...this.props.selectedProcessingStation,
      longitude: event.target.value
    });
  }

  private readonly handleLatitudeFormChange = (event: any) => {
    this.setState({ latitude: event.target.value });
    this.props.setSelectedProcessingStation({
      ...this.props.selectedProcessingStation,
      latitude: event.target.value
    });
  }

  private readonly handleNameFormChange = (event: any) => {
    this.setState({ station: event.target.value });
    this.props.setSelectedProcessingStation({
      ...this.props.selectedProcessingStation,
      name: event.target.value
    });
  }

  private readonly handleDescriptionFormChange = (event: any) => {
    this.setState({ description: event.target.value });
    this.props.setSelectedProcessingStation({
      ...this.props.selectedProcessingStation,
      description: event.target.value
    });
  }

  /**
   * Create table data based on the current state.
   */
  private readonly generateStationConfigurationData = (): StationConfigurationRow[] => {
    const stationConfigurationRows = [];
    if (this.state.sites !== undefined) {
      this.state.sites.forEach(site => {
        const tempStationConfigurationRow: StationConfigurationRow = {
          id: site.id,
          name: site.name,
          siteId: site.id,
          latitude: site.location ? site.location.latitudeDegrees : 0,
          longitude: site.location ? site.location.longitudeDegrees : 0,
          elevation: site.location ? site.location.elevationKm : 0,
          stationType: this.state.stationType,
          description: this.state.description,
          northOffset:
            site.channels && site.channels[0] && site.channels[0].position
              ? site.channels[0].position.northDisplacementKm
              : undefined,
          eastOffset:
            site.channels && site.channels[0] && site.channels[0].position
              ? site.channels[0].position.eastDisplacementKm
              : undefined,
          sampleRate: site.channels && site.channels[0] ? site.channels[0].sampleRate : undefined
        };
        stationConfigurationRows.push(tempStationConfigurationRow);
      });
    } else {
      const tempStationConfigurationRow: StationConfigurationRow = {
        id: '',
        name: '',
        siteId: '',
        latitude: undefined,
        longitude: undefined,
        elevation: undefined,
        stationType: '',
        description: '',
        northOffset: undefined,
        eastOffset: undefined,
        sampleRate: undefined
      };
      stationConfigurationRows.push(tempStationConfigurationRow);
    }
    return sortBy(stationConfigurationRows, 'name');
  }

  private readonly formatTime = (inputTime: string): string => {
    if (inputTime === null || inputTime === '-1' || inputTime === '-999') {
      return '-1';
      // tslint:disable-next-line:unnecessary-else
    } else {
      return inputTime.replace(/-/g, '/');
    }
  }
  /**
   * Converts channel data associated with a station to row object
   *
   * @param stationData A Processing Channel
   */
  private readonly generateStationConfigurationChannelData = (): any => {
    let channels = [];
    if (this.state.sites) {
      this.state.sites.forEach(site => {
        site.channels.forEach(channel => {
          const systemChangeTime = this.formatTime(channel.systemTime);
          const actualChangeTime = this.formatTime(channel.actualTime);
          const channelRow = {
            id: channel.id,
            channelId: channel.id,
            channelName: channel.name,
            type: channel.channelType,
            sampleRate: channel.sampleRate,
            systemChangeTime,
            actualChangeTime,
            depth: channel.depth
          };
          channels.push(channelRow);
        });
      });
      return (channels = sortBy(channels, 'channelName'));
    }
    return [];
  }

  /**
   * Stores the current Station to the OSD
   */
  private readonly uploadStation = (stationData: ReferenceStationTypes.ReferenceStation): void => {
    const now: string = new Date().toISOString();
    const infoSource: DataAcquisitionTypes.InformationSource = {
      originatingOrganization: 'External',
      informationTime: now,
      reference: 'Example'
    };
    const referenceStationToUpload: DataAcquisitionTypes.DataAcqReferenceStation = {
      name: stationData.name,
      description: stationData.description,
      stationType: stationData.stationType,
      comment: 'Uploaded from the Configure Station UI',
      source: infoSource,
      latitude: stationData.latitude ? +stationData.latitude : 0,
      longitude: stationData.longitude ? +stationData.longitude : 0,
      elevation: stationData.elevation ? +stationData.elevation : 0,
      actualChangeTime: now,
      systemChangeTime: now,
      aliases: []
    };
    this.props
      .saveReferenceStation({
        variables: {
          input: referenceStationToUpload
        },
        update: (store, { data = {} }) => {
          window.alert(`Successfully Stored Station ${referenceStationToUpload.name}`);
        }
      })
      .catch(err => window.alert(err));
  }
}
