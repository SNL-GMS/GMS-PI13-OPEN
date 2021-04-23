package gms.shared.frameworks.configuration.repository.dao.converter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.configuration.Configuration;
import gms.shared.frameworks.configuration.ConfigurationOption;
import gms.shared.frameworks.configuration.Constraint;
import gms.shared.frameworks.configuration.constraints.BooleanConstraint;
import gms.shared.frameworks.configuration.constraints.DefaultConstraint;
import gms.shared.frameworks.configuration.repository.dao.ConfigurationDao;
import gms.shared.frameworks.configuration.repository.dao.ConfigurationOptionDao;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigurationDaoConverterTest {

  private Set<ConfigurationOptionDao> configurationOptionDaos;
  private Configuration configuration;

  @BeforeEach
  public void init() throws IOException {
    this.configurationOptionDaos = new LinkedHashSet<>();
    List<ConfigurationOption> configurationOptions = new ArrayList<>();

    List<Constraint> constraints = new ArrayList<>();
    BooleanConstraint bc = BooleanConstraint.from("test-criterion", true, 1);
    constraints.add(bc);
    DefaultConstraint dc = DefaultConstraint.from();
    constraints.add(dc);
    Map<String, Object> parameters = new LinkedHashMap<>();
    ConfigurationOption co = ConfigurationOption.from("config-option-test", constraints, parameters);
    ConfigurationOptionDaoConverter codConverter = new ConfigurationOptionDaoConverter(null);
    ConfigurationOptionDao cod = codConverter.fromCoi(co);
    this.configurationOptionDaos.add(cod);
    configurationOptions.add(co);
    this.configuration = Configuration.from("test-config", configurationOptions);
  }

  @Test
  void toCoi() {
    ConfigurationDaoConverter cdc = new ConfigurationDaoConverter();
    ConfigurationDao cd = new ConfigurationDao();
    cd.setName("test-config");
    cd.setConfigurationOptionDaos(this.configurationOptionDaos);
    Configuration cfg = cdc.toCoi(cd);
    assertTrue(cfg.getName().equals("test-config"), "Configuration has incorrect name");
    assertTrue(cfg.getConfigurationOptions().size() == 1, "ConfigurationOption list is wrong size");
    cfg.getConfigurationOptions().forEach(cgOption -> {
      assertTrue(cgOption.getConstraints().size() == 2, "Constraint list is wrong size");
    });
  }

  @Test
  void fromCoi() {
    ConfigurationDaoConverter cdc = new ConfigurationDaoConverter();
    ConfigurationDao configurationDao = cdc.fromCoi(this.configuration);

    assertTrue(configurationDao.getName().equals("test-config"), "ConfigurationDao has incorrect name");
    assertTrue(configurationDao.getConfigurationOptionDaos().size() == 1, "ConfigurationOptionDao list is wrong size");
    configurationDao.getConfigurationOptionDaos().forEach(cgOption -> {
      assertTrue(cgOption.getConstraintDaos().size() == 2, "ConstraintDao list is wrong size");
    });
  }
}