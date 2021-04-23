package gms.integration.util;

import gms.integration.steps.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostgresDbConnection {

  private static final String GMS_DB_TEST_USER = "gms_read_only";
  private static final String GMS_DB_TEST_PASSWORD = "gmsdb:postgres:gms_read_only:humoured-tempered-furious-lion";

  public static Optional<Connection> getConnection(String connectionUrl) {
    Properties props = new Properties();
    props.setProperty("user", GMS_DB_TEST_USER);
    props.setProperty("password", GMS_DB_TEST_PASSWORD);
    props.setProperty("ssl", "false");
    try (Connection conn = DriverManager.getConnection(connectionUrl, props)){
      return Optional.of(conn);
    } catch (SQLException e) {
      e.printStackTrace();
      assertTrue(false);
    }
    return Optional.empty();
  }
  public static int getRowCount(String tableName, Environment environment) {
    String getRowCountForTable = "SELECT count(*) FROM " + tableName;
    int rowCount = 0;
    try {
      var conn = PostgresDbConnection
              .getConnection(environment.deploymentCtxt().getJdbcUrl());
      assertTrue(conn.isPresent());
      ResultSet resultSet = conn.get().prepareStatement(getRowCountForTable).executeQuery();
      resultSet.next();
      rowCount = Integer.parseInt(resultSet.getString(1));
    } catch (
            SQLException e) {
      e.printStackTrace();
      assertTrue(false);
    }
    return rowCount;
  }
}
