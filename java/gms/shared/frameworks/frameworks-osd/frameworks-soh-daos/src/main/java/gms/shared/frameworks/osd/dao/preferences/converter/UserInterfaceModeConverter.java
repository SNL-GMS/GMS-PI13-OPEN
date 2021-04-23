package gms.shared.frameworks.osd.dao.preferences.converter;

import gms.shared.frameworks.osd.coi.preferences.UserInterfaceMode;
import gms.shared.frameworks.osd.dao.util.EnumToStringConverter;
import javax.persistence.Converter;

@Converter
public class UserInterfaceModeConverter extends EnumToStringConverter<UserInterfaceMode> {
  public UserInterfaceModeConverter() {
    super(UserInterfaceMode.class);
  }
}
