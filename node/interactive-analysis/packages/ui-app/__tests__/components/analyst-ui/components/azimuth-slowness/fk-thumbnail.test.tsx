import { shallow } from 'enzyme';
import React from 'react';
import renderer from 'react-test-renderer';
import {
  FkThumbnail,
  FkThumbnailProps
} from '../../../../../src/ts/components/analyst-ui/components/azimuth-slowness/components/fk-thumbnail';
import { FkUnits } from '../../../../../src/ts/components/analyst-ui/components/azimuth-slowness/types';
import { fkSpectra } from './fk-util.test';

// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

const pixelSize: any = 200;
const mockCallBack = jest.fn();

const fkThumbProps: FkThumbnailProps = {
  // TODO useless test
  fkData: fkSpectra,
  predictedPoint: undefined,
  sizePx: pixelSize,
  label: 'USRK P',
  selected: false,
  fkUnit: FkUnits.FSTAT,
  dimFk: false,
  onClick: mockCallBack,
  showFkThumbnailMenu: () => {
    /** empty */
  },
  arrivalTimeMovieSpectrumIndex: 0
};

it('FkThumbnails renders & matches snapshot', () => {
  const tree = renderer.create(<FkThumbnail {...fkThumbProps} />).toJSON();

  expect(tree).toMatchSnapshot();
});

it('FkThumbnails onClick fires correctly', () => {
  const thumbnail = shallow(<FkThumbnail {...fkThumbProps} />);
  thumbnail.find('.fk-thumbnail').simulate('click');
  expect(mockCallBack).toBeCalled();
});
