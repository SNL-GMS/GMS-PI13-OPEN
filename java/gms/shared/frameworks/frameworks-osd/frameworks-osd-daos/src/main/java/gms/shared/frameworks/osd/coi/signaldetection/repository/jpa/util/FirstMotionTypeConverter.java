package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util;

import gms.shared.frameworks.osd.coi.signaldetection.FirstMotionType;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import javax.persistence.Converter;

@Converter
public class FirstMotionTypeConverter extends EnumToStringConverter<FirstMotionType> {

  public FirstMotionTypeConverter() {
    super(FirstMotionType.class);
  }
}
