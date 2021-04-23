package gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.datatransferobjects;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFile;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileInvoiceMetadata;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileMetadataType;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileRawStationDataFrameMetadata;
import java.util.Map;
import java.util.Objects;

public class TransferredFileIdResolver extends TypeIdResolverBase {

  private static final Map<String, Class<?>> NAME_TO_CLASS = Map.of(
      TransferredFileMetadataType.RAW_STATION_DATA_FRAME.toString(),
          TransferredFileRawStationDataFrameMetadata.class,
      TransferredFileMetadataType.TRANSFERRED_FILE_INVOICE.toString(),
      TransferredFileInvoiceMetadata.class);

  @Override
  public Id getMechanism() {
    return Id.NAME;
  }

  @Override
  public String idFromValue(Object value) {
    return idFromValueAndType(value, value.getClass());
  }

  @Override
  public String idFromValueAndType(Object value, Class<?> suggestedType) {
    return ( (TransferredFile) value).getMetadataType().toString();
  }

  @Override
  public JavaType typeFromId(DatabindContext context, String id) {
    final Class<?> type = NAME_TO_CLASS.get(id);
    Objects.requireNonNull(type, "unknown type name for TransferredFile: " + id);
    return context.getTypeFactory().constructParametricType(
        TransferredFile.class, type);
  }
}