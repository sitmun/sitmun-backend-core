package org.sitmun.domain.tree.node;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@DisplayName("TreeNode schema metadata")
class TreeNodeSchemaMetadataTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName(
      "STM_TREE_NOD has non-null TNO_ACTIVE (visibility) and TNO_DEFAULT (load by default)")
  void treeNodeColumnMetadata() throws SQLException {
    DataSource dataSource = jdbcTemplate.getDataSource();
    assertThat(dataSource).isNotNull();

    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metadata = connection.getMetaData();
      // H2 keeps uppercase identifiers; Postgres folds unquoted names to lowercase.
      Map<String, Integer> columns = readColumns(metadata, "STM_TREE_NOD");
      if (columns.isEmpty()) {
        columns = readColumns(metadata, "stm_tree_nod");
      }

      assertThat(columns).containsKeys("TNO_ACTIVE", "TNO_DEFAULT");
      assertThat(columns.get("TNO_ACTIVE")).isEqualTo(DatabaseMetaData.columnNoNulls);
      assertThat(columns.get("TNO_DEFAULT")).isEqualTo(DatabaseMetaData.columnNoNulls);
      assertThat(columns).doesNotContainKey("TNO_VISIBLE");
      assertThat(columns).doesNotContainKey("TNO_LOAD_BY_DEFAULT");
    }
  }

  private static Map<String, Integer> readColumns(DatabaseMetaData metadata, String tableName)
      throws SQLException {
    Map<String, Integer> columns = new HashMap<>();
    try (ResultSet resultSet = metadata.getColumns(null, null, tableName, null)) {
      while (resultSet.next()) {
        columns.put(resultSet.getString("COLUMN_NAME").toUpperCase(), resultSet.getInt("NULLABLE"));
      }
    }
    return columns;
  }
}
