package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util;


import gms.shared.frameworks.osd.coi.signaldetection.QcMaskVersionDescriptor;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.QcMaskVersionDescriptorDao;

public class QcMaskVersionDescriptorDaoConverter {

  private QcMaskVersionDescriptorDaoConverter() {
  }

  public static QcMaskVersionDescriptorDao toDao(QcMaskVersionDescriptor qcMaskVersionDescriptor) {
    return new QcMaskVersionDescriptorDao(
        qcMaskVersionDescriptor.getQcMaskId(),
        qcMaskVersionDescriptor.getQcMaskVersionId());
  }

  public static QcMaskVersionDescriptor fromDao(
      QcMaskVersionDescriptorDao qcMaskVersionDescriptorDao) {
    return QcMaskVersionDescriptor
        .from(qcMaskVersionDescriptorDao.getQcMaskId(),
            qcMaskVersionDescriptorDao.getQcMaskVersionId());

  }

}
