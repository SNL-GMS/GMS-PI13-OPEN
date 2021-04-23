package gms.shared.frameworks.osd.dao.channelsoh;

import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

/**
 * Define a Data Access Object to allow access to the relational database.
 */
@Entity
@Table(name = "channel_env_issue_boolean")
public class AcquiredChannelEnvironmentIssueBooleanDao extends AcquiredChannelEnvironmentIssueDao {

  @Column(name = "status", nullable = false)
  private boolean status;

  public boolean isStatus() {
    return status;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }

  public AcquiredChannelEnvironmentIssueBoolean toCoi() {
    return AcquiredChannelEnvironmentIssueBoolean.from(
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
    AcquiredChannelEnvironmentIssueBooleanDao that = (AcquiredChannelEnvironmentIssueBooleanDao) o;
    return status == that.status &&
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
    return "AcquiredChannelEnvironmentIssueBooleanDao{" +
        "id=" + id +
        ", channelId='" + channel + '\'' +
        ", type=" + type +
        ", startTime=" + startTime +
        ", endTime=" + endTime +
        ", status=" + status +
        '}';
  }
}
