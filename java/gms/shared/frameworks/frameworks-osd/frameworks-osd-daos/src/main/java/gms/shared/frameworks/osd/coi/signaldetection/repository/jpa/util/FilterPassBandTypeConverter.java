package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util;

import gms.shared.frameworks.osd.coi.signaldetection.FilterPassBandType;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import javax.persistence.Converter;

/**
 * JPA converter to translate the {@link FilterPassBandType} enumeration to and from a database
 * column. Generates an integer identity for the database value.  When the FilterPassBandType is
 * updated this converter must also be updated to assign an id for the new literal.
 *
 * Intended to avoid issues with standard JPA enumeration mappings to integer (subject to data
 * inconsistencies if the literals are renumbered or removed) and to string (subject to data
 * inconsistencies if the literals are renamed).  This conversion is still subject to issues if a
 * literal is removed in which case {@link #convertToEntityAttribute(String)} would not resolve to
 * any literal.
 */
@Converter(autoApply = true)
public class FilterPassBandTypeConverter extends EnumToStringConverter<FilterPassBandType> {

  public FilterPassBandTypeConverter() {
    super(FilterPassBandType.class);
  }
}
