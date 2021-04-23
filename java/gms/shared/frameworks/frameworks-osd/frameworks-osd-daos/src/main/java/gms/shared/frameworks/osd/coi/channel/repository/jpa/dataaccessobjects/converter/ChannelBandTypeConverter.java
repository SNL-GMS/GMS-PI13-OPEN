package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.converter;

import gms.shared.frameworks.osd.coi.channel.ChannelBandType;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import javax.persistence.Converter;

@Converter
public class ChannelBandTypeConverter extends EnumToStringConverter<ChannelBandType> {
  public ChannelBandTypeConverter() {
    super(ChannelBandType.class);
  }
}
