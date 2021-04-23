package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.converter;

import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment.Type;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import javax.persistence.Converter;

@Converter
public class ChannelSegmentTypeConverter extends EnumToStringConverter<Type> {
  public ChannelSegmentTypeConverter() {
    super(ChannelSegment.Type.class);
  }
}
