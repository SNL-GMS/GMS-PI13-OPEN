package gms.dataacquisition.stationreceiver.cd11.dataman;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11GapList;
import gms.dataacquisition.stationreceiver.cd11.dataman.logging.StructuredLoggingWrapper;
import gms.shared.utilities.kafka.KafkaConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.util.function.BiFunction;
import org.reactivestreams.Publisher;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

public class Cd11DataHandler {

  public static final String STATION_NAME_KEY = "station";
  private final StructuredLoggingWrapper logger = StructuredLoggingWrapper
      .create(LoggerFactory.getLogger(Cd11DataHandler.class));

  //TODO in the future we will want to make these values configurable
  private static final int GAP_EXPIRATION_IN_DAYS = -1; // Never expire.
  private static final long STORE_GAP_STATE_INTERVAL_MINUTES = 5;

  private final String stationName;
  private final DataFrameReceiverConfiguration receiverConfig;
  private final KafkaConfiguration kafkaConfiguration;

  private Cd11DataHandler(String stationName,
      DataFrameReceiverConfiguration receiverConfig,
      KafkaConfiguration kafkaConfiguration) {
    this.stationName = stationName;
    this.receiverConfig = receiverConfig;
    this.kafkaConfiguration = kafkaConfiguration;

    logger.addKeyValueArgument(STATION_NAME_KEY, stationName);
  }

  public static Cd11DataHandler create(String stationName,
      DataFrameReceiverConfiguration receiverConfig,
      KafkaConfiguration kafkaConfiguration) {
    return new Cd11DataHandler(stationName, receiverConfig, kafkaConfiguration);
  }

  public BiFunction<NettyInbound, NettyOutbound, Publisher<Void>> handlecd11Data(
      Cd11GapList cd11GapList, boolean processFrames) {

    if (GAP_EXPIRATION_IN_DAYS > 0) {
      removeExpiredGapsPeriodically(Duration.ofHours(1),
          cd11GapList)
          .onErrorContinue((error, o) ->
              logger.warn("Error encountered during expired gap removal", error))
          .subscribeOn(Schedulers.boundedElastic())
          .subscribe();
    }

    Duration persistDurationMinutes = Duration.ofMinutes(STORE_GAP_STATE_INTERVAL_MINUTES);
    persistGapsPeriodically(persistDurationMinutes, stationName, cd11GapList)
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorContinue((error, o) ->
            logger.warn("Error encountered during gap persistence", error))
        .subscribe();

    return (inbound, outbound) -> Cd11StationConnectionHandler
        .create(stationName, receiverConfig, kafkaConfiguration, cd11GapList)
        .handleFrames(inbound, outbound, processFrames)
        .then();
  }

  //copying the original functionality of dataman
  //would like to modify this so that it is scheduled to remove gaps based on the oldest gap
  private Flux<Object> removeExpiredGapsPeriodically(Duration duration,
      Cd11GapList gapList) {

    return Flux.interval(duration, duration)
        .flatMap(i -> {
          gapList.removeExpiredGaps(GAP_EXPIRATION_IN_DAYS);
          return Mono.empty();
        });
  }

  private Flux<Object> persistGapsPeriodically(Duration duration, String stationName,
      Cd11GapList cd11GapList) {

    return Flux.interval(duration, duration)
        .flatMap(i -> {
          try {
            Cd11GapListUtility.persistGapState(stationName,
                cd11GapList.getGapList());
          } catch (IOException e) {
            logger.error("Could not persist gaps for station {}", stationName, e);
          }
          return Mono.empty();
        });
  }

}
