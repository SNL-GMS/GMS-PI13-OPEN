package gms.shared.frameworks.osd.control.event.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import gms.shared.frameworks.osd.coi.InstantValue;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.LocationDao;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventLocationDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.FeaturePredictionComponentDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.FeaturePredictionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.InstantFeaturePredictionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.NumericFeaturePredictionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.PhaseFeaturePredictionDao;
import gms.shared.frameworks.osd.coi.signaldetection.EnumeratedMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementType;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementTypes;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementTypesChecking;
import gms.shared.frameworks.osd.coi.signaldetection.NumericMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.InstantValueDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.NumericMeasurementValueDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.PhaseTypeMeasurementValueDao;
import javax.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility for finding, converting, and storing {@link FeaturePrediction} and
 * {@link FeaturePredictionDao}
 */
public class FeaturePredictionDaoUtility {

  /**
   * Converts an {@link FeaturePrediction} to an {@link FeaturePredictionDao}. If the corresponding
   * {@link FeaturePredictionDao} has already been stored, the existing {@link FeaturePredictionDao}
   * is retrieved and returned. Otherwise, a new one is created and persisted. An active transaction
   * in the {@link EntityManager} is required for this function.
   * @param prediction the {@link FeaturePrediction} to convert
   * @param entityManager the {@link EntityManager} used to persist the converted object, and
   * retrieve the existing object, if it exists
   * @return the {@link FeaturePredictionDao} representing the provided {@link FeaturePrediction}
   */
  public static FeaturePredictionDao<?> fromCoi(FeaturePrediction<?> prediction,
      EntityManager entityManager) {

    Objects.requireNonNull(prediction);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive());

    FeaturePredictionDao<?> dao = entityManager.find(FeaturePredictionDao.class,
        buildId(prediction));

    if (dao == null) {
      Class<?> predictionType = prediction.getPredictionType().getMeasurementValueType();
      FeaturePredictionDao<?> predictionDao;
      if (predictionType.equals(InstantValue.class)) {
        InstantFeaturePredictionDao instantPredictionDao = new InstantFeaturePredictionDao();
        prediction.getPredictedValue()
            .ifPresent(value -> instantPredictionDao.setValue(new InstantValueDao((InstantValue) value)));
        predictionDao = instantPredictionDao;
      } else if (predictionType.equals(NumericMeasurementValue.class)) {
        NumericFeaturePredictionDao numericPredictionDao = new NumericFeaturePredictionDao();
        prediction.getPredictedValue()
            .ifPresent(value -> numericPredictionDao.setValue(new NumericMeasurementValueDao((NumericMeasurementValue) value)));
        predictionDao = numericPredictionDao;
      } else if (predictionType.equals(EnumeratedMeasurementValue.PhaseTypeMeasurementValue.class)) {
        PhaseFeaturePredictionDao phasePredictionDao = new PhaseFeaturePredictionDao();
        prediction.getPredictedValue()
            .ifPresent(value -> phasePredictionDao.setValue(new PhaseTypeMeasurementValueDao((EnumeratedMeasurementValue.PhaseTypeMeasurementValue) value)));
        predictionDao = phasePredictionDao;
      } else {
        throw new IllegalArgumentException("Unsupported feature prediction type: " + predictionType);
      }

      predictionDao.setId(buildId(prediction));
      predictionDao.setPhase(prediction.getPhase());
      predictionDao.setFeaturePredictionComponents(prediction.getFeaturePredictionComponents()
          .stream()
          .map(FeaturePredictionComponentDao::from)
          .collect(Collectors.toSet()));
      predictionDao.setExtrapolated(prediction.isExtrapolated());
      predictionDao.setPredictionType(prediction.getPredictionTypeName());
      predictionDao.setSourceLocation(new EventLocationDao(prediction.getSourceLocation()));
      predictionDao.setReceiverLocation(new LocationDao(prediction.getReceiverLocation()));
      prediction.getChannelName().ifPresent(channelName -> {
        ChannelDao channelDao = entityManager.find(ChannelDao.class, channelName);

        Objects.requireNonNull(channelDao,
            "Cannot insert FeaturePrediction for Channel that does not exist");

        predictionDao.setChannel(channelDao);
      });

      dao = predictionDao;
      entityManager.persist(dao);
    }

    return dao;
  }

  /**
   * Converts the provided {@link FeaturePredictionDao} to the COI representation
   * @param predictionDao the {@link FeaturePredictionDao} to convert
   * {@link FeaturePredictionDao} used in the {@link FeaturePredictionDao}
   * @return the converted {@link FeaturePrediction}
   */
  public static FeaturePrediction<?> toCoi(FeaturePredictionDao<?> predictionDao) {
    Objects.requireNonNull(predictionDao);

    Class<?> predictionType = FeatureMeasurementTypesChecking
        .measurementValueClassFromMeasurementTypeString(predictionDao.getPredictionType());
    FeaturePrediction.Builder<?> builder;
    if (predictionType.equals(InstantValue.class)) {
      builder = FeaturePrediction.<InstantValue>builder()
          .setPredictedValue(((InstantFeaturePredictionDao) predictionDao).toCoiPredictionValue())
          .setPredictionType((FeatureMeasurementType<InstantValue>)
              FeatureMeasurementTypes.getTypeStringToFeatureMeasurementTypeInstance()
                  .get(predictionDao.getPredictionType()));
    } else if (predictionType.equals(NumericMeasurementValue.class)) {
      builder = FeaturePrediction.<NumericMeasurementValue>builder()
          .setPredictedValue(((NumericFeaturePredictionDao) predictionDao).toCoiPredictionValue())
          .setPredictionType((FeatureMeasurementType<NumericMeasurementValue>)
              FeatureMeasurementTypes.getTypeStringToFeatureMeasurementTypeInstance()
                  .get(predictionDao.getPredictionType()));
    } else if (predictionType.equals(EnumeratedMeasurementValue.PhaseTypeMeasurementValue.class)) {
      builder = FeaturePrediction.<EnumeratedMeasurementValue.PhaseTypeMeasurementValue>builder()
          .setPredictedValue(((PhaseFeaturePredictionDao) predictionDao).toCoiPredictionValue())
          .setPredictionType((FeatureMeasurementType<EnumeratedMeasurementValue.PhaseTypeMeasurementValue>)
              FeatureMeasurementTypes.getTypeStringToFeatureMeasurementTypeInstance()
                  .get(predictionDao.getPredictionType()));
    } else {
      throw new IllegalArgumentException("Unsupported feature prediction type: " + predictionType);
    }

    return builder.setPhase(predictionDao.getPhase())
        .setFeaturePredictionComponents(predictionDao.getFeaturePredictionComponents().stream()
            .map(FeaturePredictionComponentDao::toCoi)
            .collect(Collectors.toSet()))
        .setExtrapolated(predictionDao.isExtrapolated())
        .setSourceLocation(predictionDao.getSourceLocation().toCoi())
        .setReceiverLocation(predictionDao.getReceiverLocation().toCoi())
        .setChannelName(predictionDao.getChannel() == null ? null :
            predictionDao.getChannel().getName())
        .setFeaturePredictionDerivativeMap(ImmutableMap.of())
        .build();
  }

  /**
   * Builds an id for the {@link FeaturePrediction}, based on its contents
   * @param featurePrediction the {@link FeaturePrediction} for which the id will be created
   * @return the UUID for the provided {@link FeaturePrediction}
   */
  public static UUID buildId(FeaturePrediction<?> featurePrediction) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ObjectOutput output = new ObjectOutputStream(out);
      output.writeObject(featurePrediction.getPhase());
      output.writeObject(featurePrediction.getPredictedValue().orElse(null));
      output.writeObject(featurePrediction.getFeaturePredictionComponents());
      output.writeObject(featurePrediction.isExtrapolated());
      output.writeObject(featurePrediction.getPredictionType());
      output.writeObject(featurePrediction.getSourceLocation());
      output.writeObject(featurePrediction.getReceiverLocation());
      output.writeObject(featurePrediction.getChannelName().orElse(null));
      output.writeObject(featurePrediction.getFeaturePredictionDerivativeMap());

      output.flush();

      byte[] bytes = out.toByteArray();

      Objects.requireNonNull(bytes,
          "Could not hash FeaturePrediction attributes; byte array containing serialized " +
              "FeaturePrediction is null");

      return UUID.nameUUIDFromBytes(Hashing.sha256().hashBytes(bytes).toString().getBytes());
    } catch (IOException ex) {
      throw new IllegalStateException("Error serializing FeaturePrediction attributes to byte " +
          "array", ex);
    }
  }
}
