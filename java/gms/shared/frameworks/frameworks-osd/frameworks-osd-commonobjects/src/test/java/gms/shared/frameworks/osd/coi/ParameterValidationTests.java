package gms.shared.frameworks.osd.coi;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ParameterValidation} utility operations.
 */
public class ParameterValidationTests {

  @Test
  public void testRequireTrue() {
    ParameterValidation.requireTrue(Predicate.isEqual("test"), "test", "valid test");
    ParameterValidation.requireTrue(String::equals, "test", "test", "valid test");
  }

  @Test
  public void testRequireTruePredicateExpectIllegalArgumentException() {
    final String exceptionMessage = "invalid test";
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> ParameterValidation
            .requireTrue(Predicate.isEqual("test"), "not test", exceptionMessage));
    assertTrue(exception.getMessage().contains(exceptionMessage));
  }

  @Test
  public void testRequireTrueBiPredicateExpectIllegalArgumentException() {
    final String exceptionMessage = "invalid test";
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> ParameterValidation
            .requireTrue(String::equals, "test", "not test", exceptionMessage));
    assertTrue(exception.getMessage().contains(exceptionMessage));
  }
}
