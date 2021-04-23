package gms.shared.frameworks.soh.repository.performancemonitoring.transform;

import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.dto.soh.DurationSohMonitorValues;
import gms.shared.frameworks.osd.dto.soh.HistoricalSohMonitorValues;
import gms.shared.frameworks.osd.dto.soh.HistoricalStationSoh;
import gms.shared.frameworks.osd.dto.soh.PercentSohMonitorValues;
import gms.shared.frameworks.osd.dto.soh.SohMonitorValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StationSohObjectArrayTransformerTests {
  private List<Object[]> stations = new ArrayList<>();

  @BeforeEach
  public void populateStationDate(){
    //note: the values are in reverse order to validate they are not being sorted, only calcTimes should be sorted
    stations.add(new Object[]{ "MKAR", Timestamp.valueOf("2020-07-14 19:00:00.000000"), "MKAR.MK08.SHZ", SohMonitorType.LAG.getDbId(),
            654321, null, SohStatus.GOOD.getDbId()});
    stations.add(new Object[]{ "MKAR", Timestamp.valueOf("2020-07-14 19:00:00.000000"), "MKAR.MK08.SHZ", SohMonitorType.MISSING.getDbId(),
            null, 0.2f, SohStatus.GOOD.getDbId()});
    stations.add(new Object[]{ "MKAR", Timestamp.valueOf("2020-07-14 19:00:20.000000"), "MKAR.MK08.SHZ", SohMonitorType.LAG.getDbId(),
            123456, null, SohStatus.GOOD.getDbId()});
    stations.add(new Object[]{ "MKAR", Timestamp.valueOf("2020-07-14 19:00:20.000000"), "MKAR.MK08.SHZ", SohMonitorType.MISSING.getDbId(),
            null, 0.1f, SohStatus.GOOD.getDbId()});
  }
  @Test
  public void createHistoricalStationSohCalculationTimesTest() {

    HistoricalStationSoh historicalStationSoh =
            StationSohObjectArrayTransformer.createHistoricalStationSoh("MKAR", stations);

    assertTrue(historicalStationSoh.getCalculationTimes().length == 2,
            "Calculation times is the wrong length, should be 2 but was: " +
                    historicalStationSoh.getCalculationTimes().length);

    assertTrue(historicalStationSoh.getCalculationTimes()[0] == Timestamp.valueOf("2020-07-14 19:00:00.000000").getTime(),
            "Calculation Time not set with correct value");

    for(int i=0; i<historicalStationSoh.getCalculationTimes().length-1; i++){
      assertTrue(historicalStationSoh.getCalculationTimes()[i] < historicalStationSoh.getCalculationTimes()[i+1],
              "CalculationTimes are not sorted correctly");
    }
  }

  @Test
  public void validateHistoricalStationSohValueArrays() {

    HistoricalStationSoh historicalStationSoh =
            StationSohObjectArrayTransformer.createHistoricalStationSoh("MKAR", stations);

    for(HistoricalSohMonitorValues hmv : historicalStationSoh.getMonitorValues()){
      for(Map.Entry<SohMonitorType, SohMonitorValues> es : hmv.getValuesByType().entrySet()){
        assertTrue(es.getValue().size() == 2,
                "value array not correct size");
        if(es.getValue() instanceof DurationSohMonitorValues){
          DurationSohMonitorValues smv = (DurationSohMonitorValues)es.getValue();
          for(int i=0; i<smv.getValues().length-1; i++){
            assertTrue(smv.getValues()[i] > smv.getValues()[i+1],
                    "DurationSohMonitorValues are not sorted correctly");
          }
        }
        if(es.getValue() instanceof PercentSohMonitorValues){
          PercentSohMonitorValues smv = (PercentSohMonitorValues)es.getValue();
          for(int i=0; i<smv.getValues().length-1; i++){
            assertTrue(smv.getValues()[i] > smv.getValues()[i+1],
                    "PercentSohMonitorValues are not sorted correctly");
          }
        }
      }
    }
  }
}