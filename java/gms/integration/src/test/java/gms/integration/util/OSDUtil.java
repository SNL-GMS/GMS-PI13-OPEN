package gms.integration.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.integration.util.HttpUtility.Response;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import org.apache.commons.lang3.Validate;

public class OSDUtil {

  private static final ObjectMapper OBJECT_MAPPER = CoiObjectMapperFactory.getJsonObjectMapper();

  /**
   * Determines which stations in the specified collection are not currently stored in the OSD.
   * @param stations a collection containing the stations.
   * @return a collection of those stations not in the OSD
   * @throws IOException
   */
  public static Collection<Station> stationsNotInOSD(Collection<Station> stations)
  throws IOException {

    if (stations == null || stations.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> stationNames = stations.stream()
        .map(Station::getName)
        .collect(Collectors.toList());

    Collection<Station> stationsInOSD = getStationsByName(stationNames);

    final Set<String> namesInOSD = stationsInOSD.stream()
        .map(Station::getName)
        .collect(Collectors.toSet());

    return stations.stream()
        .filter(s -> !namesInOSD.contains(s.getName()))
        .collect(Collectors.toList());
  }

  /**
   * Determines which station groups in the specified collection are not currently stored in the OSD.
   * @param stationGroups a collection containing the station groups.
   * @return a collection of those station groups not in the OSD
   * @throws IOException
   */
  public static Collection<StationGroup> stationGroupsNotInOSD(
      Collection<StationGroup> stationGroups)

      throws IOException {

    if (stationGroups == null || stationGroups.isEmpty()) {
      return Collections.emptyList();
    }

    Collection<StationGroup> stationGroupsInOSD = getStationGroupsByName(
        stationGroups.stream()
            .map(StationGroup::getName)
            .collect(Collectors.toList()));

    final Set<String> namesInOSD = stationGroupsInOSD.stream()
        .map(StationGroup::getName)
        .collect(Collectors.toSet());

    return stationGroups.stream()
        .filter(s -> !namesInOSD.contains(s.getName()))
        .collect(Collectors.toList());
  }

  /**
   * Stores stations in the OSD
   * @param stations a list of stations which must not be null.
   * @throws IOException if unsuccessful. On success, no exception is thrown.
   */
  public static void storeStations(Collection<Station> stations) throws IOException {
    Validate.notNull(stations);

    // Trivial case.
    if (stations.isEmpty()) {
      return;
    }

    final String endpointUrl = stationStorageEndpointPath();

    String jsonStations = OBJECT_MAPPER.writeValueAsString(stations);

    Response response = HttpUtility.postJSONToEndpoint(endpointUrl, jsonStations);

    if (response.code / 100 != 2) {
      throw new IOException(String.format("OSD service returned status %d: %s",
          response.code,
          (response.entity.length() < 1000 ? response.entity : response.entity.substring(0, 1000))));
    }
  }

  /**
   * Stores station groups in the OSD
   * @param stationGroups a list of station groups, which must not be null.
   * @throws IOException if unsuccessful. On success, no exception is thrown.
   */
  public static void storeStationGroups(Collection<StationGroup> stationGroups) throws IOException {
    Validate.notNull(stationGroups);

    // Trivial case.
    if (stationGroups.isEmpty()) {
      return;
    }

    final String endpointUrl = stationGroupStorageEndpointPath();

    String jsonStationGroups = OBJECT_MAPPER.writeValueAsString(stationGroups);

    Response response = HttpUtility.postJSONToEndpoint(endpointUrl, jsonStationGroups);

    if (response.code / 100 != 2) {
      throw new IOException(String.format("OSD service returned status %d: %s",
          response.code,
          (response.entity.length() < 1000 ? response.entity : response.entity.substring(0, 1000))));
    }
  }

  /**
   * Fetch a list of stations by name.
   * @param stationNames a list containing the station names.
   * @return a list of station objects.
   * @throws IOException
   */
  public static List<Station> getStationsByName(Collection<String> stationNames)
      throws IOException {
    return getStationsByName(stationNames, null);
  }

  /**
   * Private version of getStationsByName that permits return of the response in a reference
   * argument. This may be useful for debugging in some cases.
   * @param stationNames
   * @param responseRef
   * @return
   * @throws IOException
   */
  private static List<Station> getStationsByName(
      Collection<String> stationNames, AtomicReference<Response> responseRef)
    throws IOException {
    return getByIdentifier(stationNames, Station.class, stationsEndpointPath(), responseRef);
  }

  /**
   * Fetch a list of stations by name.
   * @param stationGroupNames a list containing the station group names.
   * @return a list of station objects.
   * @throws IOException
   */
  public static List<StationGroup> getStationGroupsByName(Collection<String> stationGroupNames)
      throws IOException {
    return getStationGroupsByName(stationGroupNames, null);
  }

  /**
   * Private version of getStationGroupsByName to permits return of the response in a reference
   * parameter. This may be useful for debugging purposes.
   * @param stationGroupNames
   * @param responseRef
   * @return
   * @throws IOException
   */
  private static List<StationGroup> getStationGroupsByName(
      Collection<String> stationGroupNames,
      AtomicReference<Response> responseRef) throws IOException {
    return getByIdentifier(stationGroupNames, StationGroup.class, stationGroupsEndpointPath(),
        responseRef);
  }

  /**
   * Get a list of T objects by their identifiers.
   * @param identifiers a collection containing the identifiers such as names or uuids.
   * @param clzz the class of the objects.
   * @param endpointUrl the url to call.
   * @param responseRef if non-null, the response from the http call is stored in this
   *   reference object.
   * @param <T>
   * @return a list of T objects.
   * @throws IOException
   */
  private static <T> List<T> getByIdentifier(Collection<?> identifiers,
      Class<? extends T> clzz,
      String endpointUrl,
      AtomicReference<HttpUtility.Response> responseRef) throws IOException {

    HttpUtility.Response response = HttpUtility
        .postJSONToEndpoint(endpointUrl, arrayString(identifiers));

    if (responseRef != null) {
      responseRef.set(response);
    }

    if (response.code / 100 == 2) {

      ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

      try (StringReader reader = new StringReader(response.entity);
          JsonReader jsonReader = Json.createReader(reader)) {

        JsonArray jsonArray = jsonReader.readArray();
        final int sz = jsonArray.size();

        List<T> result = new ArrayList<>(sz);

        for (int i = 0; i < sz; i++) {
          result.add(objectMapper.readValue(jsonArray.getJsonObject(i).toString(),
              clzz));
        }

        return result;
      }

    } else {
      throw new IOException(String.format("OSD service returned status %d: %s",
          response.code,
          (response.entity.length() < 1000 ? response.entity : response.entity.substring(0, 1000)))
      );
    }
  }

  /**
   * Returns the URL to the stations endpoint.
   * @return
   */
  private static String stationsEndpointPath() {
    return ServiceUtility.getServiceURL(ServiceUtility.FRAMEWORKS_OSD_SERVICE) + "/osd/stations";
  }

  /**
   * Returns a string version of the URL to which to post JSON for storing new stations.
   * @return a url string.
   */
  private static String stationStorageEndpointPath() {
    return stationsEndpointPath() + "/new";
  }

  /**
   * Returns a string version of the URL to which to post JSON for retrieving station groups
   * by name.
   * @return a url string.
   */
  private static String stationGroupsEndpointPath() {
    return ServiceUtility.getServiceURL(ServiceUtility.FRAMEWORKS_OSD_SERVICE) +
      "/osd/station-groups";
  }

  /**
   * Returns a string version of the URL to which to post JSON for storing new station groups.
   * @return a url string.
   */
  private static String stationGroupStorageEndpointPath() {
    return stationGroupsEndpointPath() + "/new";
  }

  /**
   * Utility method for generating a JSON array of strings.
   * @param objects a collection of simple objects such as strings, integers, or UUIDs.
   *   This method should not be used on complex objects that should be serialized into
   *   JSON objects.
   * @return
   */
  private static String arrayString(Collection<?> objects) {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    boolean first = true;
    for (Object ob : objects) {
      if (first) {
        first = false;
      } else {
        sb.append(',');
      }
      sb.append('"');
      sb.append(ob.toString());
      sb.append('"');
    }
    sb.append(']');
    return sb.toString();
  }

}
