package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.converter;

import gms.shared.frameworks.osd.coi.channel.ChannelInstrumentType;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import javax.persistence.Converter;

@Converter
public class ChannelInstrumentTypeConverter extends EnumToStringConverter<ChannelInstrumentType> {
  public ChannelInstrumentTypeConverter() {
    super(ChannelInstrumentType.class);
  }
}
