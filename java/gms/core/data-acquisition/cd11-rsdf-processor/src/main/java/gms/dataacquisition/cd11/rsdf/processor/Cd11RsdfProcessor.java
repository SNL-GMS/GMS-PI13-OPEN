package gms.dataacquisition.cd11.rsdf.processor;

import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;

/**
 * Processing class responsible for reading in {@link RawStationDataFrame} messages, parsing them
 * into {@link AcquiredStationSohExtract}, and publishing both this extract and its {@link
 * AcquiredChannelEnvironmentIssue}s to the appropriate topics.
 */
public interface Cd11RsdfProcessor {

  /**
   * Conducts any bookkeeping and setup necessary to initialize the processor for handling data,
   * then starts the processor
   */
  void run();
}
