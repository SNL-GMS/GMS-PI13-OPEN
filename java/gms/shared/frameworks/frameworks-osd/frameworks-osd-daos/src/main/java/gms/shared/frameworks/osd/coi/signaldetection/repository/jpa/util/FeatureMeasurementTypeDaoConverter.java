package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util;

import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementTypeDao;
import gms.shared.frameworks.osd.coi.util.EnumToStringConverter;
import javax.persistence.Converter;

@Converter
public class FeatureMeasurementTypeDaoConverter extends
    EnumToStringConverter<FeatureMeasurementTypeDao> {

  public FeatureMeasurementTypeDaoConverter() {
    super(FeatureMeasurementTypeDao.class);
  }
}
