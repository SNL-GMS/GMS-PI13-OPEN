package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.converter;


import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import gms.shared.frameworks.osd.coi.waveforms.Timeseries;
import gms.shared.frameworks.osd.coi.waveforms.Timeseries.Type;

public class TimeseriesTypeConverter extends EnumToStringConverter<Type> {
  public TimeseriesTypeConverter() {
    super(Timeseries.Type.class);
  }
}
