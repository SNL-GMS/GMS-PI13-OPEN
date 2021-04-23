package gms.shared.utilities.standardtestdataset.qcmaskconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.signaldetection.QcMask;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI that converts SME formatted JSONs for Qc Masks (deemed .smeware) to GMS COI format
 */
public class QcMaskConverter {

  private QcMaskConverter() {
  }

  private static final ObjectMapper mapper = CoiObjectMapperFactory.getJsonObjectMapper();

  public static List<QcMask> convertJsonToCoi(String masksFile) throws IOException {
    List<QcMask> qcMasks = new ArrayList<>();
    File preProcessingQcJson = new File(masksFile);
    PreProcessingQcMask[] preProcessingQcMasks = mapper
        .readValue(preProcessingQcJson, PreProcessingQcMask[].class);
    for (PreProcessingQcMask preMask : preProcessingQcMasks) {
      qcMasks.add(preMask.toQcMask());
    }
    return qcMasks;
  }
}
