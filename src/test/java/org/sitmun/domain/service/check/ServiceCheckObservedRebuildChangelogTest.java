package org.sitmun.domain.service.check;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ServiceCheckObservedRebuildChangelogTest {

  private static final String REBUILD = "34_service_check_observed_timestamptz_rebuild.sql";
  private static final String ALTER = "32_service_check_observed_timestamptz.yaml";

  @Test
  @DisplayName("Oracle rebuilds populated observed columns before the empty-column ALTER")
  void rebuildIsIncludedBeforeAlter() throws Exception {
    assertRebuildBeforeAlter(Path.of("config/db/changelog/db.changelog-master.yaml"));
    assertRebuildBeforeAlter(Path.of("src/test/resources/db/changelog/db.changelog-h2.yaml"));
    assertRebuildBeforeAlter(Path.of("src/test/resources/db/changelog/db.changelog-oracle.yaml"));
    assertRebuildBeforeAlter(Path.of("src/test/resources/db/changelog/db.changelog-postgres.yaml"));
  }

  private static void assertRebuildBeforeAlter(Path master) throws Exception {
    String text = Files.readString(master);
    int rebuild = text.indexOf(REBUILD);
    int alter = text.indexOf(ALTER);
    assertThat(rebuild).as(master.toString()).isNotNegative();
    assertThat(alter).as(master.toString()).isNotNegative();
    assertThat(rebuild).as(master.toString()).isLessThan(alter);
  }
}
