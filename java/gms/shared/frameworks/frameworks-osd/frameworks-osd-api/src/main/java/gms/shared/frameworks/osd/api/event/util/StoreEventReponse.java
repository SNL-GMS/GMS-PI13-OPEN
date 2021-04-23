package gms.shared.frameworks.osd.api.event.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.UUID;

@AutoValue
public abstract class StoreEventReponse {

  public abstract ImmutableList<UUID> getStoredEvents();

  public abstract ImmutableList<UUID> getUpdatedEvents();

  public abstract ImmutableList<UUID> getErrorEvents();

  @JsonCreator
  public static StoreEventReponse from(
      @JsonProperty("storedEvents") List<UUID> storedEvents,
      @JsonProperty("updatedEvents") List<UUID> updatedEvents,
      @JsonProperty("errorEvents") List<UUID> errorEvents) {
    return new AutoValue_StoreEventReponse(ImmutableList.copyOf(storedEvents),
        ImmutableList.copyOf(updatedEvents), ImmutableList.copyOf(errorEvents));
  }
}
