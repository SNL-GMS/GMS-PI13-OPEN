package gms.shared.frameworks.osd.coi.channel;

import static gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures.referenceChannel;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import org.junit.jupiter.api.Test;

class ReferenceChannelTests {

  @Test
  void testSerialization() throws Exception {
    TestUtilities.testSerialization(referenceChannel, ReferenceChannel.class);
  }

  @Test
  void testEmptyNameThrowsException() {
    Exception exception = assertThrows(IllegalArgumentException.class,
        () -> referenceChannel.toBuilder().setName("").build());
    assertEquals("name should not be an empty field", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class,
        () -> referenceChannel.toBuilder().setName(" ").build());
    assertEquals("name should not be an empty field", exception.getMessage());
  }

  @Test
  void testOrientationCodeIsWhitespaceThrowsException() {
    final Exception exception = assertThrows(IllegalArgumentException.class,
        () -> referenceChannel.toBuilder()
            .setOrientationType(ChannelOrientationType.UNKNOWN)
            .setOrientationCode(' ').build());

    assertEquals("orientationCode cannot be whitespace", exception.getMessage());
  }

  @Test
  void testOrientationTypeCodeDoesNotMatchOrientationCodeThrowsException() {
    final Exception exception = assertThrows(IllegalArgumentException.class,
        () -> referenceChannel.toBuilder()
            .setOrientationType(ChannelOrientationType.VERTICAL)
            .setOrientationCode('N').build());

    assertEquals(
        "orientationType.code must match orientationCode when orientationType is not 'UNKNOWN'",
        exception.getMessage());
  }

  @Test
  void testOrientationTypeUnknownCodeDoesNotNeedToMatchOrientationCode() {
    assertDoesNotThrow(
        () -> referenceChannel.toBuilder()
            .setOrientationType(ChannelOrientationType.UNKNOWN)
            .setOrientationCode('U').build()
    );
  }
}