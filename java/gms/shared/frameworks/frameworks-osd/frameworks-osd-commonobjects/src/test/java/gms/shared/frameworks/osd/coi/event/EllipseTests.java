package gms.shared.frameworks.osd.coi.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import org.junit.jupiter.api.Test;

public class EllipseTests {

  @Test
  public void testFrom() {
    //kWeight = 0.0 is an acceptable value here
    //kWeight doesn't matter when scalingFactorType is set to CONFIDENCE
    //confidenceLevel does
    Ellipse ellipse = Ellipse.from(
        EventTestFixtures.scalingFactorType, EventTestFixtures.K_WEIGHT,
        EventTestFixtures.CONFIDENCE_LEVEL,
        EventTestFixtures.MAJOR_AXIS_LENGTH, EventTestFixtures.MAJOR_AXIS_TREND,
        EventTestFixtures.MINOR_AXIS_LENGTH, EventTestFixtures.MINOR_AXIS_TREND,
        EventTestFixtures.DEPTH_UNCERTAINTY, EventTestFixtures.timeUncertainty);
    assertEquals(EventTestFixtures.scalingFactorType, ellipse.getScalingFactorType());
    assertEquals(EventTestFixtures.timeUncertainty, ellipse.getTimeUncertainty());
    final double tolerance = 0.0000000001;
    assertEquals(EventTestFixtures.K_WEIGHT, ellipse.getkWeight(), tolerance);
    assertEquals(EventTestFixtures.CONFIDENCE_LEVEL, ellipse.getConfidenceLevel(), tolerance);
    assertEquals(EventTestFixtures.MAJOR_AXIS_LENGTH, ellipse.getMajorAxisLength(), tolerance);
    assertEquals(EventTestFixtures.MAJOR_AXIS_TREND, ellipse.getMajorAxisTrend(), tolerance);
    assertEquals(EventTestFixtures.MINOR_AXIS_LENGTH, ellipse.getMinorAxisLength(), tolerance);
    assertEquals(EventTestFixtures.MINOR_AXIS_TREND, ellipse.getMinorAxisTrend(), tolerance);
    assertEquals(EventTestFixtures.DEPTH_UNCERTAINTY, ellipse.getDepthUncertainty(), tolerance);
  }

  @Test
  public void testBadConfidenceLevelWithConfidenceScalingFactor() {
    assertThrows(IllegalArgumentException.class,
        () -> Ellipse.from(EventTestFixtures.scalingFactorType, EventTestFixtures.K_WEIGHT, 0.0,
            EventTestFixtures.MAJOR_AXIS_LENGTH, EventTestFixtures.MAJOR_AXIS_TREND,
            EventTestFixtures.MINOR_AXIS_LENGTH, EventTestFixtures.MINOR_AXIS_TREND,
            EventTestFixtures.DEPTH_UNCERTAINTY, EventTestFixtures.timeUncertainty));
  }

  @Test
  public void testNonInfiniteKWeightWithCoverageScalingFactor() {
    assertThrows(IllegalArgumentException.class, () -> Ellipse.from(EventTestFixtures.scalingFactorType2, EventTestFixtures.K_WEIGHT,
        EventTestFixtures.CONFIDENCE_LEVEL,
        EventTestFixtures.MAJOR_AXIS_LENGTH, EventTestFixtures.MAJOR_AXIS_TREND,
        EventTestFixtures.MINOR_AXIS_LENGTH, EventTestFixtures.MINOR_AXIS_TREND,
        EventTestFixtures.DEPTH_UNCERTAINTY, EventTestFixtures.timeUncertainty));
  }

  @Test
  public void testLowKWeightWithKWeightedScalingFactor() {
    assertThrows(IllegalArgumentException.class, () -> Ellipse.from(EventTestFixtures.scalingFactorType2, EventTestFixtures.K_WEIGHT,
        EventTestFixtures.CONFIDENCE_LEVEL,
        EventTestFixtures.MAJOR_AXIS_LENGTH, EventTestFixtures.MAJOR_AXIS_TREND,
        EventTestFixtures.MINOR_AXIS_LENGTH, EventTestFixtures.MINOR_AXIS_TREND,
        EventTestFixtures.DEPTH_UNCERTAINTY, EventTestFixtures.timeUncertainty));
  }

  public void testSerialization() throws Exception {
    TestUtilities.testSerialization(EventTestFixtures.ellipse, Ellipse.class);
  }
}
