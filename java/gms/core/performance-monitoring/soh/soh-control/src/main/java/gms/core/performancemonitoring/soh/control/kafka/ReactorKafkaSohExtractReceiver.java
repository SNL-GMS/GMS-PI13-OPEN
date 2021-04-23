package gms.core.performancemonitoring.soh.control.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.core.performancemonitoring.soh.control.kafka.KafkaSohExtractConsumerFactory.SohExtractKafkaConsumer;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrameMetadata;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.function.Tuples;

/**
 * Implementation of {@link SohExtractReceiver} that consumes {@link AcquiredStationSohExtract}
 * instances from a Kafka topic.
 */

public class ReactorKafkaSohExtractReceiver implements SohExtractReceiver {

  private static final Logger logger = LogManager.getLogger(ReactorKafkaSohExtractReceiver.class);

  private static final Level SOH_TIMING = Level.getLevel("SOH_TIMING");

  private static final boolean SOH_TIMING_ENABLED = logger.isEnabled(SOH_TIMING);

  private Duration cachingDuration;

  private Disposable extractFluxDisposable;

  private Disposable asseFluxDisposable;

  private final KafkaReceiver<String, String> asseKafkaReciever;

  //
  // Cache of extracts.
  //
  // Keys are the latest payload end time contained in the extract. It's a NavigableMap so
  // when iterated over, the earliest payload end times come first. This makes it more efficient
  // to prune out old extracts. Values are list of the extracts with those payload end times.
  //
  private final NavigableMap<Instant, List<AcquiredStationSohExtract>> extractCache = new TreeMap<>();

  private final Map<TopicPartition, ReceiverOffset> offsetMap = new HashMap<>();

  /**
   * Constructor
   *
   * @param bootstrapServers the bootstrap servers setting for connecting to Kafka. This must not be
   * null.
   * @param topic the topic from which to read extracts. This must not be null.
   * @param applicationId the application id, used to set the group id for consumption from the
   * Kafka topic. This must not be null.
   * @param initialCachingDuration the duration for which to cache {@link AcquiredStationSohExtract}s. This
   * must not be null.
   */
  public ReactorKafkaSohExtractReceiver(
      String bootstrapServers,
      String topic,
      String applicationId,
      Duration initialCachingDuration
  ) {

    this.cachingDuration = initialCachingDuration;

    Map<String, Object> properties = new HashMap<>();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class);
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class);
    properties.put(ConsumerConfig.CLIENT_ID_CONFIG, applicationId);
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, applicationId);
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

    ReceiverOptions<String, String> receiverOptions = ReceiverOptions.create(properties);

    asseKafkaReciever = KafkaReceiver.create(
        receiverOptions.subscription(Collections.singleton(topic))
            .addAssignListener(partitions -> logger.info("onPartitionsAssigned: {}", partitions))
            .addRevokeListener(partitions -> logger.info("onPartitionsRevoked: {}", partitions)));
  }

  /**
   * Construct using a supplied kafka receiver and initial cache duration. This is meant for testing,
   * so it is package-private.
   *
   * @param mockKafkaReceiver Kafka receiver to use. The name of this parameter is to emphasize this
   * constructors purpose for testing.
   * @param initialCachingDuration Initial cache duration.
   */
  ReactorKafkaSohExtractReceiver(
      KafkaReceiver<String, String> mockKafkaReceiver,
      Duration initialCachingDuration
  ) {
    this.cachingDuration = initialCachingDuration;
    this.asseKafkaReciever = mockKafkaReceiver;
  }

  /**
   * Begin receiving SohExtracts, by collecting them into a set eache processsing interval,
   * amd then applying sohExtractKafkaConsumer to that set.
   *
   * @param processingInterval how long to buffer extracts consumed before forwarding
   *   them to a subscriber. This must not be null.
   * @param sohExtractKafkaConsumer consumer which consumes the set of extracts.
   */
  public synchronized void receive(
      Duration processingInterval,
      SohExtractKafkaConsumer sohExtractKafkaConsumer,
      List<AcquiredStationSohExtract> cacheData
  ) {

    synchronized (extractCache) {
      cacheData.forEach(this::addToCache);
    }

    AtomicInteger extractCounter = new AtomicInteger();

    EmitterProcessor<List<AcquiredStationSohExtract>> discardedAsseEmitter = EmitterProcessor
        .create();

    var discardedAsseSink = discardedAsseEmitter.sink();

    var discardedAsseEmitterDisposable = discardedAsseEmitter.subscribe(
        acquiredStationSohExtracts -> {

          if (SOH_TIMING_ENABLED) {

            StringBuilder stationInfoStringBuilder = new StringBuilder();

            acquiredStationSohExtracts.stream()
                .flatMap(acquiredStationSohExtract ->
                    acquiredStationSohExtract.getAcquisitionMetadata().stream())
                .forEach(rawStationDataFrameMetadata ->
                    stationInfoStringBuilder.append(
                        "\n     Station " + rawStationDataFrameMetadata.getStationName()
                            + " RSDF discarded. Reception Time: " + rawStationDataFrameMetadata
                            .getReceptionTime()
                            + "; Payload End Time:  " + rawStationDataFrameMetadata
                            .getPayloadEndTime()
                            + "; Current Time: " + Instant.now()
                            + " \n"
                    )
                );

            logger.log(
                SOH_TIMING,
                "Some extracts were discarded: {} \n",
                stationInfoStringBuilder
            );
          }

        }
    );

    //
    // Only need to start receiving the raw extracts once.
    //
    if (extractFluxDisposable == null) {
      receiveAsse(extractCounter);
    }

    extractFluxDisposable = Flux.range(0, 2_000_000_000)
        //
        // Flux.range combined with delayElements will continue to emit at a constant rate,
        // even when things back up downstream. Flux.interval fires off all items that had
        // been backed up at once - we do not want that.
        //
        // TODO: Look into backpressure handling as a possibly better way to do this. As of this
        //  change, experiments with backpressure (onBackPressureDrop, etc) failed to yield
        //  useful results.
        //
        .delayElements(processingInterval)
        .parallel()
        .runOn(Schedulers.boundedElastic())
        .map(dummy ->
            {
              logger.info(
                  "{} new extracts were received for this processing interval",
                  extractCounter.getAndSet(0)
              );

              Set<AcquiredStationSohExtract> extractSet;

              synchronized (extractCache) {
                extractSet = filterStationSohExtractCache(
                    Instant.now().minus(cachingDuration),
                    extractCache,
                    discardedAsseSink
                );
              }

              return Tuples.of(
                  extractSet,
                  offsetMap.values()
              );
            }
        )
        .sequential()
        .doFinally(
            s -> discardedAsseEmitterDisposable.dispose()
        )
        .subscribe(sohExtractKafkaConsumer);
  }

  /**
   * Stop the flux that runs on the processing interval. Used to update configuraton.
   */
  public void stopProcessingInterval() {

    this.extractFluxDisposable.dispose();
  }

  /**
   * Stop both the raw ASSE flux and the processing interval flux. Used when we are completely
   * done with this receiver.
   */
  public void stop() {

    stopProcessingInterval();
    this.asseFluxDisposable.dispose();
  }

  /**
   * Answers whether we are receiving extracts.
   * @return whether we are receiving extracts.
   */
  public boolean isReceiving() {

    return !extractFluxDisposable.isDisposed()
        && !asseFluxDisposable.isDisposed();
  }

  /**
   * Get a snapshot of the cache. This is meant only for testing, so it is package-private.
   * @return The set of all AcquiredStationSohExtract objects that can be found in the cache.
   */
  Set<AcquiredStationSohExtract> getCachedExtractsSnapshot() {
    synchronized (extractCache) {
      return this.extractCache.values().stream()
          .flatMap(List::stream)
          .collect(Collectors.toSet());
    }
  }

  private void addToCache(AcquiredStationSohExtract extract) {
    Instant payloadEndTimeLimit = Instant.now().minus(cachingDuration);
    Optional<Instant> possibleEnd = lastPayloadEndTime(extract);
    if (possibleEnd.isPresent() && possibleEnd.get().isAfter(payloadEndTimeLimit)) {
      extractCache.computeIfAbsent(possibleEnd.get(),
          key -> new ArrayList<>()).add(extract);
    }
  }

  private Optional<Instant> lastPayloadEndTime(AcquiredStationSohExtract extract) {
    return extract.getAcquisitionMetadata().stream()
        .map(RawStationDataFrameMetadata::getPayloadEndTime)
        .max(Comparator.naturalOrder());
  }

  /**
   * Called to change the caching duration for {@link AcquiredStationSohExtract}s read from the
   * Kafka topic.
   *
   * @param cachingDuration the duration for which to cache {@link AcquiredStationSohExtract}s. This
   * must not be null.
   */
  public void setCachingDuration(Duration cachingDuration) {
    Validate.isTrue(cachingDuration != null && cachingDuration.toMillis() > 0L,
        "cachingDuration must be non-null and positive");

    this.cachingDuration = cachingDuration;
  }

  /**
   * Start receiving raw AcquiredStationSohExtracts, deserializing them, and caching them.
   */
  private void receiveAsse(AtomicInteger extractCounter) {

    Flux<ReceiverRecord<String, String>> kafkaFlux = asseKafkaReciever.receive();

    ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

    asseFluxDisposable = kafkaFlux
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(receiverRecord -> {

              AcquiredStationSohExtract acquiredStationSohExtract;

              try {
                acquiredStationSohExtract = objectMapper.readValue(
                    receiverRecord.value(),
                    AcquiredStationSohExtract.class
                );

                synchronized (extractCache) {
                  addToCache(acquiredStationSohExtract);
                }

                extractCounter.incrementAndGet();

              } catch (JsonProcessingException e) {
                logger.info(
                    "Error parsing JSON, continuing to next record"
                );

              }

              var receiverOffset = receiverRecord.receiverOffset();

              offsetMap.compute(receiverOffset.topicPartition(), (tp, ro) -> {
                if (ro == null || receiverOffset.offset() > ro.offset()) {
                  return receiverOffset;
                }
                return ro;
              });

            }
        );
  }

  /**
   * Filter out the extracts that are older then the given time.
   *
   * @param payloadEndTimeLimit the time before which we want no extracts
   * @param extractCache cach to filter on
   * @return set of extracts from the cache
   */
  private static Set<AcquiredStationSohExtract> filterStationSohExtractCache(
      Instant payloadEndTimeLimit,
      Map<Instant, List<AcquiredStationSohExtract>> extractCache,
      FluxSink<List<AcquiredStationSohExtract>> discardedAsseSink
  ) {
    // Use an iterator to remove extracts with too old payloadEndTimes. Since it's a
    // NavigableMap, they are in sorted order.
    Iterator<Instant> it = extractCache.keySet().iterator();
    while (it.hasNext()) {
      Instant payloadEndTime = it.next();
      if (payloadEndTime.isAfter(payloadEndTimeLimit)) {
        // All the rest will be good, so break.
        break;
      } else {
        discardedAsseSink.next(
            extractCache.get(payloadEndTime)
        );
        // Remove it using the iterator in order not to get a ConcurrentModificationException.
        it.remove();
      }
    }
    // Now return a Set containing all the surviving extracts.
    return extractCache.values()
        .stream()
        .flatMap(List::stream)
        .collect(Collectors.toSet());
  }

}
