package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.FeatureMeasurementTypeDaoConverter;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Objects;

// TOOO: Put bounds on the generics
@Entity(name = "feature_measurement")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class FeatureMeasurementDao<T> {

  @Id
  @Column(name = "id", length = 64)
  private String id;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "measured_channel_segment_descriptor_id", referencedColumnName = "id")
  private MeasuredChannelSegmentDescriptorDao measuredChannelSegmentDescriptor;

  @Column(name = "feature_measurement_type")
  @Convert(converter = FeatureMeasurementTypeDaoConverter.class)
  private FeatureMeasurementTypeDao featureMeasurementType;

  public abstract T toCoiMeasurementValue();

  public FeatureMeasurementDao() {
  }

  protected FeatureMeasurementDao(String id,
      FeatureMeasurement<T> featureMeasurement,
      MeasuredChannelSegmentDescriptorDao descriptor) {
    this.id = id;
    measuredChannelSegmentDescriptor = descriptor;
    featureMeasurementType = FeatureMeasurementTypeDao.fromFeatureMeasurementType(featureMeasurement.getFeatureMeasurementType());
  }

  public String getId() {
    return this.id;
  }

  public MeasuredChannelSegmentDescriptorDao getMeasuredChannelSegmentDescriptor() {
    return measuredChannelSegmentDescriptor;
  }

  public void setMeasuredChannelSegmentDescriptor(MeasuredChannelSegmentDescriptorDao measuredChannelSegmentDescriptor) {
    this.measuredChannelSegmentDescriptor = measuredChannelSegmentDescriptor;
  }

  public FeatureMeasurementTypeDao getFeatureMeasurementType() {
    return featureMeasurementType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeatureMeasurementDao<?> that = (FeatureMeasurementDao<?>) o;
    return id.equals(that.id) &&
        measuredChannelSegmentDescriptor.equals(that.measuredChannelSegmentDescriptor) &&
        featureMeasurementType == that.featureMeasurementType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, measuredChannelSegmentDescriptor, featureMeasurementType);
  }

  @Override
  public String toString() {
    return "FeatureMeasurementDao{" +
        "daoId='" + id + '\'' +
        ", measuredChannelSegmentDescriptor=" + measuredChannelSegmentDescriptor +
        ", featureMeasurementType='" + featureMeasurementType + '\'' +
        '}';
  }
}
