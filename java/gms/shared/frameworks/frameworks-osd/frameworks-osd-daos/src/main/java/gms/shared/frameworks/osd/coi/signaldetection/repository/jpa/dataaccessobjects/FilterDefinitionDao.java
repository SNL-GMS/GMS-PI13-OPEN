package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.signaldetection.FilterCausality;
import gms.shared.frameworks.osd.coi.signaldetection.FilterPassBandType;
import gms.shared.frameworks.osd.coi.signaldetection.FilterSource;
import gms.shared.frameworks.osd.coi.signaldetection.FilterType;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.DoublePrecisionArrayType;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.FilterCausalityConverter;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.FilterPassBandTypeConverter;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.FilterSourceConverter;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.FilterTypeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import java.util.Arrays;
import java.util.Objects;

/**
 * JPA data access object for {@link gms.shared.frameworks.osd.coi.signaldetection.FilterDefinition}
 */
@TypeDefs( {
    @TypeDef(
        name = "double-precision-array",
        typeClass = DoublePrecisionArrayType.class
    )
})
@Entity
@Table(name = "filter_definition")
public class FilterDefinitionDao {

  @Id
  @GeneratedValue
  private long id;

  @Column(name = "name")
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "filter_type")
  @Convert(converter = FilterTypeConverter.class)
  private FilterType filterType;

  @Column(name = "filter_pass_band_type")
  @Convert(converter = FilterPassBandTypeConverter.class)
  private FilterPassBandType filterPassBandType;

  @Column(name = "low_frequency_hz")
  private double lowFrequencyHz;

  @Column(name = "high_frequency_hz")
  private double highFrequencyHz;

  @Column(name = "filter_order")
  private int filterOrder;

  @Column(name = "filter_source")
  @Convert(converter = FilterSourceConverter.class)
  private FilterSource filterSource;

  @Column(name = "filter_causality")
  @Convert(converter = FilterCausalityConverter.class)
  private FilterCausality filterCausality;

  @Column(name = "zero_phase")
  private boolean zeroPhase;

  @Column(name = "sample_rate")
  private double sampleRate;

  @Column(name = "sample_rate_tolerance")
  private double sampleRateTolerance;

  @Type(type = "double-precision-array")
  @Column(name = "a_coefficients",
      columnDefinition = "double precision[]")
  private double[] aCoefficients;

  @Type(type = "double-precision-array")
  @Column(name = "b_coefficients",
      columnDefinition = "double precision[]")
  private double[] bCoefficients;

  @Column(name = "group_delay_secs")
  private double groupDelaySecs;

  public FilterDefinitionDao() {
    // Empty constructor needed for JPA
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public FilterType getFilterType() {
    return filterType;
  }

  public void setFilterType(
      FilterType filterType) {
    this.filterType = filterType;
  }

  public FilterPassBandType getFilterPassBandType() {
    return filterPassBandType;
  }

  public void setFilterPassBandType(
      FilterPassBandType filterPassBandType) {
    this.filterPassBandType = filterPassBandType;
  }

  public double getLowFrequencyHz() {
    return lowFrequencyHz;
  }

  public void setLowFrequencyHz(double lowFrequencyHz) {
    this.lowFrequencyHz = lowFrequencyHz;
  }

  public double getHighFrequencyHz() {
    return highFrequencyHz;
  }

  public void setHighFrequencyHz(double highFrequencyHz) {
    this.highFrequencyHz = highFrequencyHz;
  }

  public int getFilterOrder() {
    return filterOrder;
  }

  public void setFilterOrder(int filterOrder) {
    this.filterOrder = filterOrder;
  }

  public FilterSource getFilterSource() {
    return filterSource;
  }

  public void setFilterSource(
      FilterSource filterSource) {
    this.filterSource = filterSource;
  }

  public FilterCausality getFilterCausality() {
    return filterCausality;
  }

  public void setFilterCausality(
      FilterCausality filterCausality) {
    this.filterCausality = filterCausality;
  }

  public boolean isZeroPhase() {
    return zeroPhase;
  }

  public void setZeroPhase(boolean zeroPhase) {
    this.zeroPhase = zeroPhase;
  }

  public double getSampleRate() {
    return sampleRate;
  }

  public void setSampleRate(double sampleRate) {
    this.sampleRate = sampleRate;
  }

  public double getSampleRateTolerance() {
    return sampleRateTolerance;
  }

  public void setSampleRateTolerance(double sampleRateTolerance) {
    this.sampleRateTolerance = sampleRateTolerance;
  }

  public double[] getACoefficients() {
    return aCoefficients;
  }

  public void setACoefficients(double[] aCoefficients) {
    this.aCoefficients = aCoefficients;
  }

  public double[] getBCoefficients() {
    return bCoefficients;
  }

  public void setBCoefficients(double[] bCoefficients) {
    this.bCoefficients = bCoefficients;
  }

  public double getGroupDelaySecs() {
    return groupDelaySecs;
  }

  public void setGroupDelaySecs(double groupDelaySecs) {
    this.groupDelaySecs = groupDelaySecs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FilterDefinitionDao that = (FilterDefinitionDao) o;
    return id == that.id &&
        Double.compare(that.lowFrequencyHz, lowFrequencyHz) == 0 &&
        Double.compare(that.highFrequencyHz, highFrequencyHz) == 0 &&
        filterOrder == that.filterOrder &&
        zeroPhase == that.zeroPhase &&
        Double.compare(that.sampleRate, sampleRate) == 0 &&
        Double.compare(that.sampleRateTolerance, sampleRateTolerance) == 0 &&
        Double.compare(that.groupDelaySecs, groupDelaySecs) == 0 &&
        name.equals(that.name) &&
        description.equals(that.description) &&
        filterType == that.filterType &&
        filterPassBandType == that.filterPassBandType &&
        filterSource == that.filterSource &&
        filterCausality == that.filterCausality &&
        Arrays.equals(aCoefficients, that.aCoefficients) &&
        Arrays.equals(bCoefficients, that.bCoefficients);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(id, name, description, filterType, filterPassBandType,
        lowFrequencyHz, highFrequencyHz, filterOrder, filterSource, filterCausality, zeroPhase,
        sampleRate, sampleRateTolerance, groupDelaySecs);
    result = 31 * result + Arrays.hashCode(aCoefficients);
    result = 31 * result + Arrays.hashCode(bCoefficients);
    return result;
  }

  @Override
  public String toString() {
    return "FilterDefinitionDao{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", filterType=" + filterType +
        ", filterPassBandType=" + filterPassBandType +
        ", lowFrequencyHz=" + lowFrequencyHz +
        ", highFrequencyHz=" + highFrequencyHz +
        ", filterOrder=" + filterOrder +
        ", filterSource=" + filterSource +
        ", filterCausality=" + filterCausality +
        ", zeroPhase=" + zeroPhase +
        ", sampleRate=" + sampleRate +
        ", sampleRateTolerance=" + sampleRateTolerance +
        ", aCoefficients=" + Arrays.toString(aCoefficients) +
        ", bCoefficients=" + Arrays.toString(bCoefficients) +
        ", groupDelaySecs=" + groupDelaySecs +
        '}';
  }
}
