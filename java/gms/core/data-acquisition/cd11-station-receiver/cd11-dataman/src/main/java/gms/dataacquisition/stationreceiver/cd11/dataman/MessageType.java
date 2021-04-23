package gms.dataacquisition.stationreceiver.cd11.dataman;


enum MessageType {
  NEW_FRAME_RECEIVED,
  PERSIST_GAP_STATE,
  REMOVE_EXPIRED_GAPS,
  SEND_ACKNACK,
  SHUTDOWN
}
