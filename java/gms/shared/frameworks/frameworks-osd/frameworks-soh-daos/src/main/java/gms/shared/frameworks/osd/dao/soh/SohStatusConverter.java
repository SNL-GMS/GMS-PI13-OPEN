package gms.shared.frameworks.osd.dao.soh;

import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.dao.util.EnumToOrdinalShortConverter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;


@Converter(autoApply = true)
public class SohStatusConverter extends EnumToOrdinalShortConverter<SohStatus> {
  private static final Map<SohStatus, Short> toColumn;

  static {
    toColumn = new EnumMap<>(SohStatus.class);

    Arrays.asList(SohStatus.values())
        .forEach(x -> toColumn.put(x, x.getDbId()));
  }
  /**
   * Creates a converter that maps a MagnitudeModel enum to a string representation of those enums
   */
  public SohStatusConverter() {
    super(toColumn);
  }
}
