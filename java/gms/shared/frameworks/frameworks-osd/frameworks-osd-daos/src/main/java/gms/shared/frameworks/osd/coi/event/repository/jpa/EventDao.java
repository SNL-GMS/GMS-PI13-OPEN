package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.event.Event;
import gms.shared.frameworks.osd.coi.event.EventHypothesis;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.FinalEventHypothesis;
import gms.shared.frameworks.osd.coi.event.LocationBehavior;
import gms.shared.frameworks.osd.coi.event.NetworkMagnitudeBehavior;
import gms.shared.frameworks.osd.coi.event.PreferredEventHypothesis;
import gms.shared.frameworks.osd.coi.event.StationMagnitudeSolution;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * JPA data access object for
 * {@link Event}
 */
@Entity
@Table(name = "event")
public class EventDao {

  @Id
  @Column(updatable = false, unique = true)
  private UUID id;

  // TODO: need to add foreign key constraints
  @ElementCollection
  @CollectionTable(name = "event_rejected_signal_detection_associations")
  private Set<UUID> rejectedSignalDetectionAssociations;

  @Column(name = "monitoring_organization")
  private String monitoringOrganization;

  @Transient
  private Set<EventHypothesisDao> hypotheses;

  @OneToMany(cascade = CascadeType.PERSIST)
  @JoinTable(name = "final_event_hypotheses",
      joinColumns = {@JoinColumn(name = "event_id", table = "event", referencedColumnName = "id")},
      inverseJoinColumns = {@JoinColumn(name = "event_hypothesis_id", table = "event_hypothesis",
          referencedColumnName = "id")})
  private List<EventHypothesisDao> finalEventHypothesisHistory;

  @OneToMany(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "event_id", referencedColumnName = "id")
  private List<PreferredEventHypothesisDao> preferredEventHypothesisHistory;

  /**
   * No-arg constructor for use by JPA.
   */
  public EventDao() {
    // Empty constructor needed for JPA
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Set<UUID> getRejectedSignalDetectionAssociations() {
    return rejectedSignalDetectionAssociations;
  }

  public void setRejectedSignalDetectionAssociations(
      Set<UUID> rejectedSignalDetectionAssociations) {
    this.rejectedSignalDetectionAssociations = rejectedSignalDetectionAssociations;
  }

  public String getMonitoringOrganization() {
    return monitoringOrganization;
  }

  public void setMonitoringOrganization(String monitoringOrganization) {
    this.monitoringOrganization = monitoringOrganization;
  }

  public Set<EventHypothesisDao> getHypotheses() {
    return hypotheses;
  }

  public void setHypotheses(
      Set<EventHypothesisDao> hypotheses) {
    this.hypotheses = hypotheses;
  }

  public List<EventHypothesisDao> getFinalEventHypothesisHistory() {
    return finalEventHypothesisHistory;
  }

  public void setFinalEventHypothesisHistory(
      List<EventHypothesisDao> finalEventHypothesisHistory) {
    this.finalEventHypothesisHistory = finalEventHypothesisHistory;
  }

  public List<PreferredEventHypothesisDao> getPreferredEventHypothesisHistory() {
    return preferredEventHypothesisHistory;
  }

  public void setPreferredEventHypothesisHistory(
      List<PreferredEventHypothesisDao> preferredEventHypothesisHistory) {
    this.preferredEventHypothesisHistory = preferredEventHypothesisHistory;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventDao eventDao = (EventDao) o;
    return Objects.equals(getId(), eventDao.getId()) &&
        Objects.equals(getRejectedSignalDetectionAssociations(),
            eventDao.getRejectedSignalDetectionAssociations()) &&
        Objects.equals(getMonitoringOrganization(), eventDao.getMonitoringOrganization()) &&
        Objects.equals(getHypotheses(), eventDao.getHypotheses()) &&
        Objects
            .equals(getFinalEventHypothesisHistory(), eventDao.getFinalEventHypothesisHistory()) &&
        Objects.equals(getPreferredEventHypothesisHistory(),
            eventDao.getPreferredEventHypothesisHistory());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getRejectedSignalDetectionAssociations(),
        getMonitoringOrganization(), getHypotheses(), getFinalEventHypothesisHistory(),
        getPreferredEventHypothesisHistory());
  }

  @Override
  public String toString() {
    return "EventDao{" +
        ", id=" + id +
        ", rejectedSignalDetectionAssociations=" + rejectedSignalDetectionAssociations +
        ", monitoringOrganization='" + monitoringOrganization + '\'' +
        ", hypotheses=" + hypotheses +
        ", finalEventHypothesisHistory=" + finalEventHypothesisHistory +
        ", preferredEventHypothesisHistory=" + preferredEventHypothesisHistory +
        '}';
  }

  private static <A, B> List<B> convertList(List<A> as, Function<A, B> f) {
    return as.stream().map(f).collect(Collectors.toList());
  }

  private static <A, B> Set<B> convertSet(Set<A> as, Function<A, B> f) {
    return as.stream().map(f).collect(Collectors.toSet());
  }
}
