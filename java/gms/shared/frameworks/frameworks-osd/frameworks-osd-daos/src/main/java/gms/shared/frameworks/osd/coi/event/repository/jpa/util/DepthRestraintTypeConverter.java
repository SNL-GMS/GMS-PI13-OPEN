package gms.shared.frameworks.osd.coi.event.repository.jpa.util;


import gms.shared.frameworks.osd.coi.event.DepthRestraintType;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;

public class DepthRestraintTypeConverter extends EnumToStringConverter<DepthRestraintType> {
  public DepthRestraintTypeConverter() {
    super(DepthRestraintType.class);
  }
}
