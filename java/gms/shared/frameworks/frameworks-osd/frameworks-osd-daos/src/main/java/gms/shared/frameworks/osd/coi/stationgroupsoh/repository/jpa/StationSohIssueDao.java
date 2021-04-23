package gms.shared.frameworks.osd.coi.stationgroupsoh.repository.jpa;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.StationSohIssue;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * JPA data access object for {@link StationSohIssue}
 */
@Entity
@Table(name = "station_soh_issue")
public class StationSohIssueDao {

  @Id
  @GeneratedValue
  private long id;

  @Column(name = "requires_acknowledgement", nullable = false)
  private boolean requiresAcknowledgement;

  @Column(name = "acknowledged_at", nullable = true)
  private Instant acknowledgedAt;

  public StationSohIssueDao() {
    // Empty constructor needed for JPA
  }

  private StationSohIssueDao(StationSohIssue stationSohIssue) {
    this.requiresAcknowledgement = stationSohIssue.getRequiresAcknowledgement();
    this.acknowledgedAt = stationSohIssue.getAcknowledgedAt();
  }

  /**
   * Create a DAO from a COI
   */
  public static StationSohIssueDao from(StationSohIssue stationSohIssue){
    Preconditions.checkNotNull(stationSohIssue,
        "Cannot create StationSohIssueDao from null StationSohIssue");
    return new StationSohIssueDao(stationSohIssue);
  }

  /**
   * Create a COI from a DAO
   */
  public StationSohIssue toCoi(){
    return StationSohIssue.from(requiresAcknowledgement, acknowledgedAt);
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public boolean isRequiresAcknowledgement() {
    return requiresAcknowledgement;
  }

  public void setRequiresAcknowledgement(boolean requiresAcknowledgement) {
    this.requiresAcknowledgement = requiresAcknowledgement;
  }

  public Instant getAcknowledgedAt() {
    return acknowledgedAt;
  }

  public void setAcknowledgedAt(Instant acknowledgedAt) {
    this.acknowledgedAt = acknowledgedAt;
  }
}
