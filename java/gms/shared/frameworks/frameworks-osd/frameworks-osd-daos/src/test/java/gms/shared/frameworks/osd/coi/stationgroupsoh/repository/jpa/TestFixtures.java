package gms.shared.frameworks.osd.coi.stationgroupsoh.repository.jpa;

import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.StationSohIssue;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TestFixtures {

  public static final Instant now = Instant.now();
  public static final StationSohIssue acknowledged = StationSohIssue.from(false, Instant.EPOCH);
  private static final StationSohIssue notAcknowledged = StationSohIssue.from(true, now);

}
