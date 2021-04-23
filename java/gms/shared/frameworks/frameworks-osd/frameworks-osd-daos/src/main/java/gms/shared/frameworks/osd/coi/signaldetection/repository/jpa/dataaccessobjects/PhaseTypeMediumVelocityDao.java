package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.PhaseType;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.PhaseTypeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "phasetypemediumvelocity")
public class PhaseTypeMediumVelocityDao {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "phase")
  @Convert(converter = PhaseTypeConverter.class)
  private PhaseType phaseType;

  @Column(name = "velocity")
  private Double velocity;

  public PhaseTypeMediumVelocityDao() { }

  public PhaseTypeMediumVelocityDao(
      PhaseType phaseType, Double velocity) {
    this.phaseType = phaseType;
    this.velocity = velocity;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public PhaseType getPhaseType() {
    return phaseType;
  }

  public void setPhaseType(PhaseType phaseType) {
    this.phaseType = phaseType;
  }

  public Double getVelocity() {
    return velocity;
  }

  public void setVelocity(Double velocity) {
    this.velocity = velocity;
  }

  @Override
  public String toString() {
    return "PhaseTypeMediumVelocityDao{" +
        "id=" + id +
        ", phaseType=" + phaseType +
        ", velocity=" + velocity +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PhaseTypeMediumVelocityDao that = (PhaseTypeMediumVelocityDao) o;

    if (id != that.id) {
      return false;
    }
    if (phaseType != that.phaseType) {
      return false;
    }
    return velocity.equals(that.velocity);
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + phaseType.hashCode();
    result = 31 * result + velocity.hashCode();
    return result;
  }

}
