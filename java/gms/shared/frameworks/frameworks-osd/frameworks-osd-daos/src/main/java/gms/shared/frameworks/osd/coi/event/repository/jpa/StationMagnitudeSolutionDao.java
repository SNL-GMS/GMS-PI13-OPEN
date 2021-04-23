package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.PhaseType;
import gms.shared.frameworks.osd.coi.event.MagnitudeModel;
import gms.shared.frameworks.osd.coi.event.MagnitudeType;
import gms.shared.frameworks.osd.coi.event.repository.jpa.util.MagnitudeModelConverter;
import gms.shared.frameworks.osd.coi.event.repository.jpa.util.MagnitudeTypeConverter;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.AmplitudeFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.StationDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.PhaseTypeConverter;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * JPA data access object for {@link gms.shared.frameworks.osd.coi.event.StationMagnitudeSolution}
 */
@Entity
@Table(name = "station_magnitude_solution")
public class StationMagnitudeSolutionDao {

  @Id
  private UUID id;

  @Column(name = "type", nullable = false)
  @Convert(converter = MagnitudeTypeConverter.class)
  private MagnitudeType type;

  @Column(name = "model", nullable = false)
  @Convert(converter = MagnitudeModelConverter.class)
  private MagnitudeModel model;

  @ManyToOne
  @JoinColumn(name = "station_name", referencedColumnName = "name",
  foreignKey = @ForeignKey(name = "sta_mag_solution_to_station"))
  private StationDao station;

  @Column(name = "phase", nullable = false)
  @Convert(converter = PhaseTypeConverter.class)
  private PhaseType phase;

  @Column(name = "magnitude", nullable = false)
  private double magnitude;

  @Column(name = "magnitude_uncertainty", nullable = false)
  private double magnitudeUncertainty;

  @Column(name = "model_correction", nullable = false)
  private double modelCorrection;

  @Column(name = "station_correction", nullable = false)
  private double stationCorrection;

  @OneToOne(cascade = CascadeType.MERGE)
  @JoinTable(name = "station_magnitude_feature_measurements",
      joinColumns = {@JoinColumn(name = "station_magnitude_solution_id",
          foreignKey = @ForeignKey(name = "feature_measurement_to_station_magnitude_fk"))},
      inverseJoinColumns = {@JoinColumn(name = "feature_measurement_id",
          foreignKey = @ForeignKey(name = "station_magnitude_to_feature_measurement_fk"))})
  private AmplitudeFeatureMeasurementDao measurement;

  /**
   * Default constructor for JPA.
   */
  public StationMagnitudeSolutionDao() {
    // Empty constructor needed for JPA
  }

  /** Generated getters / setters */
  /**
   * Generated getters / setters
   */
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public MagnitudeType getType() {
    return type;
  }

  public void setType(MagnitudeType type) {
    this.type = type;
  }

  public MagnitudeModel getModel() {
    return model;
  }

  public void setModel(MagnitudeModel model) {
    this.model = model;
  }

  public StationDao getStation() {
    return station;
  }

  public void setStation(StationDao station) {
    this.station = station;
  }

  public PhaseType getPhase() {
    return phase;
  }

  public void setPhase(PhaseType phase) {
    this.phase = phase;
  }

  public Double getMagnitude() {
    return magnitude;
  }

  public void setMagnitude(Double magnitude) {
    this.magnitude = magnitude;
  }

  public Double getMagnitudeUncertainty() {
    return magnitudeUncertainty;
  }

  public void setMagnitudeUncertainty(Double magnitudeUncertainty) {
    this.magnitudeUncertainty = magnitudeUncertainty;
  }

  public Double getModelCorrection() {
    return modelCorrection;
  }

  public void setModelCorrection(Double modelCorrection) {
    this.modelCorrection = modelCorrection;
  }

  public Double getStationCorrection() {
    return stationCorrection;
  }

  public void setStationCorrection(Double stationCorrection) {
    this.stationCorrection = stationCorrection;
  }

  public AmplitudeFeatureMeasurementDao getMeasurement() {
    return measurement;
  }

  public void setMeasurement(AmplitudeFeatureMeasurementDao measurement) {
    this.measurement = measurement;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StationMagnitudeSolutionDao that = (StationMagnitudeSolutionDao) o;
    return id == that.id &&
        Double.compare(that.magnitude, magnitude) == 0 &&
        Double.compare(that.magnitudeUncertainty, magnitudeUncertainty) == 0 &&
        Double.compare(that.modelCorrection, modelCorrection) == 0 &&
        Double.compare(that.stationCorrection, stationCorrection) == 0 &&
        type == that.type &&
        model == that.model &&
        Objects.equals(station, that.station) &&
        phase == that.phase &&
        Objects.equals(measurement, that.measurement);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, type, model, station, phase, magnitude, magnitudeUncertainty, modelCorrection,
            stationCorrection, measurement);
  }

  @Override
  public String toString() {
    return "StationMagnitudeSolutionDao{" +
        "id=" + id +
        ", type=" + type +
        ", model=" + model +
        ", stationDao=" + station +
        ", phase=" + phase +
        ", magnitude=" + magnitude +
        ", magnitudeUncertainty=" + magnitudeUncertainty +
        ", modelCorrection=" + modelCorrection +
        ", stationCorrection=" + stationCorrection +
        ", measurement=" + measurement +
        '}';
  }
}
