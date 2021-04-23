package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.converter;

import gms.shared.frameworks.osd.coi.channel.ChannelDataType;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import javax.persistence.Converter;

@Converter
public class ChannelDataTypeConverter extends EnumToStringConverter<ChannelDataType> {
  public ChannelDataTypeConverter() {
    super(ChannelDataType.class);
  }
}
