package gms.shared.frameworks.osd.coi.event.repository.jpa.util;

import gms.shared.frameworks.osd.coi.event.RestraintType;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import javax.persistence.Converter;

@Converter
public class RestraintTypeConverter extends EnumToStringConverter<RestraintType> {
  public RestraintTypeConverter() {
    super(RestraintType.class);
  }
}
