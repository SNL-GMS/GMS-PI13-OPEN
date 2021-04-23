package gms.shared.frameworks.osd.dao.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.persistence.AttributeConverter;

public abstract class EnumToOrdinalShortConverter<T extends Enum> implements AttributeConverter<T, Short> {

  private final Map<T, Short> toColumn;
  private final Map<Short, T> fromColumn;

  public EnumToOrdinalShortConverter(Map<T, Short> forwardMapping) {
    Objects.requireNonNull(forwardMapping,
        "Cannot create EnumToOrdinalShortConverter with a null forwardMapping");

    toColumn = new HashMap<>(forwardMapping);
    fromColumn = new HashMap<>();
    toColumn.forEach((k, v) -> fromColumn.put(v, k));
  }

  @Override
  public Short convertToDatabaseColumn(T filterType) {
    return toColumn.get(filterType);
  }

  @Override
  public T convertToEntityAttribute(Short id) {
    return fromColumn.get(id);
  }
}
