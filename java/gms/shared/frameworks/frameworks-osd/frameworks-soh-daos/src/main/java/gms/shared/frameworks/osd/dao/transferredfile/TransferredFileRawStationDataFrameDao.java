package gms.shared.frameworks.osd.dao.transferredfile;

import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFile;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileRawStationDataFrameMetadata;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "transferred_file_raw_station_data_frame")
public class TransferredFileRawStationDataFrameDao extends
    TransferredFileDao<TransferredFileRawStationDataFrameMetadata> {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transferred_rsdf_sequence")
  @SequenceGenerator(name = "transferred_rsdf_sequence", sequenceName = "transferred_rsdf_sequence", allocationSize = 5)
  private long id;

  @Embedded
  private TransferredFileRawStationDataFrameMetadataDao metadata;

  public TransferredFileRawStationDataFrameDao() {

  }

  public TransferredFileRawStationDataFrameDao(
      TransferredFile<TransferredFileRawStationDataFrameMetadata> tf) {
    super(tf);
    this.metadata = new TransferredFileRawStationDataFrameMetadataDao(tf.getMetadata());
  }

  @Override
  public TransferredFileRawStationDataFrameMetadata getMetadataCoi() {
    return this.metadata.toCoi();
  }

  public TransferredFileRawStationDataFrameMetadataDao getMetadata() {
    return this.metadata;
  }

  public void setMetadata(
      TransferredFileRawStationDataFrameMetadataDao metadata) {
    this.metadata = metadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    TransferredFileRawStationDataFrameDao that = (TransferredFileRawStationDataFrameDao) o;
    return id == that.id &&
        Objects.equals(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), id, metadata);
  }

  @Override
  public String toString() {
    return "TransferredFileRawStationDataFrameDao{" +
        "metadata=" + metadata +
        '}';
  }
}
