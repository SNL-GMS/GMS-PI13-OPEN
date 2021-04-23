package gms.shared.frameworks.osd.coi.preferences;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.List;
import org.apache.commons.lang3.Validate;

@AutoValue
public abstract class UserPreferences {

  public abstract String getUserId();

  public abstract String getDefaultLayoutName();

  public abstract String getSohLayoutName();

  public abstract List<WorkspaceLayout> getWorkspaceLayouts();

  public abstract List<AudibleNotification> getAudibleNotifications();

  @JsonCreator
  public static UserPreferences from(@JsonProperty("userId") String userId,
      @JsonProperty("defaultLayoutName") String defaultLayoutName,
      @JsonProperty("sohLayoutName") String sohLayoutName,
      @JsonProperty("workspaceLayouts") List<WorkspaceLayout> workspaceLayouts,
      @JsonProperty("audibleNotifications") List<AudibleNotification> audibleNotifications) {
    Validate.isTrue(workspaceLayouts != null && !workspaceLayouts.isEmpty(),
        "User Preferences must contain at least 1 WorkspaceLayout");
    Validate.isTrue(audibleNotifications != null,
        "User Preferences may not have an empty AudibleNotifications List");
    Validate.isTrue(audibleNotifications.size() == audibleNotifications.stream()
            .map(AudibleNotification::getNotificationType).distinct().count(),
        "User Preferences may not have duplicate Message type entries in the AudibleNotifications List");
    return new AutoValue_UserPreferences(userId,
        defaultLayoutName,
        sohLayoutName,
        workspaceLayouts,
        audibleNotifications);
  }
}
