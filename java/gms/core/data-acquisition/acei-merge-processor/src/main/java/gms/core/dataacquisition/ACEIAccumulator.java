package gms.core.dataacquisition;

import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import java.util.Collection;
import java.util.Set;
import reactor.kafka.receiver.ReceiverOffset;

/**
 * Interface defining an accumulator of boolean acquired channel environment issue
 * updates (those that need to be inserted and those that need to be removed) as
 * well as {@link AcquiredChannelEnvironmentIssueAnalog} instances that need
 * to be inserted to the OSD.
 */
public interface ACEIAccumulator {

  /**
   * Accumulate the update resulting from the merging of a boolean
   * issue with those already received.
   * @param update
   */
  void addBooleanIssueUpdate(AceiBooleanRangeMap.Update update, ReceiverOffset offset);

  /**
   * Accumulate a collection of analog issues for insertion.
   * @param analogIssues
   */
  void addAnalogIssues(Collection<AcquiredChannelEnvironmentIssueAnalog> analogIssues,
      ReceiverOffset offset);

}
