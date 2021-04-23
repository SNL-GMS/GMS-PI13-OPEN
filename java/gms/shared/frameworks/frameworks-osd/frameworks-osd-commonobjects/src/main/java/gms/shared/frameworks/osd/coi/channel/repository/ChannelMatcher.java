package gms.shared.frameworks.osd.coi.channel.repository;

import com.google.auto.value.AutoValue;
import java.util.List;
import java.util.UUID;

@AutoValue
public abstract class ChannelMatcher {
  public abstract List<UUID> getChannelIds();

  public abstract List<String> getCanonicalNames();
}
