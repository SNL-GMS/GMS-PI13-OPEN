package gms.shared.frameworks.osd.coi.channel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ChannelInstrumentTypeTests {

  @Test
  void testUnknownLiteral() {
    assertEquals('-', ChannelInstrumentType.UNKNOWN.getCode());
  }

  @Test
  void testNoBlankCodes() {
    assertTrue(Arrays.stream(ChannelInstrumentType.values())
        .map(ChannelInstrumentType::getCode)
        .noneMatch(Character::isWhitespace));
  }

  @Test
  void testAllCodesUnique() {
    final long numUniqueCodes = Arrays.stream(ChannelInstrumentType.values())
        .map(ChannelInstrumentType::getCode)
        .distinct()
        .count();

    assertEquals(ChannelInstrumentType.values().length, numUniqueCodes);
  }
}