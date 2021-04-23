package gms.shared.frameworks.osd.control.utils;

import gms.shared.frameworks.systemconfig.SystemConfig;
import java.util.Objects;

/**
 * Utility class for creating cassandra configurations
 */
public class CassandraConfigFactory {

  private CassandraConfigFactory() {
  }

  /**
   * Creates a {@link CassandraConfig} using {@link SystemConfig}.
   * @param sysConfig a system config, not null
   * @return a cassandra config, not null
   */
  public static CassandraConfig create(SystemConfig sysConfig) {
    Objects.requireNonNull(sysConfig, "system config cannot be null");
    return CassandraConfig.from(
        sysConfig.getValue("cassandra_connect_points"),
        sysConfig.getValueAsInt("cassandra_port"),
        sysConfig.getValue("cassandra_cluster_name"),
        sysConfig.getValue("cassandra_user"),
        sysConfig.getValue("cassandra_password"));
  }
}