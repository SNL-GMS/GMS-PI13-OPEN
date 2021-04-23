package gms.core.performancemonitoring.soh.control;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import com.google.common.util.concurrent.AtomicDouble;
import gms.shared.frameworks.configuration.Selector;
import gms.shared.frameworks.configuration.repository.FileConfigurationRepository;
import gms.shared.frameworks.configuration.repository.client.ConfigurationConsumerUtility;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
// Reactive-centered tests for StationSohControlConfiguration
//
@Disabled("Thread yield() calls cause intermittent failures in the pipeline")
class StationSohControlConfigurationReactiveTests {

  private static final Logger logger = LoggerFactory
      .getLogger(StationSohControlConfigurationReactiveTests.class);

  private static final int INITIAL_REFRESH_INTERVAL = 4;

  private static final int ADJUSTED_REFRESH_INTERVAL = 2;


  StationSohControlConfiguration stationSohControlConfiguration;

  ConfigurationConsumerUtility initWithMockedConsumerUtility(String basePath) {

    URL configBasePathUrl = Thread.currentThread().getContextClassLoader().getResource(basePath);
    Objects.requireNonNull(configBasePathUrl, "Null configBasePathUrl");
    Path configurationBasePath = new File(configBasePathUrl.getFile()).toPath();

    FileConfigurationRepository fileConfigurationRepository = FileConfigurationRepository
        .create(configurationBasePath);

    SohRepositoryInterface sohRepositoryInterface = getMockRepositoryInterface();

    ConfigurationConsumerUtility configurationConsumerUtility =
        getMockConsumerUtilityForMockSohControlDefinition(
            fileConfigurationRepository
        );

    stationSohControlConfiguration = StationSohControlConfiguration.create(
        configurationConsumerUtility,
        sohRepositoryInterface
    );

    return configurationConsumerUtility;
  }

  @Test
  void testReprocessingIntervalUpdate() {

    ConfigurationConsumerUtility configurationConsumerUtility =
        initWithMockedConsumerUtility("gms/core/performancemonitoring/soh/configuration-base");

    AtomicInteger count = new AtomicInteger(0);

    AtomicReference<Duration> reprocessingInterval = new AtomicReference<>(
        Duration.ofSeconds(INITIAL_REFRESH_INTERVAL)
    );

    //
    // Time when the lambda finishes. Initial set to now.
    // This is double to allow some leeway in comparing when the last call to the
    // lambda finished.
    //
    AtomicDouble lambdaFinishTime = new AtomicDouble((double) System.currentTimeMillis());
    AtomicBoolean justSwitched = new AtomicBoolean(false);
    AtomicReference<AssertionFailedError> assertionError = new AtomicReference<>();

    stationSohControlConfiguration.subscribeToInterval(
        configurationPair -> {
          try {
            Assertions.assertEquals(
                reprocessingInterval.get(),
                configurationPair.getSohControlDefinition().getReprocessingPeriod()
            );

            logger.info(
                "PERIOD: {}", configurationPair.getSohControlDefinition().getReprocessingPeriod()
            );

            //
            // This assertion will fail on the first iteration after the reprocessingInterval
            // has changed.
            //
            if (!justSwitched.get()) {
              Assertions.assertEquals(
                  (double) configurationPair.getSohControlDefinition().getReprocessingPeriod()
                      .getSeconds(),
                  (System.currentTimeMillis() - lambdaFinishTime.get()) / 1000.0,
                  //
                  // Be generous with the time tolerance because we are doing I/O.
                  //
                  10e-1
              );
            } else {
              justSwitched.set(false);
            }

            lambdaFinishTime.set(
                (double) System.currentTimeMillis()
            );

            count.incrementAndGet();
          } catch (AssertionFailedError e) {
            //
            // Signal there was an error so we can end this test. Otherwise, with out
            // a call to "onError*" on the Flux, which we dont have access to, Reactor
            // wont stop.
            //
            assertionError.set(e);
          }
        }
    );

    while (count.get() < 2) {
      if (assertionError.get() != null) {
        throw assertionError.get();
      }
      Thread.yield();
    }

    justSwitched.set(true);

    reprocessingInterval.set(Duration.ofSeconds(ADJUSTED_REFRESH_INTERVAL));

    //
    // Change the reprocessingPeriod while the Flux is still running.
    //
    Mockito.when(
        configurationConsumerUtility.resolve(eq("soh-control"), eq(List.of()))
    ).thenAnswer(
        invocation -> Map.of(
              "reprocessingPeriod",
              Duration.ofSeconds(ADJUSTED_REFRESH_INTERVAL)
          )
    );

    while (count.get() < 6) {
      if (assertionError.get() != null) {
        throw assertionError.get();
      }
      Thread.yield();
    }

    stationSohControlConfiguration.unsubscribe();
  }

  private SohRepositoryInterface getMockRepositoryInterface() {

    Channel mockChannelA = Mockito.mock(Channel.class);
    Mockito.when(mockChannelA.getName()).thenReturn("ChannelA");
    Mockito.when(mockChannelA.getStation()).thenReturn("StationA");
    Channel mockChannelB = Mockito.mock(Channel.class);
    Mockito.when(mockChannelB.getName()).thenReturn("ChannelB");
    Mockito.when(mockChannelB.getStation()).thenReturn("StationA");
    Channel mockChannelC = Mockito.mock(Channel.class);
    Mockito.when(mockChannelC.getName()).thenReturn("ChannelC");
    Mockito.when(mockChannelC.getStation()).thenReturn("StationA");

    Channel mockChannelD = Mockito.mock(Channel.class);
    Mockito.when(mockChannelD.getName()).thenReturn("ChannelD");
    Mockito.when(mockChannelD.getStation()).thenReturn("StationB");
    Channel mockChannelE = Mockito.mock(Channel.class);
    Mockito.when(mockChannelE.getName()).thenReturn("ChannelE");
    Mockito.when(mockChannelE.getStation()).thenReturn("StationB");

    Channel mockChannelF = Mockito.mock(Channel.class);
    Mockito.when(mockChannelF.getName()).thenReturn("ChannelF");
    Mockito.when(mockChannelF.getStation()).thenReturn("StationC");
    Channel mockChannelG = Mockito.mock(Channel.class);
    Mockito.when(mockChannelG.getName()).thenReturn("ChannelG");
    Mockito.when(mockChannelG.getStation()).thenReturn("StationC");

    Station mockStationA = Mockito.mock(Station.class);
    Station mockStationB = Mockito.mock(Station.class);
    Station mockStationC = Mockito.mock(Station.class);

    StationGroup mockStationGroupA = Mockito.mock(StationGroup.class);
    StationGroup mockStationGroupB = Mockito.mock(StationGroup.class);

    SohRepositoryInterface sohRepositoryInterface = Mockito.mock(SohRepositoryInterface.class);

    Mockito.when(
        mockStationGroupA.getName()
    ).thenReturn(
        "GroupA"
    );

    Mockito.when(
        mockStationGroupB.getName()
    ).thenReturn(
        "GroupB"
    );

    Mockito.when(mockStationA.getName())
        .thenReturn("StationA");

    TreeSet<Channel> stationAChannels = new TreeSet<>(Comparator.comparing(Channel::getName));
    stationAChannels.add(mockChannelA);
    stationAChannels.add(mockChannelB);
    stationAChannels.add(mockChannelC);
    Mockito.when(mockStationA.getChannels())
        .thenReturn(stationAChannels);

    Mockito.when(mockStationB.getName())
        .thenReturn("StationB");

    TreeSet<Channel> stationBChannels = new TreeSet<>(Comparator.comparing(Channel::getName));
    stationBChannels.add(mockChannelD);
    stationBChannels.add(mockChannelE);
    Mockito.when(mockStationB.getChannels()).thenReturn(stationBChannels);

    Mockito.when(mockStationC.getName())
        .thenReturn("StationC");

    TreeSet<Channel> stationCChannels = new TreeSet<>(Comparator.comparing(Channel::getName));
    stationCChannels.add(mockChannelF);
    stationCChannels.add(mockChannelG);
    Mockito.when(mockStationC.getChannels()).thenReturn(stationCChannels);

    TreeSet<Station> stationGroupAStations = new TreeSet<>(Comparator.comparing(Station::getName));
    stationGroupAStations.add(mockStationA);
    stationGroupAStations.add(mockStationB);

    Mockito.when(
        mockStationGroupA.getStations()
    ).thenReturn(
        stationGroupAStations
    );

    TreeSet<Station> stationGroupBStations = new TreeSet<>(Comparator.comparing(Station::getName));
    stationGroupBStations.add(mockStationC);
    Mockito.when(
        mockStationGroupB.getStations()
    ).thenReturn(
      stationGroupBStations
    );

    Mockito.when(
        sohRepositoryInterface.retrieveStationGroups(List.of("GroupA", "GroupB"))
    ).thenReturn(
        List.of(mockStationGroupA, mockStationGroupB)
    );


    return sohRepositoryInterface;
  }

  private ConfigurationConsumerUtility getMockConsumerUtilityForMockSohControlDefinition(
      FileConfigurationRepository fileConfigurationRepositoryForBase
  ) {

    ConfigurationConsumerUtility mockConfigurationConsumerUtility =
        Mockito.mock(ConfigurationConsumerUtility.class);

    ConfigurationConsumerUtility baseConfigurationConsumerUtility =
        ConfigurationConsumerUtility.builder(fileConfigurationRepositoryForBase)
            .configurationNamePrefixes(Set.of("soh-control"))
            .build();

    Mockito.when(
        mockConfigurationConsumerUtility.resolve(anyString(), anyList())
    ).thenAnswer(
        invocation -> {
          String configurationName = invocation.getArgument(0);
          List<Selector> selectors = invocation.getArgument(1);

          return baseConfigurationConsumerUtility.resolve(
              configurationName,
              selectors
          );
        }
    );

    Mockito.when(
        mockConfigurationConsumerUtility.resolve(
            anyString(), anyList(), any(Class.class)
        )
    ).thenAnswer(
        invocation -> {
          String configurationName = invocation.getArgument(0);
          List<Selector> selectors = invocation.getArgument(1);
          Class<?> clazz = invocation.getArgument(2);

          return baseConfigurationConsumerUtility.resolve(
              configurationName,
              selectors,
              clazz
          );
        }
    );

    Mockito.when(
        mockConfigurationConsumerUtility.resolve(eq("soh-control"), eq(List.of()))
    ).thenAnswer(
        invocation -> Map.of(
            "reprocessingPeriod",
            Duration.ofSeconds(INITIAL_REFRESH_INTERVAL)
        )
    );

    return mockConfigurationConsumerUtility;

  }
}
