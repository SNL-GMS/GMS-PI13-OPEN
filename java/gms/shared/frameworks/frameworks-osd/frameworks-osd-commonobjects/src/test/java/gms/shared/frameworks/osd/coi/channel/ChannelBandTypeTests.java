package gms.shared.frameworks.osd.coi.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ChannelBandTypeTests {

  @Test
  void testUnknownLiteral() {
    assertEquals('-', ChannelBandType.UNKNOWN.getCode());
  }

  @Test
  void testNoBlankCodes() {
    assertTrue(Arrays.stream(ChannelBandType.values())
        .map(ChannelBandType::getCode)
        .noneMatch(Character::isWhitespace));
  }

  @Test
  void testAllCodesUnique() {
    final long numUniqueCodes = Arrays.stream(ChannelBandType.values())
        .map(ChannelBandType::getCode)
        .distinct()
        .count();

    assertEquals(ChannelBandType.values().length, numUniqueCodes);
  }
}