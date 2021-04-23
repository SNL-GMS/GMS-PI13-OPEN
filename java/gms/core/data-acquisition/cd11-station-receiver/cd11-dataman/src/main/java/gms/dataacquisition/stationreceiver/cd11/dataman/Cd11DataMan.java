package gms.dataacquisition.stationreceiver.cd11.dataman;

import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AcknackFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ByteFrame;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;

/**
 * Processing class responsible for handling interactions with the data source.
 *
 * These processes include:
 * <ul>
 *   <li>Sending {@link Cd11AcknackFrame}s on an interval to the data source</li>
 *   <li>Handling receipt of {@link Cd11AcknackFrame}s from the data source</li>
 *   <li>Maintaining gap state and storing a recoverable copy to disk</li>
 *   <li>Reading in {@link Cd11ByteFrame} messages, parsing them into {@link RawStationDataFrame}s,
 *   and publishing to the appropriate topic.</li>
 * </ul>
 */
public interface Cd11DataMan {

  /**
   * Conducts any bookkeeping and setup necessary to initialize DataMan for handling data, then
   * starts DataMan
   */
  void execute();
}
