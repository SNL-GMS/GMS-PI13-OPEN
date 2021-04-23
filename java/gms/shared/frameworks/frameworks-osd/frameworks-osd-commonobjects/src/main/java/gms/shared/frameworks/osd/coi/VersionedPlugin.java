package gms.shared.frameworks.osd.coi;

import java.util.Map;

public interface VersionedPlugin extends gms.shared.frameworks.pluginregistry.Plugin {

  String getName();

  PluginVersion getVersion();

  void initialize(Map<String, Object> parameterFieldMap);

}
