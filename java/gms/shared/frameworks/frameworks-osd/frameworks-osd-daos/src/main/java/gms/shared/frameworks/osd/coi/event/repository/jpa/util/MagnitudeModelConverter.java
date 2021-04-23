package gms.shared.frameworks.osd.coi.event.repository.jpa.util;

import gms.shared.frameworks.osd.coi.event.MagnitudeModel;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import javax.persistence.Converter;

/**
 * JPA converter to translate the {@link MagnitudeModel} enumeration to and from a database column.
 * Generates a String identity for the database value.  When the {@link MagnitudeModel} is updated this
 * converter must also be updated to assign an id for the new literal.
 * <p>
 * Intended to avoid issues with standard JPA enumeration mappings to String (subject to data
 * inconsistencies if the literals are renumbered or removed). This conversion is still subject to issues if a
 * literal is removed in which case {@link #convertToEntityAttribute(String)} would not resolve to
 * any literal.
 */
@Converter(autoApply = true)
public class MagnitudeModelConverter extends EnumToStringConverter<MagnitudeModel> {

    /**
     * Creates a converter that maps a MagnitudeModel enum to a string representation of those enums
     */
    public MagnitudeModelConverter() {
        super(MagnitudeModel.class);
    }
}
