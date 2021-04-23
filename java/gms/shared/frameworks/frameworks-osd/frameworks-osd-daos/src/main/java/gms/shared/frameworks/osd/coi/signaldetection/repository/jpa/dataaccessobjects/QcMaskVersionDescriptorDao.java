package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "qcmask_version_descriptors")
public class QcMaskVersionDescriptorDao {

  @Id
  @GeneratedValue
  private long id;

  @Column(name = "qc_mask_id", updatable = false)
  private UUID qcMaskId;

  @Column(name = "qc_mask_version_id")
  private long qcMaskVersionId;

  public QcMaskVersionDescriptorDao() {
  }

  public QcMaskVersionDescriptorDao(
      UUID qcMaskId,
      long qcMaskVersionId) {
    this.qcMaskId = qcMaskId;
    this.qcMaskVersionId = qcMaskVersionId;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public UUID getQcMaskId() {
    return qcMaskId;
  }

  public void setQcMaskId(
      UUID qcMaskId) {
    this.qcMaskId = qcMaskId;
  }

  public long getQcMaskVersionId() {
    return qcMaskVersionId;
  }

  public void setQcMaskVersionId(
      long qcMaskVersionId) {
    this.qcMaskVersionId = qcMaskVersionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QcMaskVersionDescriptorDao that = (QcMaskVersionDescriptorDao) o;
    return id == that.id &&
        qcMaskVersionId == that.qcMaskVersionId &&
        Objects.equals(qcMaskId, that.qcMaskId);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id, qcMaskId, qcMaskVersionId);
  }

  @Override
  public String toString() {
    return "QcMaskVersionDescriptorDao{" +
        "id=" + id +
        ", qcMaskId=" + qcMaskId +
        ", qcMaskVersionId=" + qcMaskVersionId +
        '}';
  }
}
