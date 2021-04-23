package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.event.repository.jpa.SignalDetectionEventAssociationDao;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "signal_detection_hypothesis")
public class SignalDetectionHypothesisDao {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "signal_detection_id", referencedColumnName = "id")
  private SignalDetectionDao parentSignalDetection;

  @Column(name = "monitoring_organization")
  private String monitoringOrganization;

  @Column(name = "station_name")
  private String stationName;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "parent_signal_detection_hypothesis_id", referencedColumnName = "id")
  private SignalDetectionHypothesisDao parentSignalDetectionHypothesis;

  @Column(name = "rejected")
  private boolean isRejected;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinColumn(name = "arrival_time_measurement_id", referencedColumnName = "id")
  private InstantFeatureMeasurementDao arrivalTimeMeasurement;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinColumn(name = "phase_measurement_id", referencedColumnName = "id")
  private PhaseFeatureMeasurementDao phaseMeasurement;

  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinTable(name = "signal_detection_hypothesis_feature_measurement",
      joinColumns = {@JoinColumn(name = "signal_detection_hypothesis_id", referencedColumnName =
          "id")},
      inverseJoinColumns = {@JoinColumn(name = "feature_measurement_id", referencedColumnName =
          "id")})
  private Collection<FeatureMeasurementDao<?>> otherMeasurements;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "signal_detection_hypothesis_id")
  private Set<SignalDetectionEventAssociationDao> associations;

  protected SignalDetectionHypothesisDao() {
  }

  public SignalDetectionHypothesisDao(UUID id,
      SignalDetectionDao parentSignalDetection,
      String monitoringOrganization,
      String stationName,
      SignalDetectionHypothesisDao parentSignalDetectionHypothesis,
      boolean isRejected,
      InstantFeatureMeasurementDao arrivalTimeMeasurement,
      PhaseFeatureMeasurementDao phaseMeasurement,
      Collection<FeatureMeasurementDao<?>> otherMeasurements) {
    this.id = id;
    this.parentSignalDetection = parentSignalDetection;
    this.monitoringOrganization = monitoringOrganization;
    this.stationName = stationName;
    this.parentSignalDetectionHypothesis = parentSignalDetectionHypothesis;
    this.isRejected = isRejected;
    this.arrivalTimeMeasurement = arrivalTimeMeasurement;
    this.phaseMeasurement = phaseMeasurement;
    this.otherMeasurements = otherMeasurements;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public SignalDetectionDao getParentSignalDetection() {
    return parentSignalDetection;
  }

  public void setParentSignalDetection(
      SignalDetectionDao parentSignalDetection) {
    this.parentSignalDetection = parentSignalDetection;
  }

  public String getMonitoringOrganization() {
    return monitoringOrganization;
  }

  public void setMonitoringOrganization(String monitoringOrganization) {
    this.monitoringOrganization = monitoringOrganization;
  }

  public String getStationName() {
    return stationName;
  }

  public void setStationName(String stationName) {
    this.stationName = stationName;
  }

  public SignalDetectionHypothesisDao getParentSignalDetectionHypothesis() {
    return parentSignalDetectionHypothesis;
  }

  public void setParentSignalDetectionHypothesis(SignalDetectionHypothesisDao parentSignalDetectionHypothesis) {
    this.parentSignalDetectionHypothesis = parentSignalDetectionHypothesis;
  }

  public boolean isRejected() {
    return isRejected;
  }

  public void setRejected(boolean rejected) {
    isRejected = rejected;
  }

  public Collection<FeatureMeasurementDao<?>> getFeatureMeasurements() {
    return otherMeasurements;
  }

  public void setFeatureMeasurements(
      Collection<FeatureMeasurementDao<?>> otherMeasurements) {
    this.otherMeasurements = otherMeasurements;
  }

  public InstantFeatureMeasurementDao getArrivalTimeMeasurement() {
    return arrivalTimeMeasurement;
  }

  public void setArrivalTimeMeasurement(
      InstantFeatureMeasurementDao arrivalTimeMeasurement) {
    this.arrivalTimeMeasurement = arrivalTimeMeasurement;
  }

  public PhaseFeatureMeasurementDao getPhaseMeasurement() {
    return phaseMeasurement;
  }

  public void setPhaseMeasurement(
      PhaseFeatureMeasurementDao phaseMeasurement) {
    this.phaseMeasurement = phaseMeasurement;
  }

  public Set<SignalDetectionEventAssociationDao> getAssociations() {
    return associations;
  }

  public void setAssociations(Set<SignalDetectionEventAssociationDao> associations) {
    this.associations = associations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SignalDetectionHypothesisDao that = (SignalDetectionHypothesisDao) o;
    return isRejected == that.isRejected &&
        id.equals(that.id) &&
        parentSignalDetection.equals(that.parentSignalDetection) &&
        arrivalTimeMeasurement.equals(that.arrivalTimeMeasurement) &&
        phaseMeasurement.equals(that.phaseMeasurement) &&
        otherMeasurements.equals(that.otherMeasurements) &&
        associations.equals(that.associations);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, parentSignalDetection, isRejected,
            arrivalTimeMeasurement,
            phaseMeasurement, otherMeasurements, associations);
  }

  @Override
  public String toString() {
    return "SignalDetectionHypothesisDao{" +
        "signalDetectionHypothesisId=" + id +
        ", parentSignalDetection=" + parentSignalDetection +
        ", isRejected=" + isRejected +
        ", arrivalTimeMeasurement=" + arrivalTimeMeasurement +
        ", phaseMeasurement=" + phaseMeasurement +
        ", otherMeasurements=" + otherMeasurements +
        ", associations=" + associations +
        '}';
  }
}
