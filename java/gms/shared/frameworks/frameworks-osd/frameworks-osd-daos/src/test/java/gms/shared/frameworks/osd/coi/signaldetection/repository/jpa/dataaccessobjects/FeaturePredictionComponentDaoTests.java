package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gms.shared.frameworks.osd.coi.DoubleValue;
import gms.shared.frameworks.osd.coi.Units;
import gms.shared.frameworks.osd.coi.event.FeaturePredictionComponent;
import gms.shared.frameworks.osd.coi.event.FeaturePredictionCorrectionType;
import gms.shared.frameworks.osd.coi.event.repository.jpa.FeaturePredictionComponentDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FeaturePredictionComponentDaoTests {

  private FeaturePredictionComponent predictionComponent = FeaturePredictionComponent.from(
      DoubleValue.from(1.0, 1.0, Units.DEGREES),
      true,
      FeaturePredictionCorrectionType.BASELINE_PREDICTION
  );

  @Test
  void testFromAndToCoi() {

    FeaturePredictionComponentDao featurePredictionComponentDao = FeaturePredictionComponentDao.from(this.predictionComponent);

    Assertions.assertEquals(this.predictionComponent, featurePredictionComponentDao.toCoi());
  }

  @Test
  void testFromNullFeaturePredictionComponent() {

    Throwable exception = Assertions
        .assertThrows(NullPointerException.class, () -> FeaturePredictionComponentDao.from(null));

    assertEquals("Cannot create FeaturePredictionComponentDao from null FeaturePredictionComponent",
        exception.getMessage());
  }
}
