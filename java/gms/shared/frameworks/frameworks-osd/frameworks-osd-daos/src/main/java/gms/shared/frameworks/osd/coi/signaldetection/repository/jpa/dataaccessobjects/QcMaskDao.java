package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.QcMask;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Dao equivalent of {@link QcMask},
 * used to perform storage and retrieval operations on the QcMask via JPA.
 */
@Entity
@Table(name = "qc_masks")
public class QcMaskDao {

  @Id
  @Column(name = "id")
  private UUID id;

  @ManyToOne(cascade = CascadeType.MERGE)
  @JoinTable(name = "qc_mask_channels",
    joinColumns = {@JoinColumn(name = "qc_mask_id", referencedColumnName = "id")},
    inverseJoinColumns = {@JoinColumn(name = "channel_name", referencedColumnName = "name")})
  private ChannelDao channel;

  public QcMaskDao() {
    // Empty constructor needed for JPA
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QcMaskDao qcMaskDao = (QcMaskDao) o;
    return Objects.equals(id, qcMaskDao.id) &&
        Objects.equals(channel, qcMaskDao.channel);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id, channel);
  }

  @Override
  public String toString() {
    return "QcMaskDao{" +
        ", id=" + id +
        ", channel=" + channel.toString() +
        '}';
  }
}
