package gms.shared.frameworks.osd.coi.preferences.repository;

import gms.shared.frameworks.osd.coi.preferences.AudibleNotification;
import gms.shared.frameworks.osd.coi.preferences.UserPreferences;
import gms.shared.frameworks.osd.coi.preferences.WorkspaceLayout;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "user_preferences",
    indexes = {@Index(name = "user_name", columnList = "user_id", unique = true)})
public class UserPreferencesDao {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "user_id", unique = true)
  private String userId;

  @Column(name = "default_layout_name")
  private String defaultLayoutName;

  @Column(name = "soh_layout_name")
  private String sohLayoutName;

  @OneToMany
  @JoinColumn(name = "user_preferences_id", referencedColumnName = "id")
  private List<WorkspaceLayoutDao> workspaceLayouts;

  @OneToMany(mappedBy = "userPreferences", cascade = CascadeType.ALL)
  private List<AudibleNotificationDao> audibleNotifications;

  protected UserPreferencesDao() {
    // no arg JPA constructor
  }

  public UserPreferencesDao(UserPreferences userPreferences) {
    this.userId = userPreferences.getUserId();
    this.defaultLayoutName = userPreferences.getDefaultLayoutName();
    this.workspaceLayouts = userPreferences.getWorkspaceLayouts().stream()
        .map(WorkspaceLayoutDao::new)
        .collect(Collectors.toList());
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getDefaultLayoutName() {
    return defaultLayoutName;
  }

  public void setDefaultLayoutName(String defaultLayoutName) {
    this.defaultLayoutName = defaultLayoutName;
  }

  public String getSohLayoutName() {
    return sohLayoutName;
  }

  public void setSohLayoutName(String sohLayoutName) {
    this.sohLayoutName = sohLayoutName;
  }

  public List<WorkspaceLayoutDao> getWorkspaceLayouts() {
    return workspaceLayouts;
  }

  public void setWorkspaceLayouts(List<WorkspaceLayoutDao> workspaceLayouts) {
    this.workspaceLayouts = workspaceLayouts;
  }

  public List<AudibleNotificationDao> getAudibleNotifications() {
    return audibleNotifications;
  }

  public void setAudibleNotifications(
      List<AudibleNotificationDao> audibleNotifications) {
    this.audibleNotifications = audibleNotifications;
  }

  public UserPreferences toCoi() {
    List<WorkspaceLayout> workspaceLayoutCois = workspaceLayouts.stream()
        .map(WorkspaceLayoutDao::toCoi)
        .collect(Collectors.toList());
    final List<AudibleNotification> audibleNotificationCois = audibleNotifications.stream()
        .map(AudibleNotificationDao::toCoi)
        .collect(Collectors.toList());
    return UserPreferences.from(userId,
        defaultLayoutName,
        sohLayoutName,
        workspaceLayoutCois,
        audibleNotificationCois);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserPreferencesDao that = (UserPreferencesDao) o;
    return id == that.id &&
        userId.equals(that.userId) &&
        defaultLayoutName.equals(that.defaultLayoutName) &&
        sohLayoutName.equals(that.sohLayoutName) &&
        workspaceLayouts.equals(that.workspaceLayouts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, userId, defaultLayoutName, sohLayoutName, workspaceLayouts);
  }
}
