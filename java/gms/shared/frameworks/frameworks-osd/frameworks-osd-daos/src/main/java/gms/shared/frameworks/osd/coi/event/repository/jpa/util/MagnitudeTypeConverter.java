package gms.shared.frameworks.osd.coi.event.repository.jpa.util;

import gms.shared.frameworks.osd.coi.event.MagnitudeType;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import javax.persistence.Converter;

/**
 * JPA converter to translate the {@link MagnitudeType} enumeration to and from a database column.
 * Generates a String identity for the database value.  When the {@link MagnitudeType} is updated this
 * converter must also be updated to assign an id for the new literal.
 * <p>
 * Intended to avoid issues with standard JPA enumeration mappings to String (subject to data
 * inconsistencies if the literals are renumbered or removed). This conversion is still subject to issues if a
 * literal is removed in which case {@link #convertToEntityAttribute(String)} would not resolve to
 * any literal.
 */
@Converter(autoApply = true)
public class MagnitudeTypeConverter extends EnumToStringConverter<MagnitudeType> {

    public MagnitudeTypeConverter() {
        super(MagnitudeType.class);
    }
}
