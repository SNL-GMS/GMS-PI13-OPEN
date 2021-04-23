package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "channel_configuredinputs")
public class ChannelConfiguredInputsDao {

  @Id
  @GeneratedValue
  private int id;

  @ManyToOne
  @JoinColumn(
      referencedColumnName = "name",
      name = "channel_name",
      nullable = false
  )
  private ChannelDao channelName;

  @ManyToOne
  @JoinColumn(
      referencedColumnName = "name",
      name = "related_channel_name",
      nullable = false
  )
  private ChannelDao relatedChannelName;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public ChannelDao getChannelName() {
    return channelName;
  }

  public void setChannelName(
      ChannelDao channelName) {
    this.channelName = channelName;
  }

  public ChannelDao getRelatedChannelName() {
    return relatedChannelName;
  }

  public void setRelatedChannelName(
      ChannelDao relatedChannelName) {
    this.relatedChannelName = relatedChannelName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChannelConfiguredInputsDao that = (ChannelConfiguredInputsDao) o;
    return channelName.equals(that.channelName) &&
        relatedChannelName.equals(that.relatedChannelName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channelName, relatedChannelName);
  }

  @Override
  public String toString() {
    return "ChannelConfiguredInputsDao{" +
        "channelName='" + channelName + '\'' +
        ", relatedChannelName='" + relatedChannelName + '\'' +
        '}';
  }
}
