package gms.shared.frameworks.osd.coi.util;

import gms.shared.frameworks.osd.coi.Units;
import javax.persistence.Converter;

@Converter
public class UnitsConverter extends EnumToStringConverter<Units> {
  public UnitsConverter() {
    super(Units.class);
  }
}
