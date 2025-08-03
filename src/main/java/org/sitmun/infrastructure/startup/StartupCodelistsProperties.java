package org.sitmun.infrastructure.startup;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sitmun.startup.code-lists")
@Setter
@Getter
public class StartupCodelistsProperties {

  private boolean checkOnStartup = true;
  private List<DatabaseConnectionDriverDefinition> databaseConnectionDrivers =
      Collections.emptyList();
  private List<CodeListValueDefinition> codeListValues = Collections.emptyList();

  @Getter
  @Setter
  public static class DatabaseConnectionDriverDefinition {
    private String driverClass;
    private String description;
  }

  @Getter
  @Setter
  public static class CodeListValueDefinition {
    private String codeListName;
    private String value;
    private String description;
    private boolean isDefault = false;
  }
}
