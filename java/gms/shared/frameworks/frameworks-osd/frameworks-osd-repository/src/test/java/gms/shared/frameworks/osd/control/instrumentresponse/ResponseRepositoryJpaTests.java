package gms.shared.frameworks.osd.control.instrumentresponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.coi.exceptions.DataExistsException;
import gms.shared.frameworks.osd.api.channel.ChannelRepositoryInterface;
import gms.shared.frameworks.osd.api.station.StationRepositoryInterface;
import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.signaldetection.FrequencyAmplitudePhase;
import gms.shared.frameworks.osd.coi.signaldetection.Response;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures;
import gms.shared.frameworks.osd.control.channel.ChannelRepositoryJpa;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
import gms.shared.frameworks.osd.control.utils.TestFixtures;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ResponseRepositoryJpaTests {

  private static final EntityManagerFactory entityManagerFactory
      = CoiTestingEntityManagerFactory.createTesting();
  private static final ResponseRepositoryJpa repo = new ResponseRepositoryJpa(entityManagerFactory);

  private static final Set<Response> responses = createResponses(TestFixtures.station.getChannels());
  private static final Response firstResponse = responses.iterator().next();


  @BeforeAll
  static void setUp() {
    final StationRepositoryInterface stationRepo = new StationRepositoryJpa(entityManagerFactory);
    stationRepo.storeStations(List.of(TestFixtures.station));
    repo.storeResponses(responses);
  }

  @AfterAll
  static void tearDown() {
    entityManagerFactory.close();
  }

  @Test
  void testStoreAlreadyInStorageThrowsDataExistsException() {
    final DataExistsException ex = assertThrows(DataExistsException.class,
        () -> repo.storeResponses(List.of(firstResponse)));
    assertEquals("Responses already exist for these channels: " + Set.of(firstResponse.getChannelName()),
        ex.getMessage());
  }

  @Test
  void testStoreMultipleWithSameChannelNameThrowsIllegalArgumentException() {
    final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> repo.storeResponses(List.of(firstResponse, firstResponse)));
    final String expectedMsg = "Responses have duplicate channels: " + Set.of(firstResponse.getChannelName());
    assertTrue(ex.getMessage().contains(expectedMsg),
        String.format("Expected <%s> but was <%s>", expectedMsg, ex.getMessage()));
  }

  @Test
  void testStoreChannelNotInStorageThrowsIllegalArgumentException() {
    final String badChanName = UUID.randomUUID().toString();
    final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> repo.storeResponses(List.of(createResponse(badChanName, true))));
    assertEquals("Responses refer to these channels not in storage: " + Set.of(badChanName),
        ex.getMessage());
  }

  @ParameterizedTest
  @MethodSource("responsesArgs")
  void testRetrieval(Collection<Response> responses) {
    final Map<String, Response> expected = responses.stream()
        .collect(Collectors.toMap(Response::getChannelName, Function.identity()));
    assertEquals(expected, repo.retrieveResponsesByChannels(chans(responses)));
  }

  @Test
  void testRetrievalAllNamesBogus() {
    assertEquals(new HashMap<String, Response>(),
        repo.retrieveResponsesByChannels(Set.of("fake", "fake-again")));
  }

  private static Stream<Arguments> responsesArgs() {
    return Stream.of(Arguments.of(responses),
        Arguments.of(List.of(firstResponse)));
  }

  private static Set<Response> createResponses(Collection<Channel> chans) {
    // alternate populating fap and not so both cases get represented
    boolean withFap = true;
    final Set<Response> responses = new HashSet<>(chans.size());
    for (Channel c : chans) {
      responses.add(createResponse(c.getName(), withFap));
      withFap = !withFap;
    }
    return responses;
  }

  private static Response createResponse(String channelName, boolean withFap) {
    final FrequencyAmplitudePhase fap = withFap ? SignalDetectionTestFixtures.fapResponse : null;
    return Response.from(channelName, SignalDetectionTestFixtures.calibration, fap);
  }

  private static Set<String> chans(Collection<Response> responses) {
    return responses.stream().map(Response::getChannelName).collect(Collectors.toSet());
  }

}
