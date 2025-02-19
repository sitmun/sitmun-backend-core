package org.sitmun.infrastructure.persistence.type.codelist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.infrastructure.persistence.config.LiquibaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@DisplayName("CodeLists Test")
class CodeListsTest {

  @Autowired
  private CodeListValueRepository codeListValueRepository;

  @Test
  @DisplayName("Check availability of CodeLists")
  void checkAvailableCodeLists() {
    assertThat(codeListValueRepository.findDistinctCodeListName()).containsExactlyInAnyOrder(
      CodeListsConstants.APPLICATION_PARAMETER_TYPE,
      CodeListsConstants.APPLICATION_TYPE,
      CodeListsConstants.CARTOGRAPHY_FILTER_TYPE,
      CodeListsConstants.CARTOGRAPHY_FILTER_VALUE_TYPE,
      CodeListsConstants.CARTOGRAPHY_GEOMETRY_TYPE,
      CodeListsConstants.CARTOGRAPHY_LEGEND_TYPE,
      CodeListsConstants.CARTOGRAPHY_PARAMETER_FORMAT,
      CodeListsConstants.CARTOGRAPHY_PARAMETER_TYPE,
      CodeListsConstants.CARTOGRAPHY_PERMISSION_TYPE,
      // CodeListsConstants.DATABASE_CONNECTION_DRIVER,
      // DOWNLOAD_TASK_FORMAT,
      CodeListsConstants.DOWNLOAD_TASK_SCOPE,
      CodeListsConstants.QUERY_TASK_SCOPE,
      // SERVICE_NATIVE_PROTOCOL,
      CodeListsConstants.SERVICE_PARAMETER_TYPE,
      CodeListsConstants.SERVICE_TYPE,
      CodeListsConstants.SERVICE_AUTHENTICATION_MODE,
      CodeListsConstants.TASK_PARAMETER_FORMAT,
      CodeListsConstants.TASK_PARAMETER_TYPE,
      CodeListsConstants.TERRITORY_SCOPE,
      CodeListsConstants.THEMATIC_MAP_DESTINATION,
      CodeListsConstants.THEMATIC_MAP_TYPE,
      CodeListsConstants.THEMATIC_MAP_VALUE_TYPE,
      // THEMATIC_MAP_RANGE_STYLE,
      CodeListsConstants.USER_IDENTIFICATION_TYPE,
      CodeListsConstants.USER_POSITION_TYPE,
      CodeListsConstants.CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_FORMAT,
      CodeListsConstants.CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE,
      CodeListsConstants.TREE_TYPE,
      CodeListsConstants.TREE_NODE_LEAF_TYPE,
      CodeListsConstants.TREE_NODE_FOLDER_TYPE
    );
  }

  private Iterable<String> select(String list) {
    return StreamSupport.stream(
        codeListValueRepository.findAll(QCodeListValue.codeListValue.codeListName.eq(list))
          .spliterator(), false)
      .map(CodeListValue::getValue).collect(Collectors.toList());
  }

  @Test
  @DisplayName("Check application.type")
  void checkApplicationType() {
    assertThat(select(CodeListsConstants.APPLICATION_TYPE))
      .containsExactlyInAnyOrder("I", "E", "T");
  }

  @Test
  @DisplayName("Check applicationParameter.type")
  void checkApplicationParameterType() {
    assertThat(select(CodeListsConstants.APPLICATION_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("MOBILE", "Nomenclator", "PRINT_TEMPLATE");
  }

  @Test
  @DisplayName("Check cartography.geometryType")
  void checkCartographyGeometryType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_GEOMETRY_TYPE))
      .containsExactlyInAnyOrder("POINT", "LINE", "POLYGON");
  }

  @Test
  @DisplayName("Check cartography.legendType")
  void checkCartographyLegendType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_LEGEND_TYPE))
      .containsExactlyInAnyOrder("LINK", "LEGENDGRAPHIC", "CAPABILITIES");
  }

  @Test
  @DisplayName("Check cartographyFilter.Type")
  void checkCartographyFilterType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_FILTER_TYPE))
      .containsExactlyInAnyOrder("C", "D");
  }

  @Test
  @DisplayName("Check cartographyFilter.ValueType")
  void checkCartographyFilterValueType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_FILTER_VALUE_TYPE))
      .containsExactlyInAnyOrder("A", "N", "D");
  }

  @Test
  @DisplayName("Check cartographyParameter.format")
  void checkCartographyParameterFormat() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_PARAMETER_FORMAT))
      .containsExactlyInAnyOrder("I", "N", "P", "T", "U", "F");
  }

  @Test
  @DisplayName("Check cartographyParameter.type")
  void checkCartographyParameterType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("INFO");
  }

  @Test
  @DisplayName("Check cartographySpatialSelectionParameter.type")
  void checkCartographySpatialSelectionParameterType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("SELECT", "EDIT");
  }

  @Test
  @DisplayName("Check cartographyPermission.type")
  void checkCartographyPermissionType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_PERMISSION_TYPE))
      .containsExactlyInAnyOrder("C", CartographyPermission.TYPE_SITUATION_MAP, CartographyPermission.TYPE_BACKGROUND_MAP, "I");
  }

  @Test
  @DisplayName("Check downloadTask.format")
  void checkDownloadTaskFormat() {
    assertThat(select(CodeListsConstants.DOWNLOAD_TASK_FORMAT)).isEmpty();
  }

  @Test
  @DisplayName("Check downloadTask.scope")
  void checkDownloadTaskScope() {
    assertThat(select(CodeListsConstants.DOWNLOAD_TASK_SCOPE))
      .containsExactlyInAnyOrder("U", "A", "C");
  }

  @Test
  @DisplayName("Check queryTask.scope")
  void checkQueryTaskScope() {
    assertThat(select(CodeListsConstants.QUERY_TASK_SCOPE))
      .containsExactlyInAnyOrder("URL", "SQL", "WS", "INFORME", "TAREA");
  }

  @Test
  @DisplayName("Check service.nativeProtocol")
  void checkServiceNativeProtocol() {
    assertThat(select(CodeListsConstants.SERVICE_NATIVE_PROTOCOL))
      .isEmpty();
  }

  @Test
  @DisplayName("Check service.parameterType")
  void checkServiceParameterType() {
    assertThat(select(CodeListsConstants.SERVICE_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("INFO", "WMS", "OLPARAM", "WMTS");
  }

  @Test
  @DisplayName("Check service.type")
  void checkServiceType() {
    assertThat(select(CodeListsConstants.SERVICE_TYPE))
      .containsExactlyInAnyOrder("AIMS", "FME", "TC", "WFS", "WMS", "WMTS");
  }

  @Test
  @DisplayName("Check service.authenticationMode")
  void checkServiceAuthenticationMode() {
    assertThat(select(CodeListsConstants.SERVICE_AUTHENTICATION_MODE))
      .containsExactlyInAnyOrder("None", "HTTP Basic authentication");
  }

  @Test
  @DisplayName("Check taskParameter.type")
  void checkTaskParameterType() {
    assertThat(select(CodeListsConstants.TASK_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("CAMPO", "CAPA", "EDIT", "FILTRO", "FME", "GEOM", "LABEL",
        "RELM", "RELS", "SQL", "TIPO", "VISTA", "DATAINPUT", "VALOR");
  }

  @Test
  @DisplayName("Check taskParameter.format")
  void checkTaskParameterFormat() {
    assertThat(select(CodeListsConstants.TASK_PARAMETER_FORMAT))
      .containsExactlyInAnyOrder("T", "F", "N", "L", "U", "I", "C", "R", "S", "B");
  }

  @Test
  @DisplayName("Check territory.scope")
  @Deprecated
  void checkTerritoryScope() {
    assertThat(select(CodeListsConstants.TERRITORY_SCOPE)).containsExactlyInAnyOrder("M", "R", "T");
  }

  @Test
  @DisplayName("Check thematicMap.type")
  void checkThematicMapType() {
    assertThat(select(CodeListsConstants.THEMATIC_MAP_TYPE))
      .containsExactlyInAnyOrder("VU", "RE", "RL");
  }

  @Test
  @DisplayName("Check thematicMap.destination")
  void checkThematicMapDestination() {
    assertThat(select(CodeListsConstants.THEMATIC_MAP_DESTINATION))
      .containsExactlyInAnyOrder("WS", "WS_HERMES", "UPLOADED");
  }

  @Test
  @DisplayName("Check thematicMap.valueType")
  void checkThematicMapValueType() {
    assertThat(select(CodeListsConstants.THEMATIC_MAP_VALUE_TYPE))
      .containsExactlyInAnyOrder("STR", "DOU");
  }

  @Test
  @DisplayName("Check thematicMapRange.style")
  void checkThematicMapRangeStyle() {
    assertThat(select(CodeListsConstants.THEMATIC_MAP_RANGE_STYLE))
      .isEmpty();
  }

  @Test
  @DisplayName("Check user.identificationType")
  void checkUserIdentificationType() {
    assertThat(select(CodeListsConstants.USER_IDENTIFICATION_TYPE))
      .containsExactlyInAnyOrder("DNI", "NIE", "PAS");
  }

  @Test
  @DisplayName("Check userPosition.type")
  void checkUserPositionType() {
    assertThat(select(CodeListsConstants.USER_POSITION_TYPE))
      .containsExactlyInAnyOrder("AJ", "AR", "DB", "DM", "EM", "EN", "ER", "EX", "GN", "PR", "TS");
  }

  @TestConfiguration
  @Import(LiquibaseConfig.class)
  static class Configuration {
    @Bean
    @Primary
    TaskExecutor taskExecutor() {
      return new SyncTaskExecutor();
    }
  }
}