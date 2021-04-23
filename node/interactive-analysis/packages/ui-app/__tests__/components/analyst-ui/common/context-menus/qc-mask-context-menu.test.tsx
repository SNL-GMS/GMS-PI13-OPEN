import { QcMaskTypes } from '@gms/common-graphql';
import { QcMaskDialogBoxType } from '../../../../../src/ts/components/analyst-ui/common//dialogs/types';
import {
  getOnClickOpenDialog,
  QcMaskContextMenu,
  QcMaskDialogOpener
} from '../../../../../src/ts/components/analyst-ui/common/context-menus/qc-mask-context-menu';

// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();
/**
 * Tests the signal detection context menu component
 */
describe('qc-mask-context-menu', () => {
  it('exports should be defined', () => {
    expect(QcMaskContextMenu).toBeDefined();
    expect(getOnClickOpenDialog).toBeDefined();
  });

  it('should create menu items for rejected', () => {
    const eventX = 1;
    const eventY = 2;
    const mask: Partial<QcMaskTypes.QcMask> = {};
    const openDialog: QcMaskDialogOpener = jest.fn();
    const isRejected = true;
    const qcMaskComponent = QcMaskContextMenu(eventX, eventY, mask as any, openDialog, isRejected);
    expect(qcMaskComponent).toMatchSnapshot();
    expect(qcMaskComponent).toBeTruthy();
    expect(qcMaskComponent.props.children.props.disabled).toEqual(false);
  });

  it('should create menu items', () => {
    const eventX = 1;
    const eventY = 2;
    const mask: Partial<QcMaskTypes.QcMask> = {};
    const openDialog: QcMaskDialogOpener = jest.fn();
    const isRejected = false;
    const qcMaskComponent = QcMaskContextMenu(eventX, eventY, mask as any, openDialog, isRejected);
    expect(qcMaskComponent).toMatchSnapshot();
    expect(qcMaskComponent).toBeTruthy();
  });

  it('should have an onclick function generator', () => {
    const eventX = 1;
    const eventY = 2;
    const mask: Partial<QcMaskTypes.QcMask> = {};
    const openDialog: QcMaskDialogOpener = jest.fn();
    const e = { stopPropagation: jest.fn() };

    getOnClickOpenDialog(eventX, eventY, mask, openDialog, QcMaskDialogBoxType.View)(e);
    expect(openDialog).toHaveBeenCalledTimes(1);
    expect(e.stopPropagation).toHaveBeenCalledTimes(1);
    expect(openDialog).toHaveBeenCalledWith(eventX, eventY, mask, QcMaskDialogBoxType.View);

    getOnClickOpenDialog(eventX, eventY, mask, openDialog, QcMaskDialogBoxType.Modify)(e);
    expect(openDialog).toHaveBeenCalledTimes(2);
    expect(e.stopPropagation).toHaveBeenCalledTimes(2);
    expect(openDialog).toHaveBeenCalledWith(eventX, eventY, mask, QcMaskDialogBoxType.Modify);

    getOnClickOpenDialog(eventX, eventY, mask, openDialog, QcMaskDialogBoxType.Reject)(e);
    expect(openDialog).toHaveBeenCalledTimes(3);
    expect(e.stopPropagation).toHaveBeenCalledTimes(3);
    expect(openDialog).toHaveBeenCalledWith(eventX, eventY, mask, QcMaskDialogBoxType.Reject);
  });
});
