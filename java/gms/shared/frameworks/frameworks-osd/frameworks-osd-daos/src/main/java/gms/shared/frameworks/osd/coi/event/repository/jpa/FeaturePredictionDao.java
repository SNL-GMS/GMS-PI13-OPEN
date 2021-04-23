package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.PhaseType;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.LocationDao;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.Updateable;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.PhaseTypeConverter;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "feature_prediction_dao_type",
    discriminatorType = DiscriminatorType.STRING,
    columnDefinition = "varchar(255)")
@Table(name = "feature_prediction")
public abstract class FeaturePredictionDao<T> implements Updateable<FeaturePrediction<T>> {

  @Id
  private UUID id;

  @Column(name = "phase", nullable = false)
  @Convert(converter = PhaseTypeConverter.class)
  private PhaseType phase;

  @ManyToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "feature_prediction_id", referencedColumnName = "id")
  private Set<FeaturePredictionComponentDao> featurePredictionComponents;

  @Column(name = "extrapolated", nullable = false)
  private boolean extrapolated;

  @Column(name = "prediction_type", nullable = false)
  private String predictionType;

  @Embedded
  @AttributeOverrides( {
      @AttributeOverride(name = "event_latitude_degrees", column = @Column(name =
          "source_event_latitude_degrees", nullable = false)),
      @AttributeOverride(name = "event_longitude_degrees", column = @Column(name =
          "source_event_longitude_degrees", nullable = false)),
      @AttributeOverride(name = "event_depth_km", column = @Column(name = "source_event_depth_km"
          , nullable = false)),
      @AttributeOverride(name = "event_time", column = @Column(name = "source_event_time",
          nullable = false))
  })
  private EventLocationDao sourceLocation;

  @Embedded
  @AttributeOverrides( {
      @AttributeOverride(name = "latitude_degrees", column = @Column(name =
          "receiver_latitude_degrees", nullable = false)),
      @AttributeOverride(name = "longitude_degrees", column = @Column(name =
          "receiver_longitude_degrees", nullable = false)),
      @AttributeOverride(name = "depth_km", column = @Column(name = "receiver_depth_km",
          nullable = false)),
      @AttributeOverride(name = "elevation_km", column = @Column())
  })
  private LocationDao receiverLocation;

  @ManyToOne
  @JoinColumn(name = "channel_name", referencedColumnName = "name",
      foreignKey = @ForeignKey(name = "feature_prediction_to_channel"))
  private ChannelDao channel;

  /**
   * Default constructor for JPA.
   */
  public FeaturePredictionDao() {
  }

  public abstract Optional<T> toCoiPredictionValue();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public PhaseType getPhase() {
    return phase;
  }

  public void setPhase(PhaseType phase) {
    this.phase = phase;
  }

  public Set<FeaturePredictionComponentDao> getFeaturePredictionComponents() {
    return featurePredictionComponents;
  }

  public void setFeaturePredictionComponents(Set<FeaturePredictionComponentDao> featurePredictionComponents) {
    this.featurePredictionComponents = featurePredictionComponents;
  }

  public boolean isExtrapolated() {
    return extrapolated;
  }

  public void setExtrapolated(boolean extrapolated) {
    this.extrapolated = extrapolated;
  }

  public String getPredictionType() {
    return predictionType;
  }

  public void setPredictionType(String predictionType) {
    this.predictionType = predictionType;
  }

  public EventLocationDao getSourceLocation() {
    return sourceLocation;
  }

  public void setSourceLocation(EventLocationDao sourceLocation) {
    this.sourceLocation = sourceLocation;
  }

  public LocationDao getReceiverLocation() {
    return receiverLocation;
  }

  public void setReceiverLocation(LocationDao receiverLocation) {
    this.receiverLocation = receiverLocation;
  }

  public ChannelDao getChannel() {
    return channel;
  }

  public void setChannel(ChannelDao channel) {
    this.channel = channel;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeaturePredictionDao<?> that = (FeaturePredictionDao<?>) o;
    return extrapolated == that.extrapolated &&
        id.equals(that.id) &&
        phase == that.phase &&
        featurePredictionComponents.equals(that.featurePredictionComponents) &&
        predictionType.equals(that.predictionType) &&
        sourceLocation.equals(that.sourceLocation) &&
        receiverLocation.equals(that.receiverLocation) &&
        channel.equals(that.channel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, phase, featurePredictionComponents, extrapolated, predictionType,
        sourceLocation, receiverLocation, channel);
  }

  @Override
  public String toString() {
    return "FeaturePredictionDao{" +
        "id=" + id +
        ", phase=" + phase +
        ", featurePredictionComponents=" + featurePredictionComponents +
        ", isExtrapolated=" + extrapolated +
        ", predictionType='" + predictionType + '\'' +
        ", sourceLocation=" + sourceLocation +
        ", receiverLocation=" + receiverLocation +
        ", channelId=" + channel +
        '}';
  }
}
