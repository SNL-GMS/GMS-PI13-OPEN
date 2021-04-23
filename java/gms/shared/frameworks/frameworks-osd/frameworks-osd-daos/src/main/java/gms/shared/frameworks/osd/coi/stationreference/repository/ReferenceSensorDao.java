package gms.shared.frameworks.osd.coi.stationreference.repository;

import gms.shared.frameworks.osd.coi.emerging.provenance.repository.InformationSourceDao;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSensor;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.commons.lang3.Validate;

@Entity
@Table(name = "reference_sensor")
public class ReferenceSensorDao {

  @Id
  @Column(unique = true)
  private UUID id;

  @Column(name = "channel_name")
  private String channelName;

  @Column(name = "instrument_manufacturer")
  private String instrumentManufacturer;

  @Column(name = "instrument_model")
  private String instrumentModel;

  @Column(name = "serial_number")
  private String serialNumber;

  @Column(name = "number_of_components")
  private int numberOfComponents;

  @Column(name = "corner_period")
  private double cornerPeriod;

  @Column(name = "low_passband")
  private double lowPassband;

  @Column(name = "high_passband")
  private double highPassband;

  @Column(name = "actual_time")
  private Instant actualTime;

  @Column(name = "system_time")
  private Instant systemTime;

  @Column(name = "comment")
  private String comment;

  @Embedded
  private InformationSourceDao informationSource;

  /**
   * Default constructor for JPA.
   */
  public ReferenceSensorDao() {
  }

  /**
   * Create a DAO from the COI object.
   *
   * @param sensor The ReferenceSensor object.
   */
  public ReferenceSensorDao(ReferenceSensor sensor) throws NullPointerException {
    Validate.notNull(sensor);
    this.id = sensor.getId();
    this.channelName = sensor.getChannelName();
    this.instrumentManufacturer = sensor.getInstrumentManufacturer();
    this.instrumentModel = sensor.getInstrumentModel();
    this.serialNumber = sensor.getSerialNumber();
    this.numberOfComponents = sensor.getNumberOfComponents();
    this.cornerPeriod = sensor.getCornerPeriod();
    this.lowPassband = sensor.getLowPassband();
    this.highPassband = sensor.getHighPassband();
    this.actualTime = sensor.getActualTime();
    this.systemTime = sensor.getSystemTime();
    this.comment = sensor.getComment();
    this.informationSource = new InformationSourceDao(sensor.getInformationSource());
  }

  /**
   * Convert this DAO into its corresponding COI object.
   *
   * @return A ReferenceSensor COI object.
   */
  public ReferenceSensor toCoi() {
    return ReferenceSensor.builder()
        .setChannelName(getChannelName())
        .setInstrumentManufacturer(getInstrumentManufacturer())
        .setInstrumentModel(getInstrumentModel())
        .setSerialNumber(getSerialNumber())
        .setNumberOfComponents(getNumberOfComponents())
        .setCornerPeriod(getCornerPeriod())
        .setLowPassband(getLowPassband())
        .setHighPassband(getHighPassband())
        .setActualTime(getActualTime())
        .setSystemTime(getSystemTime())
        .setInformationSource(getInformationSource().toCoi())
        .setComment(getComment())
        .build();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getChannelName() {
    return channelName;
  }

  public void setChannelName(String channelName) {
    this.channelName = channelName;
  }

  public String getInstrumentManufacturer() {
    return instrumentManufacturer;
  }

  public void setInstrumentManufacturer(String instrumentManufacturer) {
    this.instrumentManufacturer = instrumentManufacturer;
  }

  public String getInstrumentModel() {
    return instrumentModel;
  }

  public void setInstrumentModel(String instrumentModel) {
    this.instrumentModel = instrumentModel;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public void setSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
  }

  public int getNumberOfComponents() {
    return numberOfComponents;
  }

  public void setNumberOfComponents(int numberOfComponents) {
    this.numberOfComponents = numberOfComponents;
  }

  public double getCornerPeriod() {
    return cornerPeriod;
  }

  public void setCornerPeriod(double cornerPeriod) {
    this.cornerPeriod = cornerPeriod;
  }

  public double getLowPassband() {
    return lowPassband;
  }

  public void setLowPassband(double lowPassband) {
    this.lowPassband = lowPassband;
  }

  public double getHighPassband() {
    return highPassband;
  }

  public void setHighPassband(double highPassband) {
    this.highPassband = highPassband;
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

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public InformationSourceDao getInformationSource() {
    return informationSource;
  }

  public void setInformationSource(InformationSourceDao informationSource) {
    this.informationSource = informationSource;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ReferenceSensorDao sensorDao = (ReferenceSensorDao) o;

    if (numberOfComponents != sensorDao.numberOfComponents) {
      return false;
    }
    if (Double.compare(sensorDao.cornerPeriod, cornerPeriod) != 0) {
      return false;
    }
    if (Double.compare(sensorDao.lowPassband, lowPassband) != 0) {
      return false;
    }
    if (Double.compare(sensorDao.highPassband, highPassband) != 0) {
      return false;
    }
    if (id != null ? !id.equals(sensorDao.id) : sensorDao.id != null) {
      return false;
    }
    if (channelName != null ? !channelName.equals(sensorDao.channelName) :
        sensorDao.channelName != null) {
      return false;
    }
    if (instrumentManufacturer != null ? !instrumentManufacturer
        .equals(sensorDao.instrumentManufacturer) : sensorDao.instrumentManufacturer != null) {
      return false;
    }
    if (instrumentModel != null ? !instrumentModel.equals(sensorDao.instrumentModel)
        : sensorDao.instrumentModel != null) {
      return false;
    }
    if (serialNumber != null ? !serialNumber.equals(sensorDao.serialNumber)
        : sensorDao.serialNumber != null) {
      return false;
    }
    if (actualTime != null ? !actualTime.equals(sensorDao.actualTime)
        : sensorDao.actualTime != null) {
      return false;
    }
    if (systemTime != null ? !systemTime.equals(sensorDao.systemTime)
        : sensorDao.systemTime != null) {
      return false;
    }
    if (comment != null ? !comment.equals(sensorDao.comment) : sensorDao.comment != null) {
      return false;
    }
    return informationSource != null ? informationSource.equals(sensorDao.informationSource)
        : sensorDao.informationSource == null;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, channelName, instrumentManufacturer, instrumentModel, serialNumber,
        numberOfComponents, cornerPeriod, lowPassband, highPassband, actualTime, systemTime,
        comment, informationSource);
  }

  @Override
  public String toString() {
    return "ReferenceSensorDao{" +
        "id=" + id +
        ", channelId=" + channelName +
        ", instrumentManufacturer='" + instrumentManufacturer + '\'' +
        ", instrumentModel='" + instrumentModel + '\'' +
        ", serialNumber='" + serialNumber + '\'' +
        ", numberOfComponents=" + numberOfComponents +
        ", cornerPeriod=" + cornerPeriod +
        ", lowPassband=" + lowPassband +
        ", highPassband=" + highPassband +
        ", actualTime=" + actualTime +
        ", systemTime=" + systemTime +
        ", comment='" + comment + '\'' +
        ", informationSource=" + informationSource +
        '}';
  }
}
