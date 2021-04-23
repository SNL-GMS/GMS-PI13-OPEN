package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects;

import com.google.common.base.Objects;
import gms.shared.frameworks.osd.coi.Units;
import gms.shared.frameworks.osd.coi.channel.ChannelBandType;
import gms.shared.frameworks.osd.coi.channel.ChannelDataType;
import gms.shared.frameworks.osd.coi.channel.ChannelInstrumentType;
import gms.shared.frameworks.osd.coi.channel.ChannelOrientationType;
import gms.shared.frameworks.osd.coi.channel.ReferenceChannel;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.converter.ChannelBandTypeConverter;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.converter.ChannelDataTypeConverter;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.converter.ChannelInstrumentTypeConverter;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.converter.ChannelOrientationTypeConverter;
import gms.shared.frameworks.osd.coi.emerging.provenance.repository.InformationSourceDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.RelativePositionDao;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceAlias;
import gms.shared.frameworks.osd.coi.stationreference.repository.ReferenceAliasDao;
import gms.shared.frameworks.osd.coi.util.UnitsConverter;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.Instant;
import java.util.stream.Collectors;
import static javax.persistence.CascadeType.ALL;

@Entity
@Table(name = "reference_channel")
public class ReferenceChannelDao {

  @Id
  @GeneratedValue
  private long id;

  // TODO: Entity id may go away if channel name can be used to uniquely identify a ReferenceChannel
  @Column(name="entity_id")
  private UUID entityId;

  @Column(name="version_id", unique = true)
  private UUID versionId;

  @Column(name = "name")
  private String name;

  @Column(name = "data_type")
  @Convert(converter = ChannelDataTypeConverter.class)
  private ChannelDataType dataType;

  @Column(name = "band_type")
  @Convert(converter = ChannelBandTypeConverter.class)
  private ChannelBandType bandType;

  @Column(name = "instrument_type")
  @Convert(converter = ChannelInstrumentTypeConverter.class)
  private ChannelInstrumentType instrumentType;

  @Column(name = "orientation_type")
  @Convert(converter = ChannelOrientationTypeConverter.class)
  private ChannelOrientationType orientationType;

  @Column(name = "orientation_code")
  private char orientationCode;

  @Column(name = "location_code")
  private String locationCode;

  @Column(name = "latitude")
  private double latitude;

  @Column(name = "longitude")
  private double longitude;

  @Column(name = "elevation")
  private double elevation;

  @Column(name = "depth")
  private double depth;

  @Column(name = "vertical_angle")
  private double verticalAngle;

  @Column(name = "horizontal_angle")
  private double horizontalAngle;

  @Column(name = "units")
  @Convert(converter = UnitsConverter.class)
  private Units units;

  @Column(name = "nominal_sample_rate")
  private double nominalSampleRate;

  @Column(name = "actual_time")
  private Instant actualTime;

  @Column(name = "system_time")
  private Instant systemTime;

  @Embedded
  private InformationSourceDao informationSource;

  @Column(name = "comment")
  private String comment;

  @Embedded
  private RelativePositionDao position;

  @OneToMany(cascade = ALL)
  @JoinTable(
      name = "reference_channel_aliases",
      joinColumns = {@JoinColumn(name = "reference_channel", table = "reference_channel", referencedColumnName = "id")},
      inverseJoinColumns = {@JoinColumn(name = "reference_alias", table = "reference_alias", referencedColumnName = "id")}
  )
  private List<ReferenceAliasDao> aliases;

  /**
   * Default constructor for JPA.
   */
  public ReferenceChannelDao() {
  }

  /**
   * Create a DAO from the COI object.
   *
   * @param channel The ReferenceChannel object.
   * @throws NullPointerException
   */
  public ReferenceChannelDao(ReferenceChannel channel) {
    Validate.notNull(channel);
    this.entityId = channel.getEntityId();
    this.versionId = channel.getVersionId();
    this.name = channel.getName();
    this.dataType = channel.getDataType();
    this.bandType = channel.getBandType();
    this.instrumentType = channel.getInstrumentType();
    this.orientationType = channel.getOrientationType();
    this.orientationCode = channel.getOrientationCode();
    this.locationCode = channel.getLocationCode();
    this.latitude = channel.getLatitude();
    this.longitude = channel.getLongitude();
    this.elevation = channel.getElevation();
    this.depth = channel.getDepth();
    this.verticalAngle = channel.getVerticalAngle();
    this.horizontalAngle = channel.getHorizontalAngle();
    this.units = channel.getUnits();
    this.nominalSampleRate = channel.getNominalSampleRate();
    this.actualTime = channel.getActualTime();
    this.systemTime = channel.getSystemTime();
    this.informationSource = new InformationSourceDao(channel.getInformationSource());
    this.comment = channel.getComment();
    this.position = RelativePositionDao.from(channel.getPosition());
    this.aliases = channel.getAliases().stream()
        .map(ReferenceAliasDao::new)
        .collect(Collectors.toList());
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public UUID getEntityId() {
    return entityId;
  }

  public void setEntityId(UUID entityId) {
    this.entityId = entityId;
  }

  public UUID getVersionId() {
    return versionId;
  }

  public void setVersionId(UUID versionId) {
    this.versionId = versionId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ChannelDataType getDataType() {
    return dataType;
  }

  public void setDataType(ChannelDataType dataType) {
    this.dataType = dataType;
  }

  public ChannelBandType getBandType() {
    return bandType;
  }

  public void setBandType(ChannelBandType bandType) {
    this.bandType = bandType;
  }

  public ChannelInstrumentType getInstrumentType() {
    return instrumentType;
  }

  public void setInstrumentType(ChannelInstrumentType instrumentType) {
    this.instrumentType = instrumentType;
  }

  public ChannelOrientationType getOrientationType() {
    return orientationType;
  }

  public void setOrientationType(ChannelOrientationType orientationType) {
    this.orientationType = orientationType;
  }

  public char getOrientationCode() {
    return orientationCode;
  }

  public void setOrientationCode(char orientationCode) {
    this.orientationCode = orientationCode;
  }

  public String getLocationCode() {
    return locationCode;
  }

  public void setLocationCode(String locationCode) {
    this.locationCode = locationCode;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public double getElevation() {
    return elevation;
  }

  public void setElevation(double elevation) {
    this.elevation = elevation;
  }

  public double getDepth() {
    return depth;
  }

  public void setDepth(double depth) {
    this.depth = depth;
  }

  public double getVerticalAngle() {
    return verticalAngle;
  }

  public void setVerticalAngle(double verticalAngle) {
    this.verticalAngle = verticalAngle;
  }

  public double getHorizontalAngle() {
    return horizontalAngle;
  }

  public void setHorizontalAngle(double horizontalAngle) {
    this.horizontalAngle = horizontalAngle;
  }

  public Units getUnits() {
    return units;
  }

  public void setUnits(Units units) {
    this.units = units;
  }

  public double getNominalSampleRate() {
    return nominalSampleRate;
  }

  public void setNominalSampleRate(double nominalSampleRate) {
    this.nominalSampleRate = nominalSampleRate;
  }

  public Instant getActualTime() {
    return actualTime;
  }

  public void setActualTime(Instant actualTime) {
    this.actualTime = actualTime;
  }

  public Instant getSystemTime() {
    return systemTime;
  }

  public void setSystemTime(Instant systemTime) {
    this.systemTime = systemTime;
  }

  public InformationSourceDao getInformationSource() {
    return informationSource;
  }

  public void setInformationSource(InformationSourceDao informationSource) {
    this.informationSource = informationSource;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public RelativePositionDao getPosition() {
    return position;
  }

  public void setPosition(RelativePositionDao position) {
    this.position = position;
  }

  public List<ReferenceAliasDao> getAliases() {
    return aliases;
  }

  public void setAliases(List<ReferenceAliasDao> aliases) {
    this.aliases = aliases;
  }

  /**
   * Convert this DAO into its corresponding COI object.
   *
   * @return A ReferenceChannel COI object.
   */
  public ReferenceChannel toCoi() {
    List<ReferenceAlias> aliasList = this.aliases.stream()
        .map(ReferenceAliasDao::toCoi)
        .collect(Collectors.toList());

    return ReferenceChannel.builder()
        .setName(name)
        .setDataType(dataType)
        .setBandType(bandType)
        .setInstrumentType(instrumentType)
        .setOrientationType(orientationType)
        .setOrientationCode(orientationCode)
        .setLocationCode(locationCode)
        .setLatitude(latitude)
        .setLongitude(longitude)
        .setElevation(elevation)
        .setDepth(depth)
        .setVerticalAngle(verticalAngle)
        .setHorizontalAngle(horizontalAngle)
        .setUnits(units)
        .setNominalSampleRate(nominalSampleRate)
        .setActualTime(actualTime)
        .setSystemTime(systemTime)
        .setActive(true)
        .setInformationSource(informationSource.toCoi())
        .setComment(comment)
        .setPosition(position.toCoi())
        .setAliases(aliasList)
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReferenceChannelDao that = (ReferenceChannelDao) o;
    return id == that.id &&
        orientationCode == that.orientationCode &&
        Double.compare(that.latitude, latitude) == 0 &&
        Double.compare(that.longitude, longitude) == 0 &&
        Double.compare(that.elevation, elevation) == 0 &&
        Double.compare(that.depth, depth) == 0 &&
        Double.compare(that.verticalAngle, verticalAngle) == 0 &&
        Double.compare(that.horizontalAngle, horizontalAngle) == 0 &&
        Double.compare(that.nominalSampleRate, nominalSampleRate) == 0 &&
        Objects.equal(entityId, that.entityId) &&
        Objects.equal(versionId, that.versionId) &&
        Objects.equal(name, that.name) &&
        dataType == that.dataType &&
        bandType == that.bandType &&
        instrumentType == that.instrumentType &&
        orientationType == that.orientationType &&
        Objects.equal(locationCode, that.locationCode) &&
        units == that.units &&
        Objects.equal(actualTime, that.actualTime) &&
        Objects.equal(systemTime, that.systemTime) &&
        Objects.equal(informationSource, that.informationSource) &&
        Objects.equal(comment, that.comment) &&
        Objects.equal(position, that.position) &&
        Objects.equal(aliases, that.aliases);
  }

  @Override
  public int hashCode() {
    return Objects
        .hashCode(id, entityId, versionId, name, dataType, bandType, instrumentType,
            orientationType,
            orientationCode, locationCode, latitude, longitude, elevation, depth, verticalAngle,
            horizontalAngle, units, nominalSampleRate, actualTime, systemTime, informationSource,
            comment, position, aliases);
  }

  @Override
  public String toString() {
    return "ReferenceChannelDao{" +
        "id=" + id +
        ", entityId=" + entityId +
        ", versionId=" + versionId +
        ", name='" + name + '\'' +
        ", dataType=" + dataType +
        ", bandType=" + bandType +
        ", instrumentType=" + instrumentType +
        ", orientationType=" + orientationType +
        ", orientationCode=" + orientationCode +
        ", locationCode='" + locationCode + '\'' +
        ", latitude=" + latitude +
        ", longitude=" + longitude +
        ", elevation=" + elevation +
        ", depth=" + depth +
        ", verticalAngle=" + verticalAngle +
        ", horizontalAngle=" + horizontalAngle +
        ", units=" + units +
        ", nominalSampleRate=" + nominalSampleRate +
        ", actualTime=" + actualTime +
        ", systemTime=" + systemTime +
        ", informationSource=" + informationSource +
        ", comment='" + comment + '\'' +
        ", position=" + position +
        ", aliases=" + aliases +
        '}';
  }
}
