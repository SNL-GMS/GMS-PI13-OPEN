package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import com.google.common.collect.ImmutableMap;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementType;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementTypes;
import java.util.Map;

public enum FeatureMeasurementTypeDao {

  ARRIVAL_TIME(FeatureMeasurementTypes.ARRIVAL_TIME),
  EMERGENCE_ANGLE(FeatureMeasurementTypes.EMERGENCE_ANGLE),
  SOURCE_TO_RECEIVER_AZIMUTH(FeatureMeasurementTypes.SOURCE_TO_RECEIVER_AZIMUTH),
  RECEIVER_TO_SOURCE_AZIMUTH(FeatureMeasurementTypes.RECEIVER_TO_SOURCE_AZIMUTH),
  SLOWNESS(FeatureMeasurementTypes.SLOWNESS),
  SIGNAL_DURATION(FeatureMeasurementTypes.SIGNAL_DURATION),
  PHASE(FeatureMeasurementTypes.PHASE),
  AMPLITUDE_A5_OVER_2(FeatureMeasurementTypes.AMPLITUDE_A5_OVER_2),
  AMPLITUDE_ANL_OVER_2(FeatureMeasurementTypes.AMPLITUDE_ANL_OVER_2),
  AMPLITUDE_ALR_OVER_2(FeatureMeasurementTypes.AMPLITUDE_ALR_OVER_2),
  AMPLITUDE_A5_OVER_2_OR(FeatureMeasurementTypes.AMPLITUDE_A5_OVER_2_OR),
  AMPLITUDE_ANP_OVER_2(FeatureMeasurementTypes.AMPLITUDE_ANP_OVER_2),
  AMPLITUDE_FKSNR(FeatureMeasurementTypes.AMPLITUDE_FKSNR),
  AMPLITUDE_LRM0(FeatureMeasurementTypes.AMPTLIUDE_LRM0),
  AMPLITUDE_NOILRM0(FeatureMeasurementTypes.AMPLITUDE_NOI_LRM0),
  AMPLITUDE_RMSAMP(FeatureMeasurementTypes.AMPLITUDE_RMSAMP),
  AMPLITUDE_SBSNR(FeatureMeasurementTypes.AMPLITUDE_SBSNR),
  PERIOD(FeatureMeasurementTypes.PERIOD),
  RECTILINEARITY(FeatureMeasurementTypes.RECTILINEARITY),
  SNR(FeatureMeasurementTypes.SNR),
  FIRST_MOTION(FeatureMeasurementTypes.FIRST_MOTION),
  SOURCE_TO_RECEIVER_DISTANCE(FeatureMeasurementTypes.SOURCE_TO_RECEIVER_DISTANCE),
  MAGNITUDE_CORRECTION(FeatureMeasurementTypes.MAGNITUDE_CORRECTION);

  private static final Map<FeatureMeasurementType, FeatureMeasurementTypeDao> TYPES_BY_COI_TYPE;

  static {
    ImmutableMap.Builder<FeatureMeasurementType, FeatureMeasurementTypeDao> byTypeBuilder = ImmutableMap.builder();
    for (FeatureMeasurementTypeDao type : values()) {
      byTypeBuilder.put(type.getCoiType(), type);
    }

    TYPES_BY_COI_TYPE = byTypeBuilder.build();
  }

  private final FeatureMeasurementType coiType;

  FeatureMeasurementTypeDao(FeatureMeasurementType coiType) {
    this.coiType = coiType;
  }

  public FeatureMeasurementType getCoiType() {
    return coiType;
  }

  public static FeatureMeasurementTypeDao fromFeatureMeasurementType(FeatureMeasurementType type) {
    return TYPES_BY_COI_TYPE.get(type);
  }

}
