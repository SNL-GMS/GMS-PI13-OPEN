package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.converter;

import gms.shared.frameworks.osd.coi.channel.ChannelOrientationType;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import javax.persistence.Converter;

@Converter
public class ChannelOrientationTypeConverter extends EnumToStringConverter<ChannelOrientationType> {
  public ChannelOrientationTypeConverter() {
    super(ChannelOrientationType.class);
  }
}
