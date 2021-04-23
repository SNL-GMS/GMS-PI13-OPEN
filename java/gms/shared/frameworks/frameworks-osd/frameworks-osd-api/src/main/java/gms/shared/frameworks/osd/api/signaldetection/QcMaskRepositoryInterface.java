package gms.shared.frameworks.osd.api.signaldetection;

import gms.shared.frameworks.common.ContentType;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.ChannelsTimeRangeRequest;
import gms.shared.frameworks.osd.coi.signaldetection.QcMask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface QcMaskRepositoryInterface {

  /**
   * Store for the first time the provided {@link QcMask} and all of its {@link
   * gms.shared.frameworks.osd.coi.signaldetection.QcMaskVersion}.
   *
   * @param qcMasks store this {@link QcMask} and its versions, not null
   */
  @Path("/qc-masks/new")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Stores the provided QcMasks")
  void storeQcMasks(
      @RequestBody(description = "the QcMasks to store")
      Collection<QcMask> qcMasks);

  // TODO: Remove once everything is off this route
  @Path("/qc-masks/channel-time")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieves QcMasks by their channel name and time range")
  Collection<QcMask> findCurrentQcMasksByChannelIdAndTimeRange(
      @RequestBody(description = "Channel names and a time range")
          ChannelTimeRangeRequest request);

  /**
   * Retrieves the current version of all QcMasks associated with the provided {@link
   * gms.shared.frameworks.osd.coi.channel.Channel}
   * Id and are valid between the provided time range.
   *
   * @param request {@link ChannelsTimeRangeRequest} representing which channels within which time range to
   * retrieve specific {@link QcMask} for.
   * @return QcMasks created for the Processing Channel
   */
  // TODO: Correct this route name one everything is moved off the single channel route
  @Path("/qc-masks/channels-time")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Retrieves QcMasks by multiple channel names and a time range")
  Map<String, List<QcMask>> findCurrentQcMasksByChannelNamesAndTimeRange(
      @RequestBody(description = "Collection of channel names and a time range")
          ChannelsTimeRangeRequest request);

}