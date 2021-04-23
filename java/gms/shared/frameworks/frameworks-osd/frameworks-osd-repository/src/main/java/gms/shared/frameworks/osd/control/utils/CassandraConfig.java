package gms.shared.frameworks.osd.control.utils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.Session;
import com.google.auto.value.AutoValue;
import gms.shared.frameworks.coi.exceptions.StorageUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoValue
public abstract class CassandraConfig {

  private static final Logger logger = LoggerFactory.getLogger(CassandraConfig.class);

  public abstract String getConnectPoints();
  public abstract int getPort();
  public abstract String getClusterName();
  public abstract String getUser();
  public abstract String getPassword();

  private Cluster cluster;

  public static CassandraConfig from(
      String connectPoints, int port, String clusterName, String user, String password) {
    return new AutoValue_CassandraConfig(connectPoints, port, clusterName,
        user, password);
  }

  /**
   * Get a session for Cassandra.
   *
   * @return A Session object.
   * @throws StorageUnavailableException if there was a failure to establish a connection to Cassandra
   */
  public Session getConnection() {
    if (this.cluster == null || this.cluster.isClosed()) {
      this.cluster = Cluster.builder().addContactPoints(getConnectPoints())
          .withPort(getPort())
          .withCredentials(getUser(), getPassword())
          .withAuthProvider(new PlainTextAuthProvider(getUser(), getPassword()))
          .build();
    }
    logger.info("Created Cassandra Cluster for {}", getClusterName());

    try {
      return this.cluster.connect();
    } catch (RuntimeException e) {
      logger.error("Error connecting to cluster", e);
      throw new StorageUnavailableException(e);
    }
  }
}
