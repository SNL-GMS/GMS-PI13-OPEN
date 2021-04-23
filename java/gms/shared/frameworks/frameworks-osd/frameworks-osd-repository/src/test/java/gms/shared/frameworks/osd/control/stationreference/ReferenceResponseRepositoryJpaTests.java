package gms.shared.frameworks.osd.control.stationreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gms.shared.frameworks.coi.exceptions.DataExistsException;
import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceResponse;
import gms.shared.frameworks.osd.control.utils.TestFixtures;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ReferenceResponseRepositoryJpaTests {

  private static EntityManagerFactory entityManagerFactory;
  private static ReferenceResponseRepositoryJpa referenceResponseRepositoryJpa;

  @BeforeAll
  public static void setUp() {
    entityManagerFactory = CoiTestingEntityManagerFactory.createTesting();
    referenceResponseRepositoryJpa = new ReferenceResponseRepositoryJpa(entityManagerFactory);

    // Load some initial objects.
    referenceResponseRepositoryJpa.storeReferenceResponses(TestFixtures.ALL_REFERENCE_RESPONSES);
  }

  @Test
  public void testRetrieval() {
    List<String> channelNames = TestFixtures.ALL_REFERENCE_RESPONSES
        .stream()
        .map(ReferenceResponse::getChannelName)
        .collect(Collectors.toList());
    List<ReferenceResponse> responses =
        referenceResponseRepositoryJpa.retrieveReferenceResponses(channelNames);

    assertEquals(3, responses.size());
  }

  @Test
  public void storeExistingResponse() {
    // Storing a channel that already exists should throw an exception
    assertThrows(DataExistsException.class, () -> {
      referenceResponseRepositoryJpa.storeReferenceResponses(TestFixtures.ALL_REFERENCE_RESPONSES);
    });
  }
}
