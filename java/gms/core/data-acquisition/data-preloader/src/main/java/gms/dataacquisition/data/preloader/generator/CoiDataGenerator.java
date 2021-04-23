package gms.dataacquisition.data.preloader.generator;

import com.google.common.collect.ImmutableMap;
import gms.dataacquisition.data.preloader.GenerationSpec;
import gms.dataacquisition.data.preloader.InitialCondition;
import gms.shared.frameworks.injector.FluxFactory;
import gms.shared.frameworks.injector.Modifier;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public abstract class CoiDataGenerator<T, M extends Modifier<?>> implements Runnable {

  protected static final int INITIAL_DELAY = 0;
  public static final int MAX_ATTEMPTS = 10;
  private static final Logger logger = LoggerFactory.getLogger(CoiDataGenerator.class);

  protected final GenerationSpec generationSpec;
  protected final Duration generationFrequency;
  protected final Duration generationDuration;
  protected Instant seedTime;
  protected final SohRepositoryInterface sohRepository;
  protected final ImmutableMap<InitialCondition, String> initialConditions;
  protected final int batchSize;
  private List<StationGroup> stationGroups = null;

  public CoiDataGenerator(GenerationSpec generationSpec, SohRepositoryInterface sohRepository) {
    this.sohRepository = sohRepository;
    this.generationSpec = generationSpec;
    this.initialConditions = generationSpec.getInitialConditions();
    this.generationFrequency = generationSpec.getSampleDuration();
    this.generationDuration = generationSpec.getDuration();
    this.batchSize = generationSpec.getBatchSize();
    this.seedTime = getStartTime();
  }

  protected List<StationGroup> getStationGroups() {
    if (this.stationGroups == null) {
      this.stationGroups = sohRepository.retrieveStationGroups(
          Arrays.asList(initialConditions.get(InitialCondition.STATION_GROUPS).split(",")));
    }
    return this.stationGroups;
  }

  protected Stream<StationGroup> stationGroups() {
    return getStationGroups().stream();
  }

  protected Stream<Station> stations() {
    return stationGroups().flatMap(StationGroup::stations).distinct();
  }

  protected Stream<Channel> channels() {
    return stations().flatMap(Station::channels);
  }

  private Instant getStartTime() {
    return generationSpec.getStartTime();
  }

  public void run() {
    logger.info("~~~~~~STARTING DATA LOAD FOR: {}~~~~~~", generationSpec.getType());

    Flux.fromIterable(this.getSeedNames())
        .parallel()
        .runOn(Schedulers.boundedElastic())
        .map(seed -> {
              logger.info("~~~~~~~~~STARTING DATA LOAD FOR: {} FOR {}~~~~~~~~~",
                  generationSpec.getType(), seed);
              return FluxFactory.create(
                  getBatchCount(),
                  INITIAL_DELAY,
                  generationFrequency,
                  batchSize,
                  () -> this.runSeedGenerator(seed),
                  getModifier(generationFrequency),
                  this::runRecordConsumer,
                  e -> logger.error(e.getMessage(), e))
                  .doOnComplete(
                      () -> logger.info("~~~~~~~~~FINISHED DATA LOAD FOR: {} FOR {}~~~~~~~~~",
                          generationSpec.getType(), seed));
            }
        )
        .doOnNext(Flux::blockLast)
        .sequential()
        .blockLast();

    logger.info("~~~~~~FINISHED DATA LOAD FOR: {}~~~~~~", generationSpec.getType());
  }

  private Optional<Integer> getBatchCount() {
    return Optional.of((int) Math
        .ceil(((double) generationDuration.toNanos()) / generationFrequency.toNanos() / batchSize));
  }

  protected abstract Collection<String> getSeedNames();

  protected T runSeedGenerator(String seedName) {
    logger.debug("make a seed");
    final var seed = this.generateSeed(seedName);
    logger.debug("made a seed");
    return seed;
  }

  protected abstract T generateSeed(String seedName);

  protected abstract M getModifier(Duration generationFrequency);

  protected void runRecordConsumer(Iterable<T> records) {
    try {
      logger.debug("run record consumer");
      this.tryConsume(records);
      logger.debug("records consumed");
    } catch (Exception e) {
      final var error = new GmsPreloaderException("Failed to consume records", e);
      logger.error(error.getMessage(), error);
      throw error;
    }
  }

  private void tryConsume(Iterable<T> records) {
    final RetryPolicy<T> retryPolicy = new RetryPolicy<T>()
        .withBackoff(100, 3000, ChronoUnit.MILLIS)
        .withMaxAttempts(MAX_ATTEMPTS)
        .handle(List.of(ExecutionException.class, IllegalStateException.class,
            InterruptedException.class, PSQLException.class))
        .onFailedAttempt(e -> logger.warn("Unable to consume records, retrying: {}", e));
    Failsafe.with(retryPolicy).run(() -> this.consumeRecords(records));
  }

  protected abstract void consumeRecords(Iterable<T> records);

  protected <D> Set<D> convertToSet(Iterable<D> records) {
    final var data = new HashSet<D>();
    records.iterator().forEachRemaining(data::add);
    return data;
  }

}
