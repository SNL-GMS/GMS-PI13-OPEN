package gms.integration.util;

import static java.util.Map.entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public class ServiceUtility {

  public static final String SIGNAL_DETECTION_ASSOCIATION_CONTROL_SERVICE =
          "signal-detection-association-control-service";
  public static final String AMPLITUDE_CONTROL_SERVICE = "amplitude-control-service";
  public static final String BEAM_CONTROL_SERVICE = "beam-control-service";
  public static final String EVENT_LOCATION_CONTROL_SERVICE = "event-location-control-service";
  public static final String EVENT_MAGNITUDE_SERVICE = "event-magnitude-service";
  public static final String FILTER_CONTROL_SERVICE = "filter-control-service";
  public static final String FP_SERVICE = "fp-service";
  public static final String FK_CONTROL_SERVICE = "fk-control-service";
  public static final String ETCD_SERVICE = "etcd-service";
  public static final String FRAMEWORKS_OSD_SERVICE = "frameworks-osd-service";
  public static final String FRAMEWORKS_CONFIGURATION_SERVICE = "frameworks-configuration-service";
  public static final String OSD_SIGNALDETECTION_REPOSITORY_SERVICE =
          "osd-signaldetection-repository-service";
  public static final String OSD_STATIONREFERENCE_COI_SERVICE = "osd-stationreference-coi-service";
  public static final String OSD_WAVEFORMS_REPOSITORY_SERVICE = "osd-waveforms-repository-service";
  public static final String QC_CONTROL_SERVICE = "qc-control-service";
  public static final String SIGNAL_DETECTOR_CONTROL_SERVICE = "signal-detector-control-service";

  public static final String CD11_RSDF_PROCESSOR = "cd11-rsdf-processor";
  public static final String DATAMAN = "da-dataman";

  public static final String STATION_SOH_CONTROL_APPLICATION = "soh-control";

  public static final String KAFKA_BOOTSTRAP_SERVERS = "kafka-bootstrap-servers";

  // Environment variables used to specify the host name. Some people have been using
  // upper case and some lower case. Let either work with UC taking the priority.
  public static final String GMS_HOSTNAME = "GMS_HOSTNAME";
  public static final String GMS_HOSTNAME_LC = "gms_hostname";
  // Default value to use for the host name if neither environment variable is defined.
  public static final String LOCALHOST = "localhost";

  public static final String DIND = "dind";
  public static final String URL_PREFIX = "http://";

  private static final Map<String, Integer> SERVICE_PORT_MAPPINGS = Map.ofEntries(

          entry(SIGNAL_DETECTION_ASSOCIATION_CONTROL_SERVICE, 8080),
          entry(AMPLITUDE_CONTROL_SERVICE, 8081),
          entry(BEAM_CONTROL_SERVICE, 8082),
          entry(EVENT_LOCATION_CONTROL_SERVICE, 8083),
          entry(EVENT_MAGNITUDE_SERVICE, 8084),
          entry(FILTER_CONTROL_SERVICE, 8085),
          entry(FP_SERVICE, 8086),
          entry(FK_CONTROL_SERVICE, 8087),
          entry(FRAMEWORKS_OSD_SERVICE, 8088),
          entry(OSD_SIGNALDETECTION_REPOSITORY_SERVICE, 8089),
          entry(OSD_STATIONREFERENCE_COI_SERVICE, 8090),
          entry(OSD_WAVEFORMS_REPOSITORY_SERVICE, 8091),
          entry(QC_CONTROL_SERVICE, 8092),
          entry(SIGNAL_DETECTOR_CONTROL_SERVICE, 8093),
          entry(STATION_SOH_CONTROL_APPLICATION, 8080),
          entry(CD11_RSDF_PROCESSOR, 8095),
          entry(DATAMAN, 8107),
          entry(KAFKA_BOOTSTRAP_SERVERS, 9094)
  );

  public static Integer DOCKER_PORT = null;

  /**
   * Get the port used by a service.
   * @param serviceName
   * @return
   */
  public static Optional<Integer> getPort(String serviceName) {
    return Optional.ofNullable(SERVICE_PORT_MAPPINGS.get(serviceName));
  }

  /**
   * Get the names of all defined services.
   * @return an unmodifiable set of the service names.
   */
  public static Set<String> getDefinedServices() {
    return Collections.unmodifiableSet(SERVICE_PORT_MAPPINGS.keySet());
  }

  /**
   * Get the host name, which is defined by the environment variables GMS_HOSTNAME or
   * gms_hostname with GMS_HOSTNAME winning if both are defined. If neither are defined
   * this method defaults to localhost.
   * @return a host name, never null.
   */
  public static String getHostname() {
    return getHostname(System.getenv());
  }

  // Package private in order to unit test by passing in a modifiable map. System.getenv() returns
  // an unmodifiable map.
  static String getHostname(Map<String, String> env) {
    return env.getOrDefault(GMS_HOSTNAME, env.getOrDefault(GMS_HOSTNAME_LC, LOCALHOST));
  }

  /**
   * Get the URL for the specified service.
   * @param serviceName the name of the service
   * @return a string version of the URL.
   * @throws NoSuchElementException if the service is undefined.
   */
  public static String getServiceURL(String serviceName) {
    return getServiceURL(serviceName, getHostname());
  }

  // Package private to facilitate unit testing, since environment vars cannot be
  // modified via code.
  static String getServiceURL(String serviceName, String hostName) {
    int port;

    if (DOCKER_PORT != null) {
      port = DOCKER_PORT;
    } else {
      // Even if the port is not used, getting the port ensures it's a valid service name.
      port = getPort(serviceName).orElseThrow(() -> {
            return new NoSuchElementException("port for service not defined: " + serviceName);
          }
      );
    }

    return (hostName.equals(LOCALHOST) || hostName.equals(DIND)) ?
            // Port is used if localhost or dind
            String.format("%s%s:%d", URL_PREFIX, hostName, port) :
            // Port is not used if hitting a container on sandbox, release, etc.
            String.format("%s%s.%s", URL_PREFIX, serviceName, hostName);
  }

  /**
   * Utility for writing a list of JSON objects to a temp file.
   * @param objectList
   * @return
   * @throws IOException
   */
  public static String writeJsonToTempFile(List<?> objectList) throws IOException {
    File tempFile = File.createTempFile("svu", ".json");
    tempFile.deleteOnExit();
    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(tempFile,
        StandardCharsets.UTF_8)))) {
      ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
      for (Object obj: objectList) {
          pw.println(objectMapper.writeValueAsString(obj));
      }
    }
    return tempFile.getPath();
  }
 }
