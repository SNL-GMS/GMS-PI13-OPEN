package gms.shared.frameworks.osd.api.signaldetection;

import gms.shared.frameworks.common.ContentType;
import gms.shared.frameworks.osd.api.util.StationsTimeRangeRequest;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetection;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionHypothesis;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

public interface SignalDetectionRepositoryInterface {

  /**
   * Store for the first time the provided {@link SignalDetection} and all of its {@link
   * gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionHypothesis} and {@link
   * gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement} objects.
   *
   * @param signalDetections store these SignalDetections and its supporting hypotheses, not null
   */
  @Path("/signal-detections/new")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Stores a collection of signal detections")
  void storeSignalDetections(
      @RequestBody(description = "The Signal Detections to Store")
          Collection<SignalDetection> signalDetections);

  /**
   * Retrieves the list of {@link SignalDetection} with the supplied ids, or empty if not found.
   *
   * @param ids The UUIDs of the request SignalDetections
   * @return List of SignalDetections
   */
  @Path("/signal-detections/ids")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieves Signal Detections by their ids")
  List<SignalDetection> findSignalDetectionsByIds(
      @RequestBody(description = "Collection of Signal Detection Ids to retrieve")
          Collection<UUID> ids);

  /**
   * Retrieves the list of {@link SignalDetection} from the supplied stations and within the
   * provided time range, or empty if not found.
   * @param request The collection of station names and the time range
   * @return List of SignalDetections
   */
  @Path("/signal-detection/stations-time")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieves signal detections by station names and a time range")
  List<SignalDetection> findSignalDetectionsByStationAndTime(
      @RequestBody(description = "Collection of station names and a time range")
          StationsTimeRangeRequest request);

  /**
   * Stores the list of {@link SignalDetectionHypothesis} and all supporting obects
   * @param signalDetectionHypotheses The {@link SignalDetectionHypothesis} collection to store,
   * not null
   */
  @Path("/signal-detection-hypotheses/new")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Stores a collection of signal detection hypotheses")
  void storeSignalDetectionHypotheses(
      @RequestBody(description = "The signal detection hypotheses to store")
      Collection<SignalDetectionHypothesis> signalDetectionHypotheses);

}
