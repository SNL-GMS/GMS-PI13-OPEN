package gms.shared.frameworks.soh.repository.preferences;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.preferences.AudibleNotification;
import gms.shared.frameworks.osd.coi.preferences.UserInterfaceMode;
import gms.shared.frameworks.osd.coi.preferences.UserPreferences;
import gms.shared.frameworks.osd.coi.preferences.WorkspaceLayout;
import gms.shared.frameworks.osd.coi.systemmessages.SystemMessageType;
import gms.shared.frameworks.osd.coi.util.RandomUtility;
import gms.shared.frameworks.osd.dao.preferences.UserPreferencesDao;
import gms.shared.frameworks.soh.repository.util.DbTest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class UserPreferencesRepositoryJpaTest extends DbTest {

  @Test
  void setUserPreferences() {
    final var userId = "Test Id " + RandomUtility.randomInt(1000);
    UserPreferences userPreferences = UserPreferences.from(userId,
        "Default Layout",
        "Default Layout",
        List.of(WorkspaceLayout.from("Default Layout", List.of(UserInterfaceMode.ANALYST), "Test " +
            "Configuration")), new ArrayList<>());

    UserPreferencesRepositoryJpa userPreferencesRepository =
        new UserPreferencesRepositoryJpa(entityManagerFactory);
    userPreferencesRepository.setUserPreferences(userPreferences);

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    UserPreferencesDao userPreferencesDao = entityManager.find(UserPreferencesDao.class, userPreferences.getUserId());

    assertNotNull(userPreferencesDao);
    assertEquals(userPreferences, userPreferencesDao.toCoi());
    entityManager.close();
  }

  @Test
  void getUserPreferencesByUserId() {
    final var userId = "Test Id " + RandomUtility.randomInt(1000);
    UserPreferences userPreferences = UserPreferences.from(userId,
        "Default Layout",
        "Default Layout",
        List.of(WorkspaceLayout.from("Default Layout", List.of(UserInterfaceMode.ANALYST), "Test " +
            "Configuration2")), new ArrayList<>());

    UserPreferencesRepositoryJpa userPreferencesRepository =
        new UserPreferencesRepositoryJpa(entityManagerFactory);
    userPreferencesRepository.setUserPreferences(userPreferences);

    Optional<UserPreferences> actual =
        userPreferencesRepository.getUserPreferencesByUserId(userPreferences.getUserId());
    assertTrue(actual.isPresent());
    assertEquals(userPreferences, actual.get());
  }

  @Test
  void setUserPreferences_withNotifications() {
    final var audibleNotifications = List.of(
        AudibleNotification.from("Hey.wav", SystemMessageType.STATION_CAPABILITY_STATUS_CHANGED),
        AudibleNotification.from("Listen.wav", SystemMessageType.STATION_NEEDS_ATTENTION));
    final var userId = "Test Id " + RandomUtility.randomInt(10000);
    UserPreferences userPreferences = UserPreferences.from(userId,
        "Default Layout",
        "Default Layout",
        List.of(WorkspaceLayout.from("Default Layout", List.of(UserInterfaceMode.ANALYST), "Test " +
            "Configuration")), audibleNotifications);

    UserPreferencesRepositoryJpa userPreferencesRepository =
        new UserPreferencesRepositoryJpa(entityManagerFactory);
    userPreferencesRepository.setUserPreferences(userPreferences);

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    UserPreferencesDao userPreferencesDao = entityManager.find(UserPreferencesDao.class, userPreferences.getUserId());

    assertNotNull(userPreferencesDao);
    assertEquals(userPreferences, userPreferencesDao.toCoi());
    entityManager.close();
  }

  @Test
  void setUserPreferences_updateNotifications() {
    final var audibleNotifications = List.of(
        AudibleNotification.from("Hey.wav", SystemMessageType.STATION_CAPABILITY_STATUS_CHANGED),
        AudibleNotification.from("Listen.wav", SystemMessageType.STATION_NEEDS_ATTENTION));
    final var userId = "Test Id " + RandomUtility.randomInt(10000);
    UserPreferences userPreferences = UserPreferences.from(userId,
        "Default Layout",
        "Default Layout",
        List.of(WorkspaceLayout.from("Default Layout", List.of(UserInterfaceMode.ANALYST), "Test " +
            "Configuration")), audibleNotifications);
    UserPreferencesRepositoryJpa userPreferencesRepository = new UserPreferencesRepositoryJpa(entityManagerFactory);
    userPreferencesRepository.setUserPreferences(userPreferences);

    final var audibleNotificationsUpdate = List.of(
        AudibleNotification.from("Hey.wav", SystemMessageType.STATION_CAPABILITY_STATUS_CHANGED),
        AudibleNotification.from("Hush.wav", SystemMessageType.CHANNEL_MONITOR_TYPE_QUIETED),
        AudibleNotification.from("WatchOut.wav", SystemMessageType.STATION_NEEDS_ATTENTION));
    UserPreferences userPreferencesUpdate = UserPreferences.from(userId,
        "Default Layout",
        "Default Layout",
        List.of(WorkspaceLayout.from("Default Layout", List.of(UserInterfaceMode.ANALYST), "Test " +
            "Configuration")), audibleNotificationsUpdate);
    userPreferencesRepository.setUserPreferences(userPreferencesUpdate);

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    UserPreferencesDao userPreferencesDao = entityManager.find(UserPreferencesDao.class, userPreferences.getUserId());
    assertNotNull(userPreferencesDao);
    assertEquals(userPreferencesUpdate, userPreferencesDao.toCoi());
    entityManager.close();
  }

  @Test
  void getUserPreferencesByUserId_withNotifications() {
    final var audibleNotifications = List.of(
        AudibleNotification.from("Hey.wav", SystemMessageType.STATION_CAPABILITY_STATUS_CHANGED),
        AudibleNotification.from("Listen.wav", SystemMessageType.STATION_NEEDS_ATTENTION));

    final var userId = "Test Id " + RandomUtility.randomInt(1000);
    UserPreferences userPreferences = UserPreferences.from(userId,
        "Default Layout",
        "Default Layout",
        List.of(WorkspaceLayout.from("Default Layout", List.of(UserInterfaceMode.ANALYST), "Test " +
            "Configuration2")), audibleNotifications);

    UserPreferencesRepositoryJpa userPreferencesRepository =
        new UserPreferencesRepositoryJpa(entityManagerFactory);
    userPreferencesRepository.setUserPreferences(userPreferences);

    Optional<UserPreferences> actual =
        userPreferencesRepository.getUserPreferencesByUserId(userPreferences.getUserId());
    assertTrue(actual.isPresent());
    assertEquals(userPreferences, actual.get());
  }

  @Test
  void testUpdate() throws IOException {
    String json = "{\n" +
        "  \"defaultLayoutName\": \"SOH Layout\",\n" +
        "  \"audibleNotifications\": [],\n" +
        "  \"sohLayoutName\": \"SOH Layout\",\n" +
        "  \"userId\": \"defaultUser\",\n" +
        "  \"workspaceLayouts\": [\n" +
        "    {\n" +
        "      \"name\": \"SOH Layout\",\n" +
        "      \"supportedUserInterfaceModes\": [\"SOH\", \"ANALYST\"],\n" +
        "      \"layoutConfiguration\": \"%7B%22settings%22:%7B%22hasHeaders%22:true," +
        "%22constrainDragToContainer%22:true,%22reorderEnabled%22:true," +
        "%22selectionEnabled%22:false,%22popoutWholeStack%22:false," +
        "%22blockedPopoutsThrowError%22:true,%22closePopoutsOnUnload%22:true," +
        "%22showPopoutIcon%22:false,%22showMaximiseIcon%22:true,%22showCloseIcon%22:true," +
        "%22responsiveMode%22:%22onload%22,%22tabOverlapAllowance%22:0," +
        "%22reorderOnTabMenuClick%22:true,%22tabControlOffset%22:10%7D," +
        "%22dimensions%22:%7B%22borderWidth%22:2,%22borderGrabWidth%22:15,%22minItemHeight%22:30," +
        "%22minItemWidth%22:30,%22headerHeight%22:30,%22dragProxyWidth%22:300," +
        "%22dragProxyHeight%22:200%7D,%22labels%22:%7B%22close%22:%22close%22," +
        "%22maximise%22:%22maximise%22,%22minimise%22:%22minimise%22," +
        "%22popout%22:%22open%20in%20new%20window%22,%22popin%22:%22pop%20in%22," +
        "%22tabDropdown%22:%22additional%20tabs%22%7D,%22content%22:%5B%7B%22type%22:%22row%22," +
        "%22isClosable%22:true,%22reorderEnabled%22:true,%22title%22:%22%22," +
        "%22content%22:%5B%7B%22type%22:%22stack%22,%22width%22:50,%22isClosable%22:true," +
        "%22reorderEnabled%22:true,%22title%22:%22%22,%22activeItemIndex%22:0," +
        "%22content%22:%5B%7B%22type%22:%22component%22,%22title%22:%22SOH%20Overview%22," +
        "%22component%22:%22soh-overview%22,%22componentName%22:%22lm-react-component%22," +
        "%22isClosable%22:true,%22reorderEnabled%22:true%7D%5D%7D,%7B%22type%22:%22stack%22," +
        "%22header%22:%7B%7D,%22isClosable%22:true,%22reorderEnabled%22:true,%22title%22:%22%22," +
        "%22activeItemIndex%22:0,%22width%22:50,%22content%22:%5B%7B%22type%22:%22component%22," +
        "%22title%22:%22SOH%20Details%22,%22component%22:%22soh-details%22," +
        "%22componentName%22:%22lm-react-component%22,%22isClosable%22:true," +
        "%22reorderEnabled%22:true%7D%5D%7D%5D%7D%5D,%22isClosable%22:true," +
        "%22reorderEnabled%22:true,%22title%22:%22%22,%22openPopouts%22:%5B%5D," +
        "%22maximisedItemId%22:null%7D\"    },\n" +
        "    {\n" +
        "      \"name\": \"Analyst Displays Layout\",\n" +
        "      \"supportedUserInterfaceModes\": [\"ANALYST\"],\n" +
        "      \"layoutConfiguration\": \"%7B%22settings%22:%7B%22hasHeaders%22:true," +
        "%22constrainDragToContainer%22:true,%22reorderEnabled%22:true," +
        "%22selectionEnabled%22:false,%22popoutWholeStack%22:false," +
        "%22blockedPopoutsThrowError%22:true,%22closePopoutsOnUnload%22:true," +
        "%22showPopoutIcon%22:false,%22showMaximiseIcon%22:true,%22showCloseIcon%22:true," +
        "%22responsiveMode%22:%22onload%22,%22tabOverlapAllowance%22:0," +
        "%22reorderOnTabMenuClick%22:true,%22tabControlOffset%22:10%7D," +
        "%22dimensions%22:%7B%22borderWidth%22:2,%22borderGrabWidth%22:15,%22minItemHeight%22:30," +
        "%22minItemWidth%22:30,%22headerHeight%22:30,%22dragProxyWidth%22:300," +
        "%22dragProxyHeight%22:200%7D,%22labels%22:%7B%22close%22:%22close%22," +
        "%22maximise%22:%22maximise%22,%22minimise%22:%22minimise%22," +
        "%22popout%22:%22open%20in%20new%20window%22,%22popin%22:%22pop%20in%22," +
        "%22tabDropdown%22:%22additional%20tabs%22%7D,%22content%22:%5B%7B%22type%22:%22row%22," +
        "%22isClosable%22:true,%22reorderEnabled%22:true,%22title%22:%22%22," +
        "%22content%22:%5B%7B%22type%22:%22column%22,%22width%22:60,%22isClosable%22:true," +
        "%22reorderEnabled%22:true,%22title%22:%22%22,%22content%22:%5B%7B%22type%22:%22stack%22," +
        "%22height%22:30,%22isClosable%22:true,%22reorderEnabled%22:true,%22title%22:%22%22," +
        "%22activeItemIndex%22:0,%22content%22:%5B%7B%22type%22:%22component%22," +
        "%22title%22:%22Map%22,%22component%22:%22map%22,%22height%22:30," +
        "%22componentName%22:%22lm-react-component%22,%22isClosable%22:true," +
        "%22reorderEnabled%22:true%7D%5D%7D,%7B%22type%22:%22stack%22,%22isClosable%22:true," +
        "%22reorderEnabled%22:true,%22title%22:%22%22,%22height%22:70,%22activeItemIndex%22:4," +
        "%22content%22:%5B%7B%22type%22:%22component%22,%22title%22:%22Events%22," +
        "%22component%22:%22events%22,%22componentName%22:%22lm-react-component%22," +
        "%22isClosable%22:true,%22reorderEnabled%22:true%7D,%7B%22type%22:%22component%22," +
        "%22title%22:%22Signal%20Detections%22,%22component%22:%22signal-detections%22," +
        "%22componentName%22:%22lm-react-component%22,%22isClosable%22:true," +
        "%22reorderEnabled%22:true%7D,%7B%22type%22:%22component%22," +
        "%22title%22:%22Azimuth%20Slowness%22,%22component%22:%22azimuth-slowness%22," +
        "%22componentName%22:%22lm-react-component%22,%22isClosable%22:true," +
        "%22reorderEnabled%22:true%7D,%7B%22type%22:%22component%22,%22title%22:%22Magnitude%22," +
        "%22component%22:%22magnitude%22,%22componentName%22:%22lm-react-component%22," +
        "%22isClosable%22:true,%22reorderEnabled%22:true%7D,%7B%22type%22:%22component%22," +
        "%22title%22:%22Location%22,%22component%22:%22location%22," +
        "%22componentName%22:%22lm-react-component%22,%22isClosable%22:true," +
        "%22reorderEnabled%22:true%7D,%7B%22type%22:%22component%22," +
        "%22title%22:%22Station%20Information%22,%22component%22:%22station-information%22," +
        "%22componentName%22:%22lm-react-component%22,%22isClosable%22:true," +
        "%22reorderEnabled%22:true%7D,%7B%22type%22:%22component%22," +
        "%22title%22:%22Station%20Configuration%22,%22component%22:%22station-configuration%22," +
        "%22componentName%22:%22lm-react-component%22,%22isClosable%22:true," +
        "%22reorderEnabled%22:true%7D,%7B%22type%22:%22component%22," +
        "%22title%22:%22SOH%20Overview%22,%22component%22:%22soh-overview%22," +
        "%22componentName%22:%22lm-react-component%22,%22isClosable%22:true," +
        "%22reorderEnabled%22:true%7D,%7B%22type%22:%22component%22," +
        "%22title%22:%22Transfer%20Gaps%22,%22component%22:%22transfer-gaps%22," +
        "%22componentName%22:%22lm-react-component%22,%22isClosable%22:true," +
        "%22reorderEnabled%22:true%7D,%7B%22type%22:%22component%22," +
        "%22title%22:%22Configure%20Station%20Groups%22," +
        "%22component%22:%22configure-station-groups%22," +
        "%22componentName%22:%22lm-react-component%22,%22isClosable%22:true," +
        "%22reorderEnabled%22:true%7D%5D%7D%5D%7D,%7B%22type%22:%22column%22," +
        "%22isClosable%22:true,%22reorderEnabled%22:true,%22title%22:%22%22,%22width%22:40," +
        "%22content%22:%5B%7B%22type%22:%22stack%22,%22height%22:30,%22isClosable%22:true," +
        "%22reorderEnabled%22:true,%22title%22:%22%22,%22activeItemIndex%22:0," +
        "%22content%22:%5B%7B%22type%22:%22component%22,%22title%22:%22Workflow%22," +
        "%22component%22:%22workflow%22,%22componentName%22:%22lm-react-component%22," +
        "%22isClosable%22:true,%22reorderEnabled%22:true%7D%5D%7D,%7B%22type%22:%22stack%22," +
        "%22height%22:70,%22isClosable%22:true,%22reorderEnabled%22:true,%22title%22:%22%22," +
        "%22activeItemIndex%22:0,%22content%22:%5B%7B%22type%22:%22component%22," +
        "%22title%22:%22Waveforms%22,%22component%22:%22waveform-display%22,%22height%22:70," +
        "%22componentName%22:%22lm-react-component%22,%22isClosable%22:true," +
        "%22reorderEnabled%22:true%7D%5D%7D%5D%7D%5D%7D%5D,%22isClosable%22:true," +
        "%22reorderEnabled%22:true,%22title%22:%22%22,%22openPopouts%22:%5B%5D," +
        "%22maximisedItemId%22:null%7D\"\n" +
        "    }\n" +
        "  ]\n" +
        "}";

    ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
    UserPreferences preferences = objectMapper.readValue(json, UserPreferences.class);

    UserPreferencesRepositoryJpa userPreferencesRepositoryJpa =
        new UserPreferencesRepositoryJpa(entityManagerFactory);
    assertDoesNotThrow(() -> userPreferencesRepositoryJpa.setUserPreferences(preferences));
  }
}