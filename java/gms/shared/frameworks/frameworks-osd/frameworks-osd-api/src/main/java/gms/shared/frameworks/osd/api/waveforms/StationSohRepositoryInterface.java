package gms.shared.frameworks.osd.api.waveforms;

import gms.shared.frameworks.common.ContentType;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeSohTypeRequest;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * The interface for storing and retrieving COI objects.
 */
public interface StationSohRepositoryInterface {

  /**
   * Stores a collection of {@link AcquiredChannelEnvironmentIssueAnalog} state of health objects containing analog
   * values.
   *
   * @param acquiredChannelSohAnalogs The analog SOH objects to store.
   */
  @Path("/station-soh/acquired-channel-soh-analog/new")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Store an Acquired Channel Soh Analog object")
  void storeAcquiredChannelSohAnalog(
      @RequestBody(description = "AcquiredChannelSohAnalog objects to store")
          Collection<AcquiredChannelEnvironmentIssueAnalog> acquiredChannelSohAnalogs);

  /**
   * Stores a collection of {@link AcquiredChannelEnvironmentIssueBoolean} state of health objects containing boolean
   * values.
   *
   * @param acquiredChannelSohBooleans The boolean SOH objects to store.
   */
  @Path("/station-soh/acquired-channel-soh-boolean/store")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Store (and potentially update) AcquiredChannelSohBoolean objects")
  void storeAcquiredChannelEnvironmentIssueBoolean(
      @RequestBody(description = "AcquiredChannelSohBoolean objects to store and/or update")
          Collection<AcquiredChannelEnvironmentIssueBoolean> acquiredChannelSohBooleans);

  /**
   * Removes a collection of {@link AcquiredChannelEnvironmentIssueBoolean} state of health objects
   *
   * @param aceiBooleans the collection of boolean SOH objects to remove.
   */
  @Path("/station-soh/acquired-channel-soh-boolean/delete")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Removes given collection of AcquiredChannelEnvironmentIssueBoolean objects")
  void removeAcquiredChannelEnvironmentIssueBooleans(
      @RequestBody(description = "collection of AcquiredChannelEnvironmentIssueBoolean objects to remove")
          Collection<AcquiredChannelEnvironmentIssueBoolean> aceiBooleans);

  /**
   * Retrieves all {@link AcquiredChannelEnvironmentIssueAnalog} objects for the provided channel created within the
   * provided time range.
   *
   * @param request The collection of channel names and time range that will bound the {@link
   * AcquiredChannelEnvironmentIssueAnalog}s retrieved.
   * @return All SOH analog objects that meet the query criteria.
   */
  @Path("/station-soh/acquired-channel-soh-analog/by-channels-time")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieve all acquired channel soh analog data for the provided channel name and time range")
  List<AcquiredChannelEnvironmentIssueAnalog> retrieveAcquiredChannelEnvironmentIssueAnalogByChannelAndTimeRange(
      @RequestBody(description = "Channel name and time range bounding the acquired channel soh "
          + "analog data retrieved")
          ChannelTimeRangeRequest request);

  /**
   * Retrieves all {@link AcquiredChannelEnvironmentIssueAnalog} objects for the provided channel and {@link
   * AcquiredChannelEnvironmentIssueType}, created within the provided time range.
   *
   * @param request The channel name, type, and time range that will bound the {@link
   * AcquiredChannelEnvironmentIssueAnalog}s retrieved.
   * @return All SOH analog objects that meet the query criteria.
   */
  @Path("/station-soh/acquired-channel-soh-analog/by-channels-time-type")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieve all acquired channel soh analog for the provided channel name, " +
      "time range, and type")
  List<AcquiredChannelEnvironmentIssueAnalog> retrieveAcquiredChannelEnvironmentIssueAnalogByChannelTimeRangeAndType(
      @RequestBody(description = "Channel name, type, and time range bounding the acquired channel " +
          "soh analog data retrieved")
          ChannelTimeRangeSohTypeRequest request);

  /**
   * Retrieves all {@link AcquiredChannelEnvironmentIssueBoolean} objects for the provided channel created within the
   * provided time range.
   *
   * @param request The collection of channel names and time range that will bound the {@link
   * AcquiredChannelEnvironmentIssueBoolean}s retrieved.
   * @return All SOH boolean objects that meet the query criteria.
   */
  @Path("/station-soh/acquired-channel-soh-boolean/by-channels-time")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieve all acquired channel soh boolean data for the provided channel name and time range")
  List<AcquiredChannelEnvironmentIssueBoolean> retrieveAcquiredChannelEnvironmentIssueBooleanByChannelAndTimeRange(
      @RequestBody(description = "Channel name and time range bounding the acquired channel soh "
          + "boolean data retrieved")
          ChannelTimeRangeRequest request);

  /**
   * Retrieves all {@link AcquiredChannelEnvironmentIssueBoolean} objects for the provided channel, {@link
   * AcquiredChannelEnvironmentIssueType}, created within the provided time range.
   *
   * @param request The collection of channel names and time range that will bound the {@link
   * AcquiredChannelEnvironmentIssueBoolean}s retrieved.
   * @return All SOH boolean objects that meet the query criteria.
   */
  @Path("/station-soh/acquired-channel-soh-boolean/by-channels-time-type")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieve all acquired channel soh boolean data for the provided channel " +
      "name, time range, and type")
  List<AcquiredChannelEnvironmentIssueBoolean> retrieveAcquiredChannelSohBooleanByChannelTimeRangeAndType(
      @RequestBody(description = "Channel name, type, and time range bounding the acquired channel " +
          "soh analog data retrieved")
          ChannelTimeRangeSohTypeRequest request);

  /**
   * Retrieve the {@link AcquiredChannelEnvironmentIssueAnalog} with the provided id.  Returns an empty {@link Optional}
   * if no AcquiredChannelSohAnalog has that id.
   *
   * @param acquiredChannelEnvironmentIssueId id for the AcquiredChannelSohAnalog, not null
   * @return Optional AcquiredChannelSohAnalog object with the provided id, not null
   */
  @Path("/station-soh/acquired-channel-soh-analog/by-id")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieve acquired channel soh analog data for the provided id")
  Optional<AcquiredChannelEnvironmentIssueAnalog> retrieveAcquiredChannelEnvironmentIssueAnalogById(
      @RequestBody(description = "Id for acquired channel environment issue analog data to retrieve")
          UUID acquiredChannelEnvironmentIssueId);

  /**
   * Retrieve the {@link AcquiredChannelEnvironmentIssueBoolean} with the provided id.  Returns an empty {@link
   * Optional} if no AcquiredChannelSohBoolean has that id.
   *
   * @param acquiredChannelEnvironmentIssueId id for the AcquiredChannelSohBoolean, not null
   * @return Optional AcquiredChannelSohBoolean object with the provided id, not null
   */
  @Path("/station-soh/acquired-channel-soh-boolean/by-id")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieve acquired channel soh boolean for the provided id")
  Optional<AcquiredChannelEnvironmentIssueBoolean> retrieveAcquiredChannelEnvironmentIssueBooleanById(
      @RequestBody(description = "Id for acquired channel environment issue boolean data to retrieve")
          UUID acquiredChannelEnvironmentIssueId);

  /**
   * Retrieve the {@link AcquiredChannelEnvironmentIssueAnalog} with the provided id.  Returns an empty {@link Optional}
   * if no AcquiredChannelSohAnalog has that id.
   *
   * @param request time range request to find AcquiredChannelEnvironmentIssueAnalogs by, not null
   * @return Optional AcquiredChannelSohAnalog object with the provided id, not null
   */
  @Path("/station-soh/acquired-channel-soh-analog/by-time")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieve acquired channel soh analog data for the provided time range")
  List<AcquiredChannelEnvironmentIssueAnalog> retrieveAcquiredChannelEnvironmentIssueAnalogByTime(
      @RequestBody(description = "Time range for acquired channel environment issue analog data to retrieve")
          TimeRangeRequest request);

  /**
   * Retrieve the {@link AcquiredChannelEnvironmentIssueBoolean} with the provided id.  Returns an empty {@link
   * Optional} if no AcquiredChannelSohBoolean has that id.
   *
   * @param request time range for the AcquiredChannelSohBoolean, not null
   * @return Optional AcquiredChannelSohBoolean object with the provided id, not null
   */
  @Path("/station-soh/acquired-channel-soh-boolean/by-time")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieve acquired channel soh boolean for the provided time range")
  List<AcquiredChannelEnvironmentIssueBoolean> retrieveAcquiredChannelEnvironmentIssueBooleanByTime(
      @RequestBody(description = "Time range for acquired channel environment issue boolean data to retrieve")
          TimeRangeRequest request);
  /**
   * Retrieve the {@link AcquiredChannelEnvironmentIssueBoolean} with latest end time for given channel.
   * Returns an empty {@link List} if no AcquiredChannelSohBoolean in query.
   *
   * @param channelNames names of channels for latest query AcquiredChannelSohBoolean, not null
   * @return List AcquiredChannelSohBoolean objects with latest end times for given channels, not null
   */
  @Path("/station-soh/acquired-channel-soh-boolean/latest")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieves latest data for ACEI boolean")
  List<AcquiredChannelEnvironmentIssueBoolean> retrieveLatestAcquiredChannelEnvironmentIssueBoolean(
      @RequestBody(description = "Channels names for latest data retrieval")
          List<String> channelNames);
  /**
   * Retrieve the {@link AcquiredChannelEnvironmentIssueAnalog} with latest end time for given channel.
   * Returns an empty {@link List} if no AcquiredChannelSohAnalog in query.
   *
   * @param channelNames names of channels for latest query AcquiredChannelSohAnalog, not null
   * @return List AcquiredChannelSohAnalog objects with latest end times for given channels, not null
   */
  @Path("/station-soh/acquired-channel-soh-analog/latest")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieves latest data for ACEI analog")
  List<AcquiredChannelEnvironmentIssueAnalog> retrieveLatestAcquiredChannelEnvironmentIssueAnalog(
      @RequestBody(description = "Channels names for latest data retrieval")
          List<String> channelNames);
}
