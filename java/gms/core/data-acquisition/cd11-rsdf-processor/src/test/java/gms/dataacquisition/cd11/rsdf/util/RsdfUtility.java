package gms.dataacquisition.cd11.rsdf.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import java.io.IOException;
import java.net.URL;

public class RsdfUtility {

  private static final ObjectMapper jsonObjectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  public static RawStationDataFrame getRawStationDataFrame(String fileName) throws IOException {
    URL testFrameUrl = Thread.currentThread().getContextClassLoader().getResource(fileName);
    return jsonObjectMapper.readValue(testFrameUrl, RawStationDataFrame.class);
  }
}
