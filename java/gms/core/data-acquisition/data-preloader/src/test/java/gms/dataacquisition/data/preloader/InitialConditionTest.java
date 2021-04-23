package gms.dataacquisition.data.preloader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class InitialConditionTest {

  @ParameterizedTest
  @EnumSource(InitialCondition.class)
  void testParse(InitialCondition expected) {
    assertEquals(expected, InitialCondition.parse(expected.toString()));
  }
}