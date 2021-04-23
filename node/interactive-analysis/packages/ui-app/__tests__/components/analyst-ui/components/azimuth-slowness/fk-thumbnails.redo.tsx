import { EventTypes } from '@gms/common-graphql';
import { ReactWrapper } from 'enzyme';
import Immutable from 'immutable';
import React from 'react';
import {
  FkThumbnailList,
  FkThumbnailListProps
} from '../../../../../src/ts/components/analyst-ui/components/azimuth-slowness/components/fk-thumbnail-list/fk-thumbnail-list';
import { FkUnits } from '../../../../../src/ts/components/analyst-ui/components/azimuth-slowness/types';
import { signalDetectionsData } from '../../../../__data__/signal-detections-data';

// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Adapter = require('enzyme-adapter-react-16');

const mockProps: Partial<FkThumbnailListProps> = {
  sortedSignalDetections: signalDetectionsData,
  signalDetectionIdsToFeaturePrediction: Immutable.Map<string, EventTypes.FeaturePrediction[]>(),
  thumbnailSizePx: 300,
  selectedSdIds: [],
  unassociatedSdIds: [],
  fkUnitsForEachSdId: Immutable.Map<string, FkUnits>(),
  clearSelectedUnassociatedFks: () => {
    /** empty */
  },
  markFksForSdIdsAsReviewed: () => {
    /** empty */
  },
  showFkThumbnailContextMenu: () => {
    /** empty */
  },
  setSelectedSdIds: (ids: string[]) => {
    /** empty */
  }
};

describe('FK thumbnails tests', () => {
  // enzyme needs a new adapter for each configuration
  beforeEach(() => {
    Enzyme.configure({ adapter: new Adapter() });
  });

  it('renders a snapshot', (done: jest.DoneCallback) => {
    // Mounting enzyme into the DOM
    // Using a testing DOM not real DOM
    // So a few things will be missing window.fetch, or alert etc...
    const wrapper: ReactWrapper = Enzyme.mount(
      // 3 nested components would be needed if component dependent on apollo-redux for example see workflow
      // (apollo provider, provider (redux provider), Redux-Apollo (our wrapped component)
      <FkThumbnailList {...(mockProps as any)} />
    );

    setImmediate(() => {
      wrapper.update();

      expect(wrapper.find(FkThumbnailList)).toMatchSnapshot();

      done();
    });
  });
});
