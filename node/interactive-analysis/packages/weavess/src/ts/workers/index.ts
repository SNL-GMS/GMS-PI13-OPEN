import { RpcProvider } from 'worker-rpc';

import { createPositionBufferForDataBySampleRate } from './create-position-buffer';
import { createRecordSectionPositionBuffer } from './create-record-section-line';
import { WorkerOperations } from './operations';

const rpcProvider = new RpcProvider(
  // tslint:disable-next-line:no-unnecessary-callback-wrapper
  (message, transfer: any) => postMessage(message, transfer)
);
onmessage = e => rpcProvider.dispatch(e.data);

rpcProvider.registerRpcHandler(
  WorkerOperations.CREATE_POSITION_BUFFER,
  createPositionBufferForDataBySampleRate
);

rpcProvider.registerRpcHandler(
  WorkerOperations.CREATE_RECORD_SECTION_POSITION_BUFFER,
  createRecordSectionPositionBuffer
);
