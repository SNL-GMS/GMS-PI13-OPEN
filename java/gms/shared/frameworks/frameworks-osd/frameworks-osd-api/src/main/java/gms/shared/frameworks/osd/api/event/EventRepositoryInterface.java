package gms.shared.frameworks.osd.api.event;

import gms.shared.frameworks.common.ContentType;
import gms.shared.frameworks.osd.api.event.util.FindEventByTimeAndLocationRequest;
import gms.shared.frameworks.osd.coi.event.Event;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.Collection;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

public interface EventRepositoryInterface {

  /**
   * Stores a collection of events.  The Event's should all be new (never existed before).
   *
   * @param events The {@link Collection} of {@link Event} reference containing what events to
   * store.
   */
  @Path("/events")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "stores the following events into the database ")
  void storeEvents(Collection<Event> events);

  /**
   * Finds events by their primary id's.
   *
   * @param eventIds the id's to search for
   * @return a collection of event's with the given id's, may be empty
   */
  @Path("/events/query/ids")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "returns all events requested by the list of event IDs provided")
  Collection<Event> findEventsByIds(
      @RequestBody(description = "list of UUIDs of events to find an retrieve from the database")
          Collection<UUID> eventIds);

  /**
   * Finds events by time range and location.  To be included in the results,
   * the event has to have a preferred hypothesis that matches the query params
   * (is in the requested location and time range)
   *
   * @param request the {@link FindEventByTimeAndLocationRequest} containing the start time, end
   * time, and min and max latitude and longitudes specifying the events to find.
   * @return events that are in the given time range and location, may be empty
   */
  //TODO: should this have another signature excluding the optional params?
  @Path("/events/query/time-lat-lon")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "returns all events within a specific time range and location")
  Collection<Event> findEventsByTimeAndLocation(
      @RequestBody(description = "start time of the time range")
          FindEventByTimeAndLocationRequest request);
}
