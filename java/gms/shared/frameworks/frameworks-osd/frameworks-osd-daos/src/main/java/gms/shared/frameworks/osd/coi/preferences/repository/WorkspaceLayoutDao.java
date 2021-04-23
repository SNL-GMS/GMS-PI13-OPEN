package gms.shared.frameworks.osd.coi.preferences.repository;

import gms.shared.frameworks.osd.coi.preferences.UserInterfaceMode;
import gms.shared.frameworks.osd.coi.preferences.WorkspaceLayout;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "workspace_layout")
public class WorkspaceLayoutDao {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "name")
  private String name;

  @ElementCollection
  @CollectionTable(name = "workspace_layout_supported_ui_modes",
      joinColumns = {@JoinColumn(name = "workspace_layout_id")})
  @Column(name = "supported_user_interface_mode")
  private List<UserInterfaceMode> supportedUserInterfaceModes;

  @Lob
  @Column(name = "layout_configuration")
  private String layoutConfiguration;

  protected WorkspaceLayoutDao() {

  }

  public WorkspaceLayoutDao(WorkspaceLayout workspaceLayout) {
    this.id = generateId(workspaceLayout);
    this.name = workspaceLayout.getName();
    this.supportedUserInterfaceModes = workspaceLayout.getSupportedUserInterfaceModes();
    this.layoutConfiguration = workspaceLayout.getLayoutConfiguration();
  }

  private static UUID generateId(WorkspaceLayout workspaceLayout) {
    ObjectOutput objectOutput;
    byte[] objectBytes;

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

      objectOutput = new ObjectOutputStream(byteArrayOutputStream);

      objectOutput.writeObject(workspaceLayout.getName());

      ArrayList<UserInterfaceMode> sortedModes =
          new ArrayList<>(workspaceLayout.getSupportedUserInterfaceModes());
      Collections.sort(sortedModes);
      objectOutput.writeObject(sortedModes);

      objectOutput.writeObject(workspaceLayout.getLayoutConfiguration());
      objectBytes = byteArrayOutputStream.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException(
          "Error serializing WorkspaceLayout attributes to byte array", ex);
    }

    Objects.requireNonNull(objectBytes,
        "Could not has WorkspaceLayout attributes; byte array containing serialized " +
            "WorkspaceLayout output is null");

    return UUID.nameUUIDFromBytes(objectBytes);
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<UserInterfaceMode> getSupportedUserInterfaceModes() {
    return supportedUserInterfaceModes;
  }

  public void setSupportedUserInterfaceModes(List<UserInterfaceMode> supportedUserInterfaceModes) {
    this.supportedUserInterfaceModes = supportedUserInterfaceModes;
  }

  public String getLayoutConfiguration() {
    return layoutConfiguration;
  }

  public void setLayoutConfiguration(String layoutConfiguration) {
    this.layoutConfiguration = layoutConfiguration;
  }

  public WorkspaceLayout toCoi() {
    return WorkspaceLayout.from(name, supportedUserInterfaceModes, layoutConfiguration);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkspaceLayoutDao that = (WorkspaceLayoutDao) o;
    return id == that.id &&
        Objects.equals(name, that.name) &&
        Objects.equals(supportedUserInterfaceModes, that.supportedUserInterfaceModes) &&
        Objects.equals(layoutConfiguration, that.layoutConfiguration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, supportedUserInterfaceModes, layoutConfiguration);
  }
}
