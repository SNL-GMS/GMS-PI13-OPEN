package gms.shared.frameworks.osd.coi.event.repository.jpa.util;

import gms.shared.frameworks.osd.coi.event.ScalingFactorType;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import javax.persistence.Converter;

@Converter
public class ScalingFactorTypeConverter extends EnumToStringConverter<ScalingFactorType> {
  public ScalingFactorTypeConverter() {
    super(ScalingFactorType.class);
  }
}
