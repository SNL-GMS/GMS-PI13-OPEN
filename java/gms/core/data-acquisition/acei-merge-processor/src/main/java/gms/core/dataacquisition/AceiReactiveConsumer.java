package gms.core.dataacquisition;

import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import java.util.function.Consumer;
import org.reactivestreams.Subscriber;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.util.function.Tuple2;

/**
 * A consumer of {@link gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue}
 * instances from some reactive source. Consumed issues should be forwarded to one of two
 * registered subscribers.
 */
public interface AceiReactiveConsumer {

  /**
   * Start consuming instances of
   * {@link gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue}
   * forwarding ones that are {@link AcquiredChannelEnvironmentIssueBoolean} instances
   * to the first subscriber and forwarding those that are
   * {@link AcquiredChannelEnvironmentIssueAnalog} instances to the second
   * subscriber
   * @param booleanConsumer the subscriber for the boolean issues. Must not be null.
   * @param analogConsumer the subscriber for the analog issues. Must not be null.
   */
  void consume(
      Consumer<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> booleanConsumer,
      Consumer<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> analogConsumer
  );

  /**
   * Called to stop consumption of messages and forwarding of them to the subscribers.
   */
  void stop();

}
