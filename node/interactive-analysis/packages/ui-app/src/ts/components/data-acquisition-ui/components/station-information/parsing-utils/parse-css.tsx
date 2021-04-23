import { ReferenceStationTypes } from '@gms/common-graphql';
import { julianDateToISOString } from '@gms/common-util';
import * as parseCssFields from './parse-css-fields';

/**
 * Given an array of strings representing the lines in a CSS formatted site file, return a map where the keys
 * are refstas and the values are maps containing the row with ProcessingStation information and a list of rows
 * containing ProcessingSite information
 *
 * @param siteFileContents array of strings representing the lines in a CSS formatted site file
 * @returns a map where the keys are refstas and the values are maps containing the row with ProcessingStation
 * information and a list of rows containing ProcessingSite information
 */
const createRefstaToRow = (siteFileContents: string[]): any => {
  const refstaToRow = {};

  // Go through the whole site file and identify lines where sta = refsta and add them to refstaToRow.
  // These are the processing stations.
  siteFileContents.forEach(line => {
    if (line.length === 0) return;
    const sta = parseCssFields.parseSiteSta(line);
    const refsta = parseCssFields.parseSiteRefsta(line);
    // Because sta information can be repeated multiple times in a file, create a map where sta is the key
    // and the value is a list of maps with a row representing the ProcesingStation and a list of rows
    // representing the ProcessingSites for that ProcessingStation
    if (refstaToRow[refsta] === undefined) {
      refstaToRow[refsta] = { processingStationRow: undefined, processingSiteRows: [] };
    }

    const existingProcessingStationRow = refstaToRow[refsta].processingStationRow;
    if (sta === refsta) {
      if (existingProcessingStationRow === undefined) {
        refstaToRow[refsta].processingStationRow = line;
      } else {
        // Already seen this station - keep the one with the most recent ondate
        const currentOndate = parseCssFields.parseSiteOndate(line);
        const existingOndate = parseCssFields.parseSiteOndate(existingProcessingStationRow);
        if (currentOndate > existingOndate) {
          refstaToRow[refsta].processingStationRow = line;
        }
      }
    }
  });

  // Go through the whole site file again add the lines where sta != refsta to the refstaToRow entry
  // for their refsta where the time range falls in the main time range
  siteFileContents.forEach(line => {
    if (line.length === 0) return;
    const sta = parseCssFields.parseSiteSta(line);
    const refsta = parseCssFields.parseSiteRefsta(line);

    if (sta !== refsta) {
      const lineOndate = parseCssFields.parseSiteOndate(line);
      const refstaOndate = parseCssFields.parseSiteOndate(refstaToRow[refsta].processingStationRow);
      const lineOffdate = parseCssFields.parseSiteOffdate(line);
      const refstaOffdate = parseCssFields.parseSiteOffdate(
        refstaToRow[refsta].processingStationRow
      );
      // Check that site time range falls within the refsta time range
      if (lineOndate >= refstaOndate && lineOffdate <= refstaOffdate) {
        refstaToRow[refsta].processingSiteRows.push(line);
      }
    }
  });
  return refstaToRow;
};

/**
 * Given an array of strings representing the lines in a CSS formatted sitechan file, return an array of
 * maps that contain ondate, offdate, and ProcessingChannel built with a row of sitechan information
 *
 * @param sitechanFileContents array of strings representing the lines in a CSS formatted sitechan file
 * @returns an array of maps that contain ondate, offdate, and ProcessingChannel built with a row of
 * sitechan information
 */
const createProcessingChannels = (sitechanFileContents: string[]): any => {
  const sitechanStaToChannelInfo = {};
  sitechanFileContents.forEach(line => {
    if (line.length === 0) return;

    const sta = parseCssFields.parseSitechanSta(line);
    // Because sta information can be repeated multiple times in a file, create a map where sta is the key
    // and the value is a list of maps with ondate, offdate, and ProcessingChannel information for that sta
    if (sitechanStaToChannelInfo[sta] === undefined) {
      sitechanStaToChannelInfo[sta] = [];
    }

    const currentChannel: ReferenceStationTypes.ReferenceChannel = {
      id: `UNKNOWN_${parseCssFields.getRandomId()}`,
      name: `${sta}/${parseCssFields.parseSitechanType(line)}`,
      channelType: parseCssFields.parseSitechanDescription(line),
      // Use 0 since CSS sitechan information does not have a sample rate
      sampleRate: 0,
      depth: parseCssFields.parseSitechanDepth(line),
      actualTime: '-1',
      systemTime: julianDateToISOString(parseCssFields.parseSitechanOndateAsString(line))
    };

    sitechanStaToChannelInfo[sta].push({
      ondate: parseCssFields.parseSitechanOndate(line),
      offdate: parseCssFields.parseSitechanOffdate(line),
      processingChannel: currentChannel
    });
  });
  return sitechanStaToChannelInfo;
};

/**
 * Create ProcessingSites from site rows that have site file information. These ProcessingSites have
 * lists of ProcessingChannels which are obtained from looking up ProcessingChannel information with
 * matching sta information that falls within the ProcessingSite's ondate / offdate time range.
 *
 * @param processingSiteRows site rows to use to create ProcessingSite
 * @param sitechanStaToChannelInfo map from sitechan sta information to sitechan information (used to
 * find the processing channels for each processing site)
 */
const createProcessingSites = (
  processingSiteRows: string[],
  sitechanStaToChannelInfo: {}
): ReferenceStationTypes.ReferenceSite[] => {
  const processingSites = [];
  processingSiteRows.forEach(line => {
    const sta = parseCssFields.parseSiteSta(line);
    const staOndate = parseCssFields.parseSiteOndate(line);
    const staOffdate = parseCssFields.parseSiteOffdate(line);
    const staDnorth = parseCssFields.parseSiteDnorth(line);
    const staDeast = parseCssFields.parseSiteDeast(line);
    const processingChannelsInTimeRange = [];
    if (sitechanStaToChannelInfo[sta] != undefined) {
      sitechanStaToChannelInfo[sta].forEach(chan => {
        const chanOndate = chan.ondate;
        const chanOffdate = chan.offdate;

        // Check that sitechan time range falls within site time range
        if (chanOndate >= staOndate && chanOffdate <= staOffdate) {
          // This is a little odd that the channel information comes from the site
          // files but it results from different representations across CSS and the OSD
          chan.processingChannel.position = {
            eastDisplacementKm: staDnorth,
            northDisplacementKm: staDeast,
            verticalDisplacementKm: 0
          };
          processingChannelsInTimeRange.push(chan.processingChannel);
        }
      });
    }

    const currentSite: ReferenceStationTypes.ReferenceSite = {
      id: `UNKNOWN_${parseCssFields.getRandomId()}`,
      name: sta,
      location: {
        latitudeDegrees: parseCssFields.parseSiteLatitude(line),
        longitudeDegrees: parseCssFields.parseSiteLongitude(line),
        elevationKm: parseCssFields.parseSiteElevation(line)
      },
      channels: processingChannelsInTimeRange
    };
    processingSites.push(currentSite);
  });

  return processingSites;
};

/**
 * Given the contents of site and sitechan files, create ProcessingStations from that information
 *
 * @param siteFileConents string[] where each element is a line from a site file
 * @param sitechanFileContents string[] where each element is a line from a sitechan file
 * @returns array of ProcessingStations
 */
export function createProcessingStations(
  siteFileContents: string[],
  sitechanFileContents: string[]
): ReferenceStationTypes.ReferenceStation[] {
  // Create ProcessingChannels from sitechan file contents
  const sitechanStaToChannelInfo = createProcessingChannels(sitechanFileContents);
  // Create mapping from refsta to file row with ProcessingStation information and a list of file
  // rows containing ProcessingSite information
  const refstaToRow = createRefstaToRow(siteFileContents);

  const assembledStations = [];
  const refstaList = Object.keys(refstaToRow);
  refstaList.forEach(refsta => {
    const refstaRow = refstaToRow[refsta].processingStationRow;
    const sta = parseCssFields.parseSiteSta(refstaRow);

    const hasProcessingSites = refstaToRow[refsta].processingSiteRows.length > 0;
    // If we have a station that doesn't have any processing sites, then it is its own processing site
    const processingSiteRows: string[] = hasProcessingSites
      ? refstaToRow[refsta].processingSiteRows
      : [refstaToRow[refsta].processingStationRow];
    const processingSites: ReferenceStationTypes.ReferenceSite[] = createProcessingSites(
      processingSiteRows,
      sitechanStaToChannelInfo
    );
    const currentStation: ReferenceStationTypes.ReferenceStation = {
      id: sta,
      name: sta,
      stationType: parseCssFields.determineStationType(
        refsta,
        sta,
        parseCssFields.parseSiteStatype(refstaRow)
      ),
      description: parseCssFields.parseSiteDescription(refstaRow),
      defaultChannel: undefined,
      networks: [],
      modified: false,
      location: {
        latitudeDegrees: parseCssFields.parseSiteLatitude(refstaRow),
        longitudeDegrees: parseCssFields.parseSiteLongitude(refstaRow),
        elevationKm: parseCssFields.parseSiteElevation(refstaRow)
      },
      sites: processingSites,
      dataAcquisition: undefined,
      latitude: parseCssFields.parseSiteLatitude(refstaRow),
      longitude: parseCssFields.parseSiteLongitude(refstaRow),
      elevation: parseCssFields.parseSiteElevation(refstaRow)
    };
    assembledStations.push(currentStation);
  });
  return assembledStations;
}

/**
 * Given a file, return a Promise that, when fulfilled, will return an array of strings where array element
 * represents a line in the input file
 *
 * @param inputFile file to read
 * @returns Promise that resolves to array of strings where each array element represents a line in the input file
 */
export async function readUploadedFileAsText(inputFile: any): Promise<any> {
  const fileReader = new FileReader();

  return new Promise<any>((resolve, reject) => {
    fileReader.onerror = () => {
      fileReader.abort();
      reject('Problem parsing input file.');
    };

    fileReader.onload = () => {
      const content = fileReader.result as string;
      const splitContent = content.split('\n');
      const lines = [];
      splitContent.forEach(line => {
        lines.push(line);
      });
      resolve(lines);
    };
    fileReader.readAsText(inputFile);
  });
}
