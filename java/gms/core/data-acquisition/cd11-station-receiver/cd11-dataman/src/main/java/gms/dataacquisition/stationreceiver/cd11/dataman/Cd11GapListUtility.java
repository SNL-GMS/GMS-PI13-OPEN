package gms.dataacquisition.stationreceiver.cd11.dataman;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11GapList;
import gms.dataacquisition.stationreceiver.cd11.common.GapList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cd11GapListUtility {

  private static final Logger logger = LoggerFactory.getLogger(Cd11GapListUtility.class);


  private static final String GAP_STORAGE_PATH = "shared-volume/gaps/";
  private static final String FILE_EXTENSION = ".json";
  private static final ObjectMapper objectMapper;

  static {
    // Ensure that the fake gap storage path exists.
    File gapsDir = new File(GAP_STORAGE_PATH);
    if (!gapsDir.exists()) {
      gapsDir.mkdirs();
    }

    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  private Cd11GapListUtility() {

  }

  //GapList loaders/writers
  public static Cd11GapList loadGapState(String stationName) {
    Path path = Paths.get(GAP_STORAGE_PATH + stationName + FILE_EXTENSION);
    if (Files.exists(path)) {
      try {
        String contents = new String(Files.readAllBytes(path));
        return new Cd11GapList(objectMapper.readValue(contents, GapList.class));
      } catch (IOException e) {
        logger.error("Error deserializing GapList", e);
        return new Cd11GapList();
      }
    } else {
      return new Cd11GapList();
    }
  }

  public static void persistGapState(String stationName, GapList gapList)
      throws IOException {
    String path = GAP_STORAGE_PATH + stationName + FILE_EXTENSION;
    //todo no need for PrintWriter

    try {
      // Set file permissions.
      File file = new File(path);
      boolean permissionsSet = file.setReadable(true, false);
      permissionsSet = permissionsSet && file.setWritable(true, false);
      permissionsSet = permissionsSet && file.setExecutable(false, false);

      if (!permissionsSet) {
        logger.warn("Failed to set permissions on gaps file.");
      }
      objectMapper.writeValue(file, gapList);
    } catch (JsonGenerationException | JsonMappingException e) {
      logger.error("Failed to persist gap state:", e);
    }

  }

  public static void clearGapState(String stationName) {
    Path path = Paths.get(GAP_STORAGE_PATH + stationName + FILE_EXTENSION);
    try {
      Files.delete(path);
    } catch (IOException e) {
      logger.error("Failed to clear gap state", e);
    }
  }

}
