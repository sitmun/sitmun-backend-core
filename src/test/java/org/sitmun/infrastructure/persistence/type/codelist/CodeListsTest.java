package org.sitmun.infrastructure.persistence.type.codelist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.infrastructure.persistence.config.LiquibaseConfig;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@SpringBootTest
@Import(TranslationService.class)
@DisplayName("Code Lists Integration Test")
class CodeListsTest {

  @Autowired private CodeListValueRepository codeListValueRepository;

  @Test
  @DisplayName("Verify all code list constants are available in database")
  void verifyAllCodeListConstantsAvailable() {
    assertThat(codeListValueRepository.findDistinctCodeListName())
        .containsExactlyInAnyOrder(
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
            // CodeListsConstants.TREE_NODE_LEAF_TYPE,
            // CodeListsConstants.TREE_NODE_FOLDER_TYPE
            "databaseConnection.driver",
            "queryTask.parameterType",
            "taskEntity.jsonParamType",
            "taskEntity.queryType",
            "treenode.folder.type",
            "treenode.leaf.type",
            "treenode.viewmode",
            "editTask.fieldType",
            "editTask.scope");
  }

  private Iterable<String> select(String list) {
    return StreamSupport.stream(
            codeListValueRepository
                .findAll(QCodeListValue.codeListValue.codeListName.eq(list))
                .spliterator(),
            false)
        .map(CodeListValue::getValue)
        .collect(Collectors.toList());
  }

  @Test
  @DisplayName("Verify application.type code list values")
  void checkApplicationType() {
    assertThat(select(CodeListsConstants.APPLICATION_TYPE))
        .containsExactlyInAnyOrder("I", "E", "ED", "T");
  }

  @Test
  @DisplayName("Verify applicationParameter.type code list values")
  void checkApplicationParameterType() {
    assertThat(select(CodeListsConstants.APPLICATION_PARAMETER_TYPE))
        .containsExactlyInAnyOrder("MOBILE", "Nomenclator", "PRINT_TEMPLATE");
  }

  @Test
  @DisplayName("Verify cartography.geometryType code list values")
  void checkCartographyGeometryType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_GEOMETRY_TYPE))
        .containsExactlyInAnyOrder("POINT", "LINE", "POLYGON");
  }

  @Test
  @DisplayName("Verify cartography.legendType code list values")
  void checkCartographyLegendType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_LEGEND_TYPE))
        .containsExactlyInAnyOrder("LINK", "LEGENDGRAPHIC", "CAPABILITIES");
  }

  @Test
  @DisplayName("Verify cartographyFilter.Type code list values")
  void checkCartographyFilterType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_FILTER_TYPE))
        .containsExactlyInAnyOrder("C", "D");
  }

  @Test
  @DisplayName("Verify cartographyFilter.ValueType code list values")
  void checkCartographyFilterValueType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_FILTER_VALUE_TYPE))
        .containsExactlyInAnyOrder("A", "N", "D");
  }

  @Test
  @DisplayName("Verify cartographyParameter.format code list values")
  void checkCartographyParameterFormat() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_PARAMETER_FORMAT))
        .containsExactlyInAnyOrder("I", "N", "P", "T", "U", "F");
  }

  @Test
  @DisplayName("Verify cartographyParameter.type code list values")
  void checkCartographyParameterType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_PARAMETER_TYPE))
        .containsExactlyInAnyOrder("INFO");
  }

  @Test
  @DisplayName("Verify cartographySpatialSelectionParameter.type code list values")
  void checkCartographySpatialSelectionParameterType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE))
        .containsExactlyInAnyOrder("SELECT", "EDIT");
  }

  @Test
  @DisplayName("Verify cartographyPermission.type code list values")
  void checkCartographyPermissionType() {
    assertThat(select(CodeListsConstants.CARTOGRAPHY_PERMISSION_TYPE))
        .containsExactlyInAnyOrder(
            "C",
            CartographyPermission.TYPE_SITUATION_MAP,
            CartographyPermission.TYPE_BACKGROUND_MAP,
            "I");
  }

  @Test
  @DisplayName("Verify downloadTask.format code list values")
  void checkDownloadTaskFormat() {
    assertThat(select(CodeListsConstants.DOWNLOAD_TASK_FORMAT)).isEmpty();
  }

  @Test
  @DisplayName("Verify downloadTask.scope code list values")
  void checkDownloadTaskScope() {
    assertThat(select(CodeListsConstants.DOWNLOAD_TASK_SCOPE))
        .containsExactlyInAnyOrder("U", "A", "C");
  }

  @Test
  @DisplayName("Verify queryTask.scope code list values")
  void checkQueryTaskScope() {
    assertThat(select(CodeListsConstants.QUERY_TASK_SCOPE))
        .containsExactlyInAnyOrder(
            "INFORME",
            "SQL",
            "TAREA",
            "URL",
            "WS",
            "cartography-query",
            "sql-query",
            "web-api-query");
  }

  @Test
  @DisplayName("Verify service.nativeProtocol code list values")
  void checkServiceNativeProtocol() {
    assertThat(select(CodeListsConstants.SERVICE_NATIVE_PROTOCOL)).isEmpty();
  }

  @Test
  @DisplayName("Verify service.parameterType code list values")
  void checkServiceParameterType() {
    assertThat(select(CodeListsConstants.SERVICE_PARAMETER_TYPE))
        .containsExactlyInAnyOrder("INFO", "WMS", "OLPARAM", "WMTS");
  }

  @Test
  @DisplayName("Verify service.type code list values")
  void checkServiceType() {
    assertThat(select(CodeListsConstants.SERVICE_TYPE))
        .containsExactlyInAnyOrder("AIMS", "FME", "TC", "WFS", "WMS", "WMTS");
  }

  @Test
  @DisplayName("Verify service.authenticationMode code list values")
  void checkServiceAuthenticationMode() {
    assertThat(select(CodeListsConstants.SERVICE_AUTHENTICATION_MODE))
        .containsExactlyInAnyOrder("None", "HTTP Basic authentication");
  }

  @Test
  @DisplayName("Verify taskParameter.type code list values")
  void checkTaskParameterType() {
    assertThat(select(CodeListsConstants.TASK_PARAMETER_TYPE))
        .containsExactlyInAnyOrder(
            "CAMPO",
            "CAPA",
            "EDIT",
            "FILTRO",
            "FME",
            "GEOM",
            "LABEL",
            "RELM",
            "RELS",
            "SQL",
            "TIPO",
            "VISTA",
            "DATAINPUT",
            "VALOR");
  }

  @Test
  @DisplayName("Verify taskParameter.format code list values")
  void checkTaskParameterFormat() {
    assertThat(select(CodeListsConstants.TASK_PARAMETER_FORMAT))
        .containsExactlyInAnyOrder("T", "F", "N", "L", "U", "I", "C", "R", "S", "B");
  }

  @Test
  @DisplayName("Verify territory.scope code list values")
  @Deprecated
  void checkTerritoryScope() {
    assertThat(select(CodeListsConstants.TERRITORY_SCOPE)).containsExactlyInAnyOrder("M", "R", "T");
  }

  @Test
  @DisplayName("Verify thematicMap.type code list values")
  void checkThematicMapType() {
    assertThat(select(CodeListsConstants.THEMATIC_MAP_TYPE))
        .containsExactlyInAnyOrder("VU", "RE", "RL");
  }

  @Test
  @DisplayName("Verify thematicMap.destination code list values")
  void checkThematicMapDestination() {
    assertThat(select(CodeListsConstants.THEMATIC_MAP_DESTINATION))
        .containsExactlyInAnyOrder("WS", "WS_HERMES", "UPLOADED");
  }

  @Test
  @DisplayName("Verify thematicMap.valueType code list values")
  void checkThematicMapValueType() {
    assertThat(select(CodeListsConstants.THEMATIC_MAP_VALUE_TYPE))
        .containsExactlyInAnyOrder("STR", "DOU");
  }

  @Test
  @DisplayName("Verify thematicMapRange.style code list values")
  void checkThematicMapRangeStyle() {
    assertThat(select(CodeListsConstants.THEMATIC_MAP_RANGE_STYLE)).isEmpty();
  }

  @Test
  @DisplayName("Verify user.identificationType code list values")
  void checkUserIdentificationType() {
    assertThat(select(CodeListsConstants.USER_IDENTIFICATION_TYPE))
        .containsExactlyInAnyOrder("DNI", "NIE", "PAS");
  }

  @Test
  @DisplayName("Verify userPosition.type code list values")
  void checkUserPositionType() {
    assertThat(select(CodeListsConstants.USER_POSITION_TYPE))
        .containsExactlyInAnyOrder(
            "AJ", "AR", "DB", "DM", "EM", "EN", "ER", "EX", "GN", "PR", "TS");
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
