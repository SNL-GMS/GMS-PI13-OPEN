package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.Response;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * JPA data access object for {@link Response}
 */

@Entity
@Table(name = "response")
public class ResponseDao {

  @Id
  private UUID id;

  @OneToOne
  @JoinColumn(name = "channel_name", referencedColumnName = "name", unique = true)
  private ChannelDao channel;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinTable(name = "response_calibrations",
      joinColumns = {@JoinColumn(name = "response_id")},
      inverseJoinColumns = {@JoinColumn(name = "calibration_id")
      })
  private CalibrationDao calibration;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinTable(name = "response_frequency_amplitude_phase",
      joinColumns = {@JoinColumn(name = "response_id")},
      inverseJoinColumns = {@JoinColumn(name = "frequency_amplitude_phase_id")
      })
  private FrequencyAmplitudePhaseDao frequencyAmplitudePhase;

  protected ResponseDao() {
  }

  public ResponseDao(Response response, ChannelDao chan) {
    Objects.requireNonNull(response, "Cannot create ResponseDao from null Response");
    this.id = UUID.randomUUID();
    this.channel = Objects.requireNonNull(chan, "Cannot create ResponseDao from null channel");
    this.calibration = CalibrationDao.from(response.getCalibration());
    this.frequencyAmplitudePhase = response.getFapResponse()
        .map(FrequencyAmplitudePhaseDao::from).orElse(null);
  }

  /**
   * Create a COI from a DAO
   */
  public Response toCoi() {
    return Response.from(
        this.channel.getName(),
        this.calibration.toCoi(),
        getFrequencyAmplitudePhase().map(FrequencyAmplitudePhaseDao::toCoi).orElse(null));
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ChannelDao getChannel() {
    return channel;
  }

  public void setChannel(ChannelDao channel) {
    this.channel = channel;
  }

  public CalibrationDao getCalibration() {
    return calibration;
  }

  public void setCalibration(
      CalibrationDao calibration) {
    this.calibration = calibration;
  }

  public Optional<FrequencyAmplitudePhaseDao> getFrequencyAmplitudePhase() {
    return Optional.ofNullable(frequencyAmplitudePhase);
  }

  public void setFrequencyAmplitudePhase(
      FrequencyAmplitudePhaseDao frequencyAmplitudePhase) {
    this.frequencyAmplitudePhase = frequencyAmplitudePhase;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResponseDao that = (ResponseDao) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(channel, that.channel) &&
        Objects.equals(calibration, that.calibration) &&
        Objects.equals(frequencyAmplitudePhase, that.frequencyAmplitudePhase);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, channel, calibration, frequencyAmplitudePhase);
  }

  @Override
  public String toString() {
    return "ResponseDao{" +
        "id=" + id +
        ", channel=" + channel +
        ", calibration=" + calibration +
        ", frequencyAmplitudePhase=" + frequencyAmplitudePhase +
        '}';
  }
}
