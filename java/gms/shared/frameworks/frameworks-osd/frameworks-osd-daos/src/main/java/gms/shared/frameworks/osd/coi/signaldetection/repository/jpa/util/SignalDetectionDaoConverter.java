package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util;

import gms.shared.frameworks.osd.coi.signaldetection.SignalDetection;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionHypothesis;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.SignalDetectionDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.StationDao;
import java.util.List;
import java.util.Objects;

public class SignalDetectionDaoConverter {

  private SignalDetectionDaoConverter() {}

  public static SignalDetectionDao toDao(SignalDetection signalDetection, StationDao stationDao) {
    Objects.requireNonNull(signalDetection, "Cannot create SignalDetectionDao from a null SignalDetection");

    SignalDetectionDao dao = new SignalDetectionDao(
        signalDetection.getId(), signalDetection.getMonitoringOrganization(), stationDao);

    return dao;
  }

  public static SignalDetection fromDao(SignalDetectionDao signalDetectionDao, List<SignalDetectionHypothesis> signalDetectionHypotheses) {
    Objects.requireNonNull(signalDetectionDao, "Cannot create SignalDetection from a null SignalDetectionDao");
    Objects.requireNonNull(signalDetectionHypotheses, "Cannot create SignalDetection from a null signalDetectionHypotheses");

    return SignalDetection.from(signalDetectionDao.getId(),
        signalDetectionDao.getMonitoringOrganization(), signalDetectionDao.getStation().getName(),
        signalDetectionHypotheses);
  }
}
