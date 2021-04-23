package gms.shared.frameworks.osd.control.utils;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import gms.shared.frameworks.osd.coi.InstantValue;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.signaldetection.AmplitudeMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.DurationMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.EnumeratedMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.NumericMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.AmplitudeFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.DurationFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FirstMotionFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.InstantFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.MeasuredChannelSegmentDescriptorDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.NumericFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.PhaseFeatureMeasurementDao;
import javax.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Objects;

public class FeatureMeasurementDaoUtility {

  public static FeatureMeasurementDao<?> fromCoi(FeatureMeasurement<?> measurement,
      EntityManager entityManager) {

    Objects.requireNonNull(measurement);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive(),
        "An active transaction is required to convert FeatureMeasurements");

    FeatureMeasurementDao<?> dao = entityManager.find(FeatureMeasurementDao.class,
        buildId(measurement));

    if (dao == null) {
      // TODO: is there a way to do this without the casting?  Would it be worthwhile?

      MeasuredChannelSegmentDescriptorDao descriptorDao = MeasuredChannelSegmentDescriptorDaoUtility
          .fromCoi(measurement.getMeasuredChannelSegmentDescriptor(), entityManager);

      final Class<?> type = measurement.getFeatureMeasurementType().getMeasurementValueType();
      String id = buildId(measurement);
      if (type.equals(AmplitudeMeasurementValue.class)) {
        dao =
            new AmplitudeFeatureMeasurementDao(id,
                (FeatureMeasurement<AmplitudeMeasurementValue>) measurement,
                descriptorDao);
      } else if (type.equals(DurationMeasurementValue.class)) {
        dao =
            new DurationFeatureMeasurementDao(id,
                (FeatureMeasurement<DurationMeasurementValue>) measurement,
                descriptorDao);
      } else if (type.equals(EnumeratedMeasurementValue.FirstMotionMeasurementValue.class)) {
        dao = new FirstMotionFeatureMeasurementDao(id,
            (FeatureMeasurement<EnumeratedMeasurementValue.FirstMotionMeasurementValue>) measurement,
            descriptorDao);
      } else if (type.equals(NumericMeasurementValue.class)) {
        dao =
            new NumericFeatureMeasurementDao(id,
                (FeatureMeasurement<NumericMeasurementValue>) measurement,
                descriptorDao);
      } else if (type.equals(EnumeratedMeasurementValue.PhaseTypeMeasurementValue.class)) {
        dao =
            new PhaseFeatureMeasurementDao(id,
                (FeatureMeasurement<EnumeratedMeasurementValue.PhaseTypeMeasurementValue>) measurement,
                descriptorDao);
      } else if (type.equals(InstantValue.class)) {
        dao = new InstantFeatureMeasurementDao(id,
            (FeatureMeasurement<InstantValue>) measurement,
            descriptorDao);
      } else {
        throw new IllegalArgumentException("Unsupported feature measurement type " + type);
      }

      entityManager.persist(dao);
    }

    return dao;
  }

  public static <T> FeatureMeasurement<T> toCoi(FeatureMeasurementDao<T> featureMeasurementDao,
      Channel channel) {

    Objects.requireNonNull(featureMeasurementDao);
    Objects.requireNonNull(channel);

    return FeatureMeasurement.from(channel,
        MeasuredChannelSegmentDescriptorDaoUtility.toCoi(featureMeasurementDao.getMeasuredChannelSegmentDescriptor()),
        featureMeasurementDao.getFeatureMeasurementType().getCoiType().getFeatureMeasurementTypeName(),
        featureMeasurementDao.toCoiMeasurementValue());
  }

  public static String buildId(FeatureMeasurement<?> featureMeasurement) {

    ObjectOutput objectOutput;
    byte[] objectBytes;

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

      objectOutput = new ObjectOutputStream(byteArrayOutputStream);

      objectOutput.writeObject(featureMeasurement.getMeasuredChannelSegmentDescriptor());
      objectOutput.writeObject(featureMeasurement.getFeatureMeasurementType());
      objectOutput.writeObject(featureMeasurement.getMeasurementValue());

      objectOutput.flush();

      objectBytes = byteArrayOutputStream.toByteArray();
    } catch (IOException e) {

      throw new IllegalStateException(
          "Error serializing FeatureMeasurement attributes to byte array", e);
    }

    Objects.requireNonNull(objectBytes,
        "Could not hash FeatureMeasurement attributes; byte array containing serialized " +
            "FeatureMeasurement output is null");

    return Hashing.sha256().hashBytes(objectBytes).toString();
  }
}
