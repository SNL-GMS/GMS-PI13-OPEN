package gms.shared.frameworks.osd.coi.stationreference.repository;

import gms.shared.frameworks.osd.coi.emerging.provenance.repository.InformationSourceDao;
import gms.shared.frameworks.osd.coi.stationreference.NetworkOrganization;
import gms.shared.frameworks.osd.coi.stationreference.NetworkRegion;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceNetwork;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "reference_network")
public class ReferenceNetworkDao {

  @Id
  @GeneratedValue
  private long id;

  @Column(name = "entity_id")
  private UUID entityId;

  @Column(name = "version_id", unique = true)
  private UUID versionId;

  @Column(name = "name")
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "org")
  private NetworkOrganization organization;

  @Column(name = "region")
  private NetworkRegion region;

  @Embedded
  private InformationSourceDao source;

  @Column(name = "comment")
  private String comment;

  @Column(name = "actual_time")
  private Instant actualTime;

  @Column(name = "system_time")
  private Instant systemTime;

  @Column(name = "active")
  private boolean active;

  /**
   * Default constructor for JPA.
   */
  public ReferenceNetworkDao() {
  }

  /**
   * Create a DAO from the corresponding COI object.
   *
   * @param referenceNetwork The ReferenceNetwork object.
   */
  public ReferenceNetworkDao(ReferenceNetwork referenceNetwork) throws NullPointerException {
    Objects.requireNonNull(referenceNetwork);
    this.name = referenceNetwork.getName();
    this.description = referenceNetwork.getDescription();
    this.entityId = referenceNetwork.getEntityId();
    this.versionId = referenceNetwork.getVersionId();
    this.organization = referenceNetwork.getOrganization();
    this.region = referenceNetwork.getRegion();
    this.source = new InformationSourceDao(referenceNetwork.getSource());
    this.comment = referenceNetwork.getComment();
    this.actualTime = referenceNetwork.getActualChangeTime();
    this.systemTime = referenceNetwork.getSystemChangeTime();
    this.active = referenceNetwork.isActive();
  }

  /**
   * Convert this DAO into a COI object.
   *
   * @return The ReferenceNetwork object.
   */
  public ReferenceNetwork toCoi() {
    return ReferenceNetwork.builder()
        .setName(name)
        .setDescription(description)
        .setOrganization(organization)
        .setRegion(region)
        .setSource(source.toCoi())
        .setComment(comment)
        .setActualChangeTime(actualTime)
        .setSystemChangeTime(systemTime)
        .setActive(true)
        .build();
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public UUID getEntityId() {
    return entityId;
  }

  public void setEntityId(UUID id) {
    this.entityId = id;
  }

  public UUID getVersionId() {
    return versionId;
  }

  public void setVersionId(UUID id) {
    this.versionId = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public NetworkOrganization getOrganization() {
    return organization;
  }

  public void setOrganization(
      NetworkOrganization organization) {
    this.organization = organization;
  }

  public NetworkRegion getRegion() {
    return region;
  }

  public void setRegion(
      NetworkRegion region) {
    this.region = region;
  }

  public InformationSourceDao getSource() {
    return source;
  }

  public void setSource(InformationSourceDao source) {
    this.source = source;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Instant getActualTime() {
    return actualTime;
  }

  public void setActualTime(Instant actualTime) {
    this.actualTime = actualTime;
  }

  public Instant getSystemTime() {
    return systemTime;
  }

  public void setSystemTime(Instant systemTime) {
    this.systemTime = systemTime;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReferenceNetworkDao)) {
      return false;
    }
    ReferenceNetworkDao that = (ReferenceNetworkDao) o;
    return id == that.id &&
        active == that.active &&
        entityId.equals(that.entityId) &&
        versionId.equals(that.versionId) &&
        name.equals(that.name) &&
        description.equals(that.description) &&
        organization == that.organization &&
        region == that.region &&
        source.equals(that.source) &&
        comment.equals(that.comment) &&
        actualTime.equals(that.actualTime) &&
        systemTime.equals(that.systemTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, entityId, versionId, name, description, organization, region, source, comment, actualTime, systemTime, active);
  }

  @Override
  public String toString() {
    return "ReferenceNetworkDao{" +
        "id=" + id +
        ", entityId=" + entityId +
        ", versionId=" + versionId +
        ", name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", organization=" + organization +
        ", region=" + region +
        ", source=" + source +
        ", comment='" + comment + '\'' +
        ", actualTime=" + actualTime +
        ", systemTime=" + systemTime +
        ", active=" + active +
        '}';
  }
}
