package gms.shared.frameworks.osd.control.waveforms;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.LocalDate;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import com.datastax.driver.extras.codecs.arrays.DoubleArrayCodec;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.primitives.Doubles;
import gms.shared.frameworks.coi.exceptions.RepositoryException;
import gms.shared.frameworks.coi.exceptions.StorageUnavailableException;
import gms.shared.frameworks.osd.coi.waveforms.FkSpectra;
import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import gms.shared.frameworks.osd.coi.waveforms.repository.jpa.FkSpectraDao;
import gms.shared.frameworks.osd.coi.waveforms.repository.jpa.FkSpectrumDao;
import gms.shared.frameworks.osd.control.utils.CassandraConfig;
import gms.shared.frameworks.osd.control.utils.CassandraUtility;
import gms.shared.frameworks.osd.control.utils.WaveformOverlapResolver;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedSupplier;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TimeseriesRepositoryCassandra {

  // cassandra schema constants
  private static final String KEYSPACE = "gms_timeseries_data";
  static final String WAVEFORMS_TABLE = KEYSPACE + "." + "new_waveforms";
  static final String FKSPECTRA_TABLE = KEYSPACE + "." + "fk_spectra";

  private static final int DOUBLES_PER_BLOCK = 100_000;
  private static final long BILLION = 1_000_000_000L;

  private static final Logger logger = LoggerFactory.getLogger(TimeseriesRepositoryCassandra.class);

  private final Session session;
  private final PreparedStatement preparedWaveformsQuery;
  private final PreparedStatement preparedWaveformsInsert;
  private final PreparedStatement preparedFkQuery;
  private final PreparedStatement preparedFkInsert;

  private TimeseriesRepositoryCassandra(CassandraConfig cassandraConfig) {
    this.session = getWithRetry(cassandraConfig::getConnection,
        "Failed to initialize cassandra session, will try again");
    logger.debug("session:\nstate: {}\ncluster: {} ", session.getState(), session.getCluster());
    this.preparedWaveformsQuery = waveformQueryStatement(this.session);
    this.preparedWaveformsInsert = waveformInsertStatement(this.session);
    this.preparedFkQuery = fkQueryStatement(this.session);
    this.preparedFkInsert = fkInsertStatement(this.session);
  }

  public static TimeseriesRepositoryCassandra create(CassandraConfig cassandraConfig) {
    return new TimeseriesRepositoryCassandra(cassandraConfig);
  }

  public void close() {
    if (!session.isClosed()) {
      session.close();
    }
  }

  public Map<String, List<Waveform>> retrieveWaveformsByChannelAndTime(
      Collection<String> channelNames,
      Instant startTime,
      Instant endTime) {
    Validate.notNull(channelNames);
    Validate.notNull(startTime);
    Validate.notNull(endTime);

    logger.debug("Finding waveforms by channel name and time");

    return channelNames.stream().collect(Collectors.toMap(Function.identity(),
        c -> retrieveWaveformsByTime(c, startTime, endTime)));
  }

  /**
   * Retrieves the Waveforms associated with the provided time and channel
   *
   * @param channelName The name of the channel associated with the waveform
   * @param startTime The start time of the waveforms to retrieve
   * @param endTime The end time of the waveforms to retrieve
   * @return A List of Waveforms from the provided channel bounded by the provided start and end
   * times
   */
  public List<Waveform> retrieveWaveformsByTime(String channelName,
      Instant startTime,
      Instant endTime) {

    Objects.requireNonNull(channelName);
    Objects.requireNonNull(startTime);
    Objects.requireNonNull(endTime);
    Preconditions.checkState(startTime.isBefore(endTime),
        "Cannot retrieve waveforms when start time is after end time");

    return WaveformOverlapResolver.resolve(queryWaveforms(channelName, startTime, endTime).entrySet()
        .stream()
        .map(entry -> Map.entry(entry.getKey().trim(startTime, endTime), entry.getValue()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  /**
   * Populates the provided {@link FkSpectraDao} with the timeseries data
   *
   * @param fkSpectraDaos The {@link FkSpectraDao}s with the appropriate FkSpectrum data
   * @return =
   */
  public List<FkSpectra> populateFkSpectra(List<FkSpectraDao> fkSpectraDaos) {
    Objects.requireNonNull(fkSpectraDaos);

    Map<FkSpectrumDao, ResultSetFuture> callbacks = fkSpectraDaos.stream()
        .flatMap(fkSpectraDao -> fkSpectraDao.getValues().stream())
        .collect(Collectors.toMap(Functions.identity(), this::createSpectrumStatement));
    DoubleArrayCodec codec = new DoubleArrayCodec();
    while (!callbacks.isEmpty()) {
      for (Iterator<Map.Entry<FkSpectrumDao, ResultSetFuture>> fkIterator =
           callbacks.entrySet().iterator(); fkIterator.hasNext(); ) {
        Map.Entry<FkSpectrumDao, ResultSetFuture> callback = fkIterator.next();

        if (callback.getValue().isDone()) {
          try {
            callback.getValue().get().forEach(row -> {
              // Deserialize the power and fstat arrays from the ResultSetFuture
              callback.getKey().setPower(unflattenArray(
                  codec.deserialize(row.getBytes("power"),
                      ProtocolVersion.NEWEST_SUPPORTED),
                  row.getInt("samples_d1_size"),
                  row.getInt("samples_d2_size")));
              callback.getKey().setFstat(unflattenArray(
                  codec.deserialize(row.getBytes("fstat"),
                      ProtocolVersion.NEWEST_SUPPORTED),
                  row.getInt("samples_d1_size"),
                  row.getInt("samples_d2_size")));
            });
          } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
          }

          fkIterator.remove();
        }
      }
    }

    return fkSpectraDaos.stream()
        .map(FkSpectraDao::toCoi)
        .collect(Collectors.toList());
  }

  /**
   * Store the provided @{link Waveform} data under the provided channel name
   *
   * @param waveforms The @{link Waveform} data to store
   * @param channelName The channel name to store it under
   */
  public void storeWaveforms(List<Waveform> waveforms, String channelName) {
    Objects.requireNonNull(waveforms);
    Objects.requireNonNull(channelName);

    Collection<ResultSetFuture> futures = new ArrayList<>();
    for (Waveform waveform : waveforms) {
      List<Waveform> brokenDownWaveforms = breakIntoBlocks(waveform);

      for (Waveform brokenWaveform : brokenDownWaveforms) {
        LocalDate waveformDate =
            LocalDate.fromMillisSinceEpoch(brokenWaveform.getStartTime().toEpochMilli());
        List<Double> samples = Doubles.asList(brokenWaveform.getValues());

        logger.debug("store waveforms preparedWaveformInsert prepared Id{}, routingKey {}",
            preparedWaveformsInsert.getPreparedId(), preparedWaveformsInsert.getRoutingKey());

        // Bind the prepared query for inserting waveforms to the data for the current waveform
        BoundStatement statement = preparedWaveformsInsert.bind()
            .setString("c", channelName)
            .setDate("d", waveformDate)
            .setLong("st", CassandraUtility.toEpochNano(brokenWaveform.getStartTime()))
            .setLong("e", CassandraUtility.toEpochNano(brokenWaveform.getEndTime()))
            .setLong("sc", brokenWaveform.getSampleCount())
            .setDouble("sr", brokenWaveform.getSampleRate())
            .setList("s", samples);

        futures.add(session.executeAsync(statement));
      }
    }

    List<ResultSetFuture> futuresToRemove = new ArrayList<>();
    while (!futures.isEmpty()) {
      for (ResultSetFuture result : futures) {
        if (result.isDone()) {
          try {
            result.getUninterruptibly();
          } catch (NoHostAvailableException e) {
            throw new StorageUnavailableException(e);
          } catch (QueryExecutionException | QueryValidationException e) {
            throw new RepositoryException(e);
          }
          futuresToRemove.add(result);
        }
      }

      futures.removeAll(futuresToRemove);
    }
  }

  /**
   * Store the power and fstat arrays from the provided {@link FkSpectraDao}
   *
   * @param spectras the Collection of {@link FkSpectraDao} to store data for
   */
  public void storeFk(Collection<FkSpectraDao> spectras) {
    Validate.notEmpty(spectras);

    try {
      for (FkSpectraDao spectra : spectras) {
        DoubleArrayCodec codec = new DoubleArrayCodec();
        for (FkSpectrumDao spectrum : spectra.getValues()) {
          double[] flattenedPower = flattenArray(spectrum.getPower());
          double[] flattenedFstat = flattenArray(spectrum.getFstat());

          UUID storageId = UUID.randomUUID();
          spectrum.setSampleStorageId(storageId);

          // Bind the prepared FkSpectra insert query to the current data
          BoundStatement statement = preparedFkInsert.bind()
              .setUUID("id", storageId)
              .setBytes("pow", codec.serialize(flattenedPower, ProtocolVersion.NEWEST_SUPPORTED))
              .setBytes("fst", codec.serialize(flattenedFstat, ProtocolVersion.NEWEST_SUPPORTED))
              .setInt("d1", spectrum.getPower().length)
              .setInt("d2", spectrum.getPower()[0].length);
          session.execute(statement);
        }
      }
    } catch (NoHostAvailableException e) {
      throw new StorageUnavailableException(e);
    } catch (QueryExecutionException | QueryValidationException e) {
      throw new RepositoryException(e);
    }
  }

  private ResultSetFuture createSpectrumStatement(FkSpectrumDao fkSpectrumDao) {
    BoundStatement statement = preparedFkQuery.bind()
        .setUUID("id", fkSpectrumDao.getSampleStorageId());
    return session.executeAsync(statement);
  }

  private Map<Waveform, Instant> queryWaveforms(String channelName, Instant start, Instant end) {
    return handleCompletedCallbacks(createWaveformQuery(channelName, start, end), channelName);
  }

  private Table<String, Waveform, Instant> handleCompletedCallback(
      ResultSetFuture callback) {
    Table<String, Waveform, Instant> waveformsByStorageTime = HashBasedTable.create();
    try {
      for (Row row : callback.get()) {
        Instant start = CassandraUtility.fromEpochNano(row.getLong("start_epoch_nano"));
        double sampleRate = row.getDouble("sample_rate");
        String channelName = row.getString("channel_name");
        double[] values = row.getList("samples", Double.class).stream()
            .mapToDouble(d -> d)
            .toArray();

        // Build the initial waveform and add it to the collection that will be resolved
        Waveform waveform = Waveform.from(start, sampleRate, values);
        waveformsByStorageTime
            .put(channelName, waveform, row.getTimestamp("storage_time").toInstant());
      }
    } catch (InterruptedException | ExecutionException ex) {
      throw new RuntimeException(ex);
    }

    return waveformsByStorageTime;
  }

  private Map<Waveform, Instant> handleCompletedCallbacks(
      Collection<ResultSetFuture> callbacks, String channelName) {
    final Map<Waveform, Instant> waveformsByStorageTime = new HashMap<>();
    while (!callbacks.isEmpty()) {
      List<ResultSetFuture> callbacksToRemove = new ArrayList<>();
      for (ResultSetFuture callback : callbacks) {
        if (callback.isDone()) {
          callbacksToRemove.add(callback);

          waveformsByStorageTime.putAll(handleCompletedCallback(callback).row(channelName));
        }
      }

      callbacks.removeAll(callbacksToRemove);
    }
    return waveformsByStorageTime;
  }

  private List<ResultSetFuture> createWaveformQuery(String channelName, Instant startTime,
      Instant endTime) {
    List<ResultSetFuture> callbacks = new ArrayList<>();
    for (LocalDate date : CassandraUtility.getCassandraDays(startTime, endTime)) {
      BoundStatement statement = preparedWaveformsQuery.bind()
          .setString("c", channelName)
          .setDate("d", date)
          .setLong("s", CassandraUtility.toEpochNano(startTime))
          .setLong("e", CassandraUtility.toEpochNano(endTime));

      callbacks.add(session.executeAsync(statement));
    }

    return callbacks;
  }

  /**
   * 'Flatten' a 2d double array into a 1d double array to be stored into Cassandra
   *
   * @param array Array to be 'flattened'
   * @return A 'flattened' 1d double array
   */
  private static double[] flattenArray(double[][] array) {
    double[] newArray = new double[array.length * array[0].length];
    int index = 0;

    for (int i = 0; i < array.length; i++) {
      System.arraycopy(array[i], 0, newArray, index, array[i].length);
      index += array[i].length;
    }

    return newArray;
  }

  /**
   * 'Unflatten' a 1d double array read from Cassandra into a 2d double array
   *
   * @param array Array to be 'unflattened'
   * @param d1Size The size of the outer dimension of the resulting 2d array
   * @param d2Size The size of the inner dimension of the resulting 2d array
   * @return An 'unflattened' 2d double array
   */
  private static double[][] unflattenArray(double[] array, int d1Size, int d2Size) {
    double[][] newArray = new double[d1Size][d2Size];

    for (int i = 0; i < d1Size; i++) {
      for (int j = 0; j < d2Size; j++) {
        newArray[i][j] = array[j + i * d2Size];
      }
    }

    return newArray;
  }

  private static List<Waveform> breakIntoBlocks(Waveform waveform) {
    long nanosPerBlock = (long) ((DOUBLES_PER_BLOCK) / waveform.getSampleRate() * BILLION);
    //since start time is included, we add 1 less than block size to get the correct amount of
    // samples
    long blockEndNanos = (long) ((DOUBLES_PER_BLOCK - 1) / waveform.getSampleRate() * BILLION);

    Instant end;
    List<Waveform> blockWaveforms = new ArrayList<>();
    for (Instant start = waveform.getStartTime(); !start.isAfter(waveform.getEndTime());
         start = start.plusNanos(nanosPerBlock)) {

      //end time check required due to validation exceptions in Waveform.window
      end = start.plusNanos(blockEndNanos);
      if (end.isAfter(waveform.getEndTime())) {
        end = waveform.getEndTime();
      }

      blockWaveforms.add(waveform.window(start, end));
    }

    return blockWaveforms;
  }

  //TODO: Remove ALLOW FILTERING ONCE DB image is optimized

  /* We have to use start time as our inequality criteria for queries
     because that is the clustering column of our db. To achieve this we will manually have to:
     1) subtract the blocking size from the start time (leading to possible overfetching),
        but will guarantee that we grab the block containing our desired start time
     2) use the start time and our ending inequality. Because we always grab full blocks,
        this will give us all the blocks until the start time no longer matches the criteria
  */

  private static PreparedStatement waveformQueryStatement(Session sesh) {
    return getWithRetry(() -> sesh.prepare(
        "SELECT channel_name, start_epoch_nano, end_epoch_nano, sample_count, sample_rate, " +
            "samples, storage_time "
            + "FROM " + WAVEFORMS_TABLE
            + " WHERE channel_name = :c AND date = :d AND start_epoch_nano <= :e AND " +
            "end_epoch_nano >= :s"
            + " "
            + "ALLOW FILTERING")
            .setConsistencyLevel(ConsistencyLevel.QUORUM),
        "Failed to initialize prepared waveforms query, will try again");
  }

  private static PreparedStatement waveformInsertStatement(Session sesh) {
    return getWithRetry(() -> sesh.prepare(
        "INSERT INTO " + WAVEFORMS_TABLE
            + "(channel_name, date, start_epoch_nano, end_epoch_nano, sample_count, " +
            "sample_rate, " +
            "samples, storage_time) "
            + "VALUES (:c, :d, :st, :e, :sc, :sr, :s, toTimestamp(now()))")
            .setConsistencyLevel(ConsistencyLevel.QUORUM),
        "Failed to initialize prepared waveforms insert, will try again");
  }

  private static PreparedStatement fkQueryStatement(Session sesh) {
    return getWithRetry(() -> sesh.prepare(
        "SELECT id, power, fstat, samples_d1_size, samples_d2_size"
            + " FROM " + FKSPECTRA_TABLE
            + " WHERE id = :id"),
        "Failed to initialize prepared fk query, will try again");
  }

  private static PreparedStatement fkInsertStatement(Session sesh) {
    return getWithRetry(() -> sesh.prepare(
        "INSERT INTO " + FKSPECTRA_TABLE
            + " (id, power, fstat, samples_d1_size, samples_d2_size)"
            + " VALUES (:id, :pow, :fst, :d1, :d2)")
            .setConsistencyLevel(ConsistencyLevel.QUORUM),
        "Failed to initialize prepared fk insert, will try again");
  }

  private static <T> T getWithRetry(CheckedSupplier<T> f, String failureMsg) {
    final RetryPolicy<T> retryPolicy = new RetryPolicy<T>()
        .withBackoff(50, 1000, ChronoUnit.MILLIS)
        .withMaxAttempts(50)
        .handle(List.of(Exception.class))
        .onFailedAttempt(e -> logger.warn(failureMsg));
    return Failsafe.with(retryPolicy).get(f);
  }
}
