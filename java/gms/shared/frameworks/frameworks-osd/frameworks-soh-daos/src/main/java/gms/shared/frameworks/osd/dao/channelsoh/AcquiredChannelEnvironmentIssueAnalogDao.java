package gms.shared.frameworks.osd.dao.channelsoh;

import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

/**
 * Define a Data Access Object to allow access to the relational database.
 */
@Entity
@Table(name = "channel_env_issue_analog")
public class AcquiredChannelEnvironmentIssueAnalogDao extends AcquiredChannelEnvironmentIssueDao{

  @Column(name = "status", nullable = false)
  private double status;

  public double getStatus() {
    return status;
  }

  public void setStatus(double status) {
    this.status = status;
  }

  public AcquiredChannelEnvironmentIssueAnalog toCoi() {
    return AcquiredChannelEnvironmentIssueAnalog.from(
        this.id, this.channel.getName(), this.type,
        this.startTime, this.endTime, this.status);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AcquiredChannelEnvironmentIssueAnalogDao that = (AcquiredChannelEnvironmentIssueAnalogDao) o;
    return Double.compare(that.status, status) == 0 &&
        id.equals(that.id) &&
        channel.equals(that.channel) &&
        type == that.type &&
        startTime.equals(that.startTime) &&
        endTime.equals(that.endTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, channel, type, startTime, endTime, status);
  }

  @Override
  public String toString() {
    return "AcquiredChannelSohAnalogDao{" +
        "id=" + id +
        ", channelId='" + channel + '\'' +
        ", type=" + type +
        ", startTime=" + startTime +
        ", endTime=" + endTime +
        ", status=" + status +
        '}';
  }
}
