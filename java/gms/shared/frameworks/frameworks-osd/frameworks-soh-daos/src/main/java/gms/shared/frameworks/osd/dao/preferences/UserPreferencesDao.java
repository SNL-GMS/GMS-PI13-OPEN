package gms.shared.frameworks.osd.dao.preferences;

import gms.shared.frameworks.osd.coi.preferences.AudibleNotification;
import gms.shared.frameworks.osd.coi.preferences.UserPreferences;
import gms.shared.frameworks.osd.coi.preferences.WorkspaceLayout;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "user_preferences")
public class UserPreferencesDao {

  @Id
  @Column(name = "id")
  private String userId;

  @Column(name = "default_layout_name")
  private String defaultLayoutName;

  @Column(name = "soh_layout_name")
  private String sohLayoutName;

  @OneToMany(mappedBy = "userPreferences", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<WorkspaceLayoutDao> workspaceLayouts;

  @OneToMany(mappedBy = "userPreferences", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<AudibleNotificationDao> audibleNotifications;

  protected UserPreferencesDao() {
    // no arg JPA constructor
  }

  public UserPreferencesDao(UserPreferences userPreferences) {
    this.userId = userPreferences.getUserId();
    this.defaultLayoutName = userPreferences.getDefaultLayoutName();
    this.sohLayoutName = userPreferences.getSohLayoutName();
    this.workspaceLayouts = userPreferences.getWorkspaceLayouts().stream()
        .map(WorkspaceLayoutDao::new)
        .map(workspaceLayout -> {
          workspaceLayout.setUserPreferences(UserPreferencesDao.this);
          return workspaceLayout;
        })
        .collect(Collectors.toList());
    this.audibleNotifications = userPreferences.getAudibleNotifications().stream()
        .map(AudibleNotificationDao::new)
        .map(audibleNotification -> {
          audibleNotification.setUserPreferences(UserPreferencesDao.this);
          return audibleNotification;
        })
        .collect(Collectors.toList());
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
    final List<WorkspaceLayout> workspaceLayoutCois = workspaceLayouts.stream()
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
    return userId.equals(that.userId) &&
        defaultLayoutName.equals(that.defaultLayoutName) &&
        sohLayoutName.equals(that.sohLayoutName) &&
        workspaceLayouts.equals(that.workspaceLayouts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, defaultLayoutName, sohLayoutName, workspaceLayouts);
  }
}
