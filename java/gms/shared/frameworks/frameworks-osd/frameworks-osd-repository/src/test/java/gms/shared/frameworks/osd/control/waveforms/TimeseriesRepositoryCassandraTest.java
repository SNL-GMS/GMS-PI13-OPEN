package gms.shared.frameworks.osd.control.waveforms;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.extras.codecs.arrays.DoubleArrayCodec;
import gms.shared.frameworks.osd.coi.waveforms.FkSpectra;
import gms.shared.frameworks.osd.coi.waveforms.FkSpectrum;
import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import gms.shared.frameworks.osd.coi.waveforms.repository.jpa.FkSpectraDao;
import gms.shared.frameworks.osd.coi.waveforms.repository.jpa.FkSpectrumDao;
import gms.shared.frameworks.osd.control.utils.CassandraConfig;
import gms.shared.frameworks.osd.control.utils.CassandraUtility;
import gms.shared.frameworks.osd.control.utils.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TimeseriesRepositoryCassandraTest {

  private TimeseriesRepositoryCassandra timeseriesRepository;
  private CassandraConfig config;

  @BeforeEach
  void setUp() {
    // TODO Get this from CassandraConfigFactory instead (requires system config / etcd)
    config = CassandraConfig.from("localhost", 9042,
        "GMS", "gms", "cassandra-is-not-provided");
    timeseriesRepository = TimeseriesRepositoryCassandra.create(config);
    assertNotNull(timeseriesRepository);
  }

  @AfterEach
  void tearDown() {
    Session session = config.getConnection();
    session.execute("TRUNCATE " + TimeseriesRepositoryCassandra.WAVEFORMS_TABLE);
    session.execute("TRUNCATE " + TimeseriesRepositoryCassandra.FKSPECTRA_TABLE);
    session.close();

    timeseriesRepository.close();
  }

  @Test
  void testCreateNull() {
    assertThrows(NullPointerException.class, () -> TimeseriesRepositoryCassandra.create(null));
  }

  @ParameterizedTest
  @MethodSource("createStoreWaveformsArguments")
  void testStoreWaveformArgumentValidation(List<Waveform> waveforms, String channelName) {
    assertThrows(NullPointerException.class,
        () -> timeseriesRepository.storeWaveforms(waveforms, channelName));
  }

  static Stream<Arguments> createStoreWaveformsArguments() {
    return Stream.of(
        arguments(null, "test"),
        arguments(List.of(TestFixtures.waveform1), null)
    );
  }

  @Test
  void testStoreWaveforms() {
    String channelName = "testChannel";
    List<Waveform> waveformsToStore = List.of(TestFixtures.waveform1, TestFixtures.waveform2);
    assertDoesNotThrow(() -> timeseriesRepository.storeWaveforms(waveformsToStore, channelName));

    Session session = config.getConnection();

    PreparedStatement waveformRetrieval = session.prepare("SELECT channel_name, start_epoch_nano," +
        " end_epoch_nano, sample_count, sample_rate, samples, storage_time " +
        "FROM " + TimeseriesRepositoryCassandra.WAVEFORMS_TABLE +
        " WHERE channel_name = :c ALLOW FILTERING");

    BoundStatement statement = waveformRetrieval.bind()
        .setString("c", channelName);
    ResultSet results = session.execute(statement);

    List<Waveform> retrievedWaveforms = new ArrayList<>();
    for (Row row : results) {
      Instant start = CassandraUtility.fromEpochNano(row.getLong("start_epoch_nano"));
      double rate = row.getDouble("sample_rate");

      double[] values = row.getList("samples", Double.class).stream()
          .mapToDouble(d -> d)
          .toArray();

      retrievedWaveforms.add(Waveform.from(start, rate, values));
    }

    session.close();

    assertEquals(waveformsToStore.size(), retrievedWaveforms.size());
    assertTrue(retrievedWaveforms.containsAll(waveformsToStore));
  }

  @Test
  void testStoreFkArgumentValidation() {
    assertThrows(NullPointerException.class, () -> timeseriesRepository.storeFk(null));
  }

  @Test
  void testStoreFk() {
    FkSpectra spectra = TestFixtures.buildFkSpectra();
    List<FkSpectraDao> spectras = List.of(FkSpectraDao.fromCoi(spectra));
    spectras.get(0).getTimeSeries().setId(UUID.randomUUID());
    assertDoesNotThrow(() -> timeseriesRepository.storeFk(spectras));

    spectras.stream()
        .forEach(obj ->
            obj.getValues().stream()
                .forEach(spectrum -> assertNotNull(spectrum.getSampleStorageId())));

    Session session = config.getConnection();

    PreparedStatement fkRetrieval = session.prepare(
        "SELECT id, power, fstat, samples_d1_size, samples_d2_size"
            + " FROM " + TimeseriesRepositoryCassandra.FKSPECTRA_TABLE
            + " WHERE id = :id");

    for(FkSpectraDao spectraDao : spectras) {
      for (int i = 0; i < spectraDao.getValues().size(); i++) {
        FkSpectrumDao spectrumDao = spectraDao.getValues().get(i);
        BoundStatement statement = fkRetrieval.bind()
            .setUUID("id", spectrumDao.getSampleStorageId());

        ResultSet results = session.execute(statement);
        int rowCount = 0;
        for (Row row : results) {
          int rows = row.getInt("samples_d1_size");
          int columns = row.getInt("samples_d2_size");
          DoubleArrayCodec codec = new DoubleArrayCodec();
          double[][] actualPower = unflattenArray(codec.deserialize(row.getBytes("power"),
              ProtocolVersion.NEWEST_SUPPORTED), rows, columns);
          double[][] actualFstat = unflattenArray(codec.deserialize(row.getBytes("fstat"),
              ProtocolVersion.NEWEST_SUPPORTED), rows, columns);

          FkSpectrum spectrum = spectra.getValues().get(i);

          double[][] expectedPower = spectrum.getPowerMutable();
          double[][] expectedFstat = spectrum.getFstatMutable();
          assertEquals(expectedPower.length, actualPower.length);
          assertEquals(expectedFstat.length, actualFstat.length);
          for (int j = 0; j < rows; j++) {
            assertArrayEquals(expectedPower[j], actualPower[j]);
            assertArrayEquals(expectedFstat[j], actualFstat[j]);
          }

          rowCount++;
        }

        assertEquals(1, rowCount);
      }
    }
    session.close();
  }

  @ParameterizedTest
  @MethodSource("getRetrieveWaveformsArguments")
  void testRetrieveWaveformsValidation(String name, Instant startTime, Instant endTime, Class<?
      extends Exception> exceptionType) {
    assertThrows(exceptionType,
        () -> timeseriesRepository.retrieveWaveformsByTime(name, startTime, endTime));
  }

  static Stream<Arguments> getRetrieveWaveformsArguments() {
    return Stream.of(
        arguments(null, Instant.EPOCH, Instant.EPOCH.plusSeconds(2), NullPointerException.class),
        arguments("Test Channel", null, Instant.EPOCH.plusSeconds(2), NullPointerException.class),
        arguments("Test Channel", Instant.EPOCH, null, NullPointerException.class),
        arguments("Test Channel", Instant.EPOCH.plusSeconds(2), Instant.EPOCH,
            IllegalStateException.class)
    );
  }

  @Test
  void testRetrieveWaveformsByTime() {
    String channelName = "testChannel";
    List<Waveform> waveformsToStore = List.of(TestFixtures.waveform1);
    timeseriesRepository.storeWaveforms(waveformsToStore, channelName);

    List<Waveform> retrievedWaveforms = timeseriesRepository.retrieveWaveformsByTime(channelName,
        TestFixtures.waveform1.getStartTime(), TestFixtures.waveform1.getEndTime());

    assertEquals(1, retrievedWaveforms.size());
    assertEquals(TestFixtures.waveform1, retrievedWaveforms.get(0));
  }

  @Test
  void testRetrieveWaveformRetrievalSmallerInterval() {
    String channelName = "testChannel";
    Waveform waveform = TestFixtures.buildLongWaveform(Instant.EPOCH.plusSeconds(2), 60, 20);

    timeseriesRepository.storeWaveforms(List.of(waveform), channelName);

    Instant queryStart = Instant.EPOCH.plusSeconds(5);
    Instant queryEnd = Instant.EPOCH.plusSeconds(20);

    List<Waveform> retrievedWaveforms = timeseriesRepository.retrieveWaveformsByTime(channelName,
        queryStart, queryEnd);

    assertEquals(1, retrievedWaveforms.size());
    Waveform expected = waveform.trim(queryStart, queryEnd);
    Waveform actual = retrievedWaveforms.get(0);

    assertEquals(expected, actual);
  }

  @Test
  void testPopulateFkSpectraValidation() {
    assertThrows(NullPointerException.class, () -> timeseriesRepository.populateFkSpectra(null));
  }

  @Test
  void testPopulateFkSpectra() {
    FkSpectra spectra = TestFixtures.buildFkSpectra();
    FkSpectraDao spectraDao = FkSpectraDao.fromCoi(spectra);
    spectraDao.getTimeSeries().setId(UUID.randomUUID());
    timeseriesRepository.storeFk(List.of(spectraDao));

    for (FkSpectrumDao spectrumDao : spectraDao.getValues()) {
      spectrumDao.setPower(null);
      spectrumDao.setFstat(null);
    }

    List<FkSpectra> actualSpectra = timeseriesRepository.populateFkSpectra(List.of(spectraDao));
    assertEquals(1, actualSpectra.size());
    assertEquals(spectra, actualSpectra.get(0));
  }

  private double[][] unflattenArray(double[] data, int rows, int columns) {
    double[][] unflattened = new double[rows][columns];

    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < columns; j++) {
        unflattened[i][j] = data[j + i * columns];
      }
    }

    return unflattened;
  }

}
