import { QcMaskTypes } from '@gms/common-graphql';
import React from 'react';
import {
  QcMaskForm,
  QcMaskOverlap
} from '../../../../../src/ts/components/analyst-ui/common/dialogs';
import { QcMaskDialogBoxType } from '../../../../../src/ts/components/analyst-ui/common/dialogs/types';
import { overlappingQcMaskData } from '../../../../__data__/qc-mask-data';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

const startTime = 1274394556;
const endTime = 1274394621;

it('QC Mask details renders & matches snapshot (single mask)', () => {
  const wrapper = Enzyme.shallow(
    <QcMaskForm
      qcMaskDialogBoxType={QcMaskDialogBoxType.Create}
      startTimeSecs={startTime}
      endTimeSecs={endTime}
      applyChanges={undefined}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('Multi Mask dialog renders & matches snapshot  (overlapping masks)', () => {
  const wrapper = Enzyme.shallow(
    <QcMaskOverlap
      masks={overlappingQcMaskData}
      contextMenuCoordinates={{
        xPx: 5,
        yPx: 5
      }}
      openNewContextMenu={(
        x: number,
        y: number,
        mask: QcMaskTypes.QcMask,
        dialogType: QcMaskDialogBoxType
      ) => {
        /* no-op */
      }}
      selectMask={(mask: QcMaskTypes.QcMask) => {
        /* no-op */
      }}
    />
  );
  expect(wrapper).toMatchSnapshot();
});
