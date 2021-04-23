package gms.integration.util;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrameMetadata;
import io.cucumber.core.internal.gherkin.deps.com.google.gson.JsonArray;
import io.cucumber.core.internal.gherkin.deps.com.google.gson.JsonElement;
import io.cucumber.core.internal.gherkin.deps.com.google.gson.JsonObject;
import io.cucumber.core.internal.gherkin.deps.com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StepUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(StepUtils.class);

  // Maps Json objects to corresponding GMS objects
  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  // Keys for extracting the ACEI and Extract objs from the Extract Json objs
  private static final String ACQUISITION_METADATA_KEY = "acquisitionMetadata";
  private static final String ACQUIRED_CHANNEL_ENVIRONMENT_ISSUES_KEY = "acquiredChannelEnvironmentIssues";

  /**
   * Create SOH ACEI ojbect list from json element
   * @param jsonArray - JsonArray object
   */
  public static List<AcquiredChannelEnvironmentIssue> createACEIList(JsonArray jsonArray) {

    // AcquiredChannelEnvironmentIssue list of soh objects
    List<AcquiredChannelEnvironmentIssue> aceiList = new ArrayList<>();

    // Create the Acei array for comparison
    Iterator<JsonElement> iterator = jsonArray.iterator();

    while (iterator.hasNext()) {
      aceiList.add(createACEIObject(iterator.next()));
    }

    return aceiList;
  }


  // Create SOH ACEI object from JsonElement
  public static AcquiredChannelEnvironmentIssue createACEIObject(JsonElement jsonElement) {

    String aceiString = jsonElement.getAsJsonObject().toString();

    Optional<AcquiredChannelEnvironmentIssue> aceiOpt = null;
    try {
      aceiOpt = Optional.of(
              objectMapper.readValue(aceiString, AcquiredChannelEnvironmentIssue.class));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return aceiOpt.get();
  }

  /**
   * Create the JsonElement list from Kafka string messages
   * @param kafkaMessages - list of kafka string messages
   */
  public static List<JsonElement> createJsonElementList(List<String> kafkaMessages) {
    JsonParser msgParser = new JsonParser();
    return kafkaMessages.stream().
            map(msgParser::parse).
            collect(Collectors.toList());
  }


  /**
   * Create SOH Extract objects from json element
   * @param jsonElement - json element from file and/or kafka
   */
  public static AcquiredStationSohExtract createSohExtract(JsonElement jsonElement) throws Exception {

    // Initialize the ACEI array list
    List<RawStationDataFrameMetadata> rsdfMetaList = new ArrayList<>();
    List<AcquiredChannelEnvironmentIssue> aceiList = new ArrayList<>();

    // Get the acquisition metadata and acei lists
    JsonObject jsonObject = jsonElement.getAsJsonObject();
    JsonArray metadataArray = jsonObject.getAsJsonArray(ACQUISITION_METADATA_KEY);
    JsonArray aceiArray = jsonObject.getAsJsonArray(ACQUIRED_CHANNEL_ENVIRONMENT_ISSUES_KEY);

    // Iterators for metadata and acei objects
    Iterator<JsonElement> metaIterator = metadataArray.iterator();
    Iterator<JsonElement> aceiIterator = aceiArray.iterator();

    // Create the RSDF Metadata list to form the AcquiredStationSohExtract object
    while (metaIterator.hasNext()) {
      rsdfMetaList.add((RawStationDataFrameMetadata)
              StepUtils.createSohFromJson(metaIterator.next(), RawStationDataFrameMetadata.class).get());
    }

    // Create the ACEI list  to form the AcquiredStationSohExtract object
    while (aceiIterator.hasNext()) {
      aceiList.add((AcquiredChannelEnvironmentIssue)
              StepUtils.createSohFromJson(aceiIterator.next(), AcquiredChannelEnvironmentIssue.class).get());
    }

    return AcquiredStationSohExtract.create(rsdfMetaList, aceiList);
  }

  /**
   * Parse json file and extract json elements
   * @param - resource file for the given json message
   */
  public static JsonElement parseJsonResource(String resourceFile) throws Exception {
    URL url = StepUtils.class.getClassLoader().getResource(resourceFile);
    File jsonFile = Paths.get(url.toURI()).toFile();

    if (jsonFile == null) {
      return null;
    }

    JsonParser jsonParser = new JsonParser();
    try (FileReader reader = new FileReader(jsonFile)) {
      JsonElement jsonElement = jsonParser.parse(reader);

      return jsonElement;
    }
  }

  /**
   * Loads a list of JSON objects from a resource.
   *
   * @param jsonResourcePath the path to the resource.
   * @return an optional wrapping the list of JSON object (empty if a problem occurs).
   */
  public static <T> Optional<List<T>> readJsonListResource(String jsonResourcePath,
      Class<? extends T> clazz) throws IOException {

    InputStream is = StepUtils.class.getClassLoader()
        .getResourceAsStream(jsonResourcePath);

    if (is != null) {

      String json = null;

      try (Reader reader = new InputStreamReader(is)) {
        char[] buffer = new char[16384];
        StringBuilder sb = new StringBuilder();
        int read;
        while ((read = reader.read(buffer)) != -1) {
          sb.append(buffer, 0, read);
        }
        json = sb.toString();
      }

      ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

      // This typically comes back as a list of LinkedHashMaps, not instances of the class T.
      List<?> tempList = objectMapper.readValue(json, new TypeReference<List<T>>() {
      });

      try {
        // For each LinkedHashMap, convert to JSON, then back to an instance of T.
        List<T> finalList = tempList.stream()
            .map(ob -> {
              try {
                return objectMapper.readValue(objectMapper.writeValueAsString(ob), clazz);
              } catch (IOException e) {
                throw new IllegalArgumentException(e);
              }
            })
            .collect(Collectors.toList());
        return Optional.of(finalList);
      } catch (IllegalArgumentException e) {
        throw new IOException(e.getCause() != null ? e.getCause() : e);
      }

    } else {
      LOGGER.error("JSON resource does not exist: %s", jsonResourcePath);
    }

    return Optional.empty();
  }

  /**
   * Loads a list of JSON objects from a resource containing one JSON-serialized object
   * per line
   *
   * @param jsonResourcePath the path to the resource.
   * @param clazz the class of the objects.
   *
   * @return an optional wrapping the list of JSON object (empty if a problem occurs).
   */
  public static <T> Optional<List<T>> readJsonLines(String jsonResourcePath,
      Class<? extends T> clazz) throws IOException {

    InputStream is = StepUtils.class.getClassLoader().getResourceAsStream(jsonResourcePath);

    if (is != null) {

      String json = null;

      ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
      List<T> objectList = new ArrayList<>();

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

        String line = null;

        while((line = reader.readLine()) != null) {
          line = line.trim();
          if (!line.isEmpty()) {
            objectList.add(objectMapper.readValue(line, clazz));
          }
        }

      }

      return Optional.of(objectList);

    } else {
      LOGGER.error("JSON resource does not exist: %s", jsonResourcePath);
    }

    return Optional.empty();
  }

  /**
   * Tests whether a service is alive by calling the alive endpoint of the service.
   *
   * @param serviceRootURL the root url for the service. This should not be the full path to the
   * alive endpoint.
   * @return true if the service responds successfully to the call.
   */
  public static boolean isServiceAlive(final String serviceRootURL) {

    if (serviceRootURL == null || serviceRootURL.isEmpty()) {
      throw new IllegalArgumentException("serviceRootURL is required");
    }

    final String aliveURL = serviceRootURL + "/alive";

    try {

      HttpClient client = HttpClientBuilder.create().build();

      LOGGER.info("Posting isAlive request to: " + aliveURL);

      HttpGet request = new HttpGet(aliveURL);
      HttpResponse getResponse = client.execute(request);

      return getResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK;

    } catch (Exception e) {

      LOGGER.error("Error in isAlive post", e);

    }

    return false;
  }

  /**
   * This method will pass data to the supplied endpoint and return data from the endpoint
   *
   * @param serviceURL url to POST data to
   * @param data data to POST
   * @return returned object from endpoint or empty string if void is returned...null if status code != 200, false if call failed
   */
  /**
   * This method will pass data to the supplied endpoint and return data from the endpoint
   *
   * @param serviceURL url to POST data to
   * @param data data to POST
   * @return returned Optional containing object from endpoint or empty Optional if void is returned
   * @throws IllegalArgumentException data parameter does not unmarshall to String
   * @throws IOException call to remote endpoint fails
   */
  public static Optional<String> postDataToEndpoint(final String serviceURL, final Object data)
      throws IllegalArgumentException, IOException {

    if (serviceURL == null || serviceURL.isEmpty()) {
      throw new IllegalArgumentException("serviceRootURL is required");
    }

    ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
    String requestBody = objectMapper
        .writeValueAsString(data);
    StringEntity stringEntity = new StringEntity(requestBody,
        ContentType.APPLICATION_JSON);
    LOGGER.info("Posting request to: " + serviceURL);
    LOGGER.info("requestBody: " + requestBody);
    HttpPost request = new HttpPost(serviceURL);
    request.setEntity(stringEntity);
    request.addHeader("Content-Type", "application/json");
    request.addHeader("Accept", "application/json");
    HttpClient client = HttpClientBuilder.create().build();
    HttpResponse response = client.execute(request);

    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
      return null;
    }
    HttpEntity entity = response.getEntity();
    return Optional.of(EntityUtils.toString(entity));
  }

  /*
   * --------------------------------------------------------------
   * Private Methods
   * --------------------------------------------------------------
   */

  /**
   * Convert the JsonElement to the specified object
   * @param jsonElement - input json element to convert
   * @param clazz - class to cast to from json element
   */
  private static Optional<?> createSohFromJson(JsonElement jsonElement, Class<?> clazz)
          throws Exception {
    String metaString = jsonElement.getAsJsonObject().toString();

    Optional<?> metaOpt = Optional.of(objectMapper.readValue(metaString, clazz));

    return metaOpt;
  }
}

