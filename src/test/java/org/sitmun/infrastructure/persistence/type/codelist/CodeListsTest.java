package org.sitmun.infrastructure.persistence.type.codelist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.domain.CodeListsConstants.*;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

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
            APPLICATION_PARAMETER_TYPE,
            APPLICATION_TYPE,
            CARTOGRAPHY_FILTER_TYPE,
            CARTOGRAPHY_FILTER_VALUE_TYPE,
            CARTOGRAPHY_GEOMETRY_TYPE,
            CARTOGRAPHY_LEGEND_TYPE,
            CARTOGRAPHY_PARAMETER_FORMAT,
            CARTOGRAPHY_PARAMETER_TYPE,
            CARTOGRAPHY_PERMISSION_TYPE,
            CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_FORMAT,
            CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE,
            DATABASE_CONNECTION_DRIVER,
            DOWNLOAD_TASK_SCOPE,
            EDIT_TASK_FIELD_TYPE,
            EDIT_TASK_SCOPE,
            MORE_INFO_TYPE,
            QUERY_TASK_PARAMETER_TYPE,
            QUERY_TASK_SCOPE,
            SERVICE_AUTHENTICATION_MODE,
            SERVICE_NATIVE_PROTOCOL,
            SERVICE_PARAMETER_TYPE,
            SERVICE_TYPE,
            TASK_ENTITY_JSON_PARAM_TYPE,
            TERRITORY_SCOPE,
            TREE_NODE_VIEWMODE,
            TREE_TYPE,
            TREE_NODE_TYPE,
            USER_IDENTIFICATION_TYPE,
            USER_POSITION_TYPE);
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
    assertThat(select(APPLICATION_TYPE)).containsExactlyInAnyOrder("I", "E", "ED", "T");
  }

  @Test
  @DisplayName("Verify applicationParameter.type code list values")
  void checkApplicationParameterType() {
    assertThat(select(APPLICATION_PARAMETER_TYPE))
        .containsExactlyInAnyOrder("MOBILE", "Nomenclator", "PRINT_TEMPLATE");
  }

  @Test
  @DisplayName("Verify cartography.geometryType code list values")
  void checkCartographyGeometryType() {
    assertThat(select(CARTOGRAPHY_GEOMETRY_TYPE))
        .containsExactlyInAnyOrder("POINT", "LINE", "POLYGON");
  }

  @Test
  @DisplayName("Verify cartography.legendType code list values")
  void checkCartographyLegendType() {
    assertThat(select(CARTOGRAPHY_LEGEND_TYPE))
        .containsExactlyInAnyOrder("LINK", "LEGENDGRAPHIC", "CAPABILITIES");
  }

  @Test
  @DisplayName("Verify cartographyFilter.Type code list values")
  void checkCartographyFilterType() {
    assertThat(select(CARTOGRAPHY_FILTER_TYPE)).containsExactlyInAnyOrder("C", "D");
  }

  @Test
  @DisplayName("Verify cartographyFilter.ValueType code list values")
  void checkCartographyFilterValueType() {
    assertThat(select(CARTOGRAPHY_FILTER_VALUE_TYPE)).containsExactlyInAnyOrder("A", "N", "D");
  }

  @Test
  @DisplayName("Verify cartographyParameter.format code list values")
  void checkCartographyParameterFormat() {
    assertThat(select(CARTOGRAPHY_PARAMETER_FORMAT))
        .containsExactlyInAnyOrder("I", "N", "P", "T", "U", "F");
  }

  @Test
  @DisplayName("Verify cartographyParameter.type code list values")
  void checkCartographyParameterType() {
    assertThat(select(CARTOGRAPHY_PARAMETER_TYPE)).containsExactlyInAnyOrder("INFO");
  }

  @Test
  @DisplayName("Verify cartographySpatialSelectionParameter.type code list values")
  void checkCartographySpatialSelectionParameterType() {
    assertThat(select(CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE))
        .containsExactlyInAnyOrder("SELECT", "EDIT");
  }

  @Test
  @DisplayName("Verify cartographyPermission.type code list values")
  void checkCartographyPermissionType() {
    assertThat(select(CARTOGRAPHY_PERMISSION_TYPE))
        .containsExactlyInAnyOrder(
            "C",
            CartographyPermission.TYPE_SITUATION_MAP,
            CartographyPermission.TYPE_BACKGROUND_MAP,
            "I");
  }

  @Test
  @DisplayName("Verify downloadTask.scope code list values")
  void checkDownloadTaskScope() {
    assertThat(select(DOWNLOAD_TASK_SCOPE)).containsExactlyInAnyOrder("U", "A", "C");
  }

  @Test
  @DisplayName("Verify queryTask.scope code list values")
  void checkQueryTaskScope() {
    assertThat(select(QUERY_TASK_SCOPE))
        .containsExactlyInAnyOrder(
            SCOPE_URL_QUERY,
            SCOPE_CARTOGRAPHY_QUERY,
            SCOPE_SQL_QUERY,
            SCOPE_WEB_API_QUERY,
            SCOPE_WEB_API_QUERY_NO_PROXY);
  }

  @Test
  @DisplayName("Verify service.nativeProtocol code list values")
  void checkServiceNativeProtocol() {
    assertThat(select(SERVICE_NATIVE_PROTOCOL)).containsExactlyInAnyOrder("NONE");
  }

  @Test
  @DisplayName("Verify service.parameterType code list values")
  void checkServiceParameterType() {
    assertThat(select(SERVICE_PARAMETER_TYPE))
        .containsExactlyInAnyOrder("INFO", "WMS", "OLPARAM", "WMTS");
  }

  @Test
  @DisplayName("Verify service.type code list values")
  void checkServiceType() {
    assertThat(select(SERVICE_TYPE))
        .containsExactlyInAnyOrder("AIMS", "FME", "TC", "WFS", "WMS", "WMTS");
  }

  @Test
  @DisplayName("Verify service.authenticationMode code list values")
  void checkServiceAuthenticationMode() {
    assertThat(select(SERVICE_AUTHENTICATION_MODE))
        .containsExactlyInAnyOrder("None", "HTTP Basic authentication", "API key");
  }

  @Test
  @DisplayName("Verify territory.scope code list values")
  @Deprecated
  void checkTerritoryScope() {
    assertThat(select(TERRITORY_SCOPE)).containsExactlyInAnyOrder("M", "R", "T");
  }

  @Test
  @DisplayName("Verify user.identificationType code list values")
  void checkUserIdentificationType() {
    assertThat(select(USER_IDENTIFICATION_TYPE)).containsExactlyInAnyOrder("DNI", "NIE", "PAS");
  }

  @Test
  @DisplayName("Verify userPosition.type code list values")
  void checkUserPositionType() {
    assertThat(select(USER_POSITION_TYPE))
        .containsExactlyInAnyOrder(
            "AJ", "AR", "DB", "DM", "EM", "EN", "ER", "EX", "GN", "PR", "TS");
  }

  @Test
  @DisplayName("Verify tree.type code list values")
  void checkTreeType() {
    assertThat(select(TREE_TYPE)).containsExactlyInAnyOrder("cartography", "edition", "touristic");
  }

  @Test
  @DisplayName("Verify treenode.node.type code list values")
  void checkTreeNodeNodeType() {
    assertThat(select(TREE_NODE_TYPE))
        .containsExactlyInAnyOrder(
            "cartography", "folder", "list", "menu", "task", "fav", "map", "nm");
  }
}
