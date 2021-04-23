package gms.shared.frameworks.osd.dao.transferredfile;

import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFile;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileInvoiceMetadata;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "transferred_file_invoice")
public class TransferredFileInvoiceDao extends TransferredFileDao<TransferredFileInvoiceMetadata> {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transferred_file_invoice_sequence")
  @SequenceGenerator(name = "transferred_file_invoice_sequence", sequenceName = "transferred_file_invoice_sequence", allocationSize = 5)
  private long id;

  @Embedded
  private TransferredFileInvoiceMetadataDao metadata;

  public TransferredFileInvoiceDao() {
  }

  public TransferredFileInvoiceDao(TransferredFile<TransferredFileInvoiceMetadata> tf) {
    super(tf);
    this.metadata = new TransferredFileInvoiceMetadataDao(tf.getMetadata());
  }

  @Override
  public TransferredFileInvoiceMetadata getMetadataCoi() {
    return this.metadata.toCoi();
  }

  public TransferredFileInvoiceMetadataDao getMetadata() {
    return this.metadata;
  }

  public void setMetadata(
      TransferredFileInvoiceMetadataDao metadata) {
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
    TransferredFileInvoiceDao that = (TransferredFileInvoiceDao) o;
    return Objects.equals(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), metadata);
  }

  @Override
  public String toString() {
    return "TransferredFileInvoiceDao{" +
        "metadata=" + metadata +
        '}';
  }
}


