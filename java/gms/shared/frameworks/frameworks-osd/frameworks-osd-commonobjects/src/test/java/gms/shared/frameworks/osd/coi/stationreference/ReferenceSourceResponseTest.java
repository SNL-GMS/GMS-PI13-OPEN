package gms.shared.frameworks.osd.coi.stationreference;


import gms.shared.frameworks.osd.coi.util.TestUtilities;
import org.junit.jupiter.api.Test;

public class ReferenceSourceResponseTest {

  /**
   * Tests that the ReferenceSourceResponse object can be serialized and deserialized with the COI
   * object mapper.
   */
  @Test
  public void testSerialization() throws Exception {
    TestUtilities.testSerialization(StationReferenceTestFixtures.REFERENCE_SOURCE_RESPONSE, ReferenceSourceResponse.class);
  }

}

