package org.sitmun.constraints;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.config.CodeLists;
import org.sitmun.common.domain.cartography.permission.CartographyPermission;
import org.sitmun.common.types.codelist.CodeListValue;
import org.sitmun.common.types.codelist.CodeListValueRepository;
import org.sitmun.common.types.codelist.QCodeListValue;
import org.sitmun.legacy.config.LiquibaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@DisplayName("CodeLists Test")
public class CodeListsTest {

  @Autowired
  private CodeListValueRepository codeListValueRepository;

  @Test
  @DisplayName("Check availability of CodeLists")
  public void checkAvailableCodeLists() {
    assertThat(codeListValueRepository.findDistinctCodeListName()).containsExactlyInAnyOrder(
      CodeLists.APPLICATION_PARAMETER_TYPE,
      CodeLists.APPLICATION_TYPE,
      CodeLists.CARTOGRAPHY_FILTER_TYPE,
      CodeLists.CARTOGRAPHY_FILTER_VALUE_TYPE,
      CodeLists.CARTOGRAPHY_GEOMETRY_TYPE,
      CodeLists.CARTOGRAPHY_LEGEND_TYPE,
      CodeLists.CARTOGRAPHY_PARAMETER_FORMAT,
      CodeLists.CARTOGRAPHY_PARAMETER_TYPE,
      CodeLists.CARTOGRAPHY_PERMISSION_TYPE,
      // DATABASE_CONNECTION_DRIVER,
      // DOWNLOAD_TASK_FORMAT,
      CodeLists.DOWNLOAD_TASK_SCOPE,
      CodeLists.QUERY_TASK_SCOPE,
      // SERVICE_NATIVE_PROTOCOL,
      CodeLists.SERVICE_PARAMETER_TYPE,
      CodeLists.SERVICE_TYPE,
      CodeLists.SERVICE_AUTHENTICATION_MODE,
      CodeLists.TASK_PARAMETER_FORMAT,
      CodeLists.TASK_PARAMETER_TYPE,
      CodeLists.TERRITORY_SCOPE,
      CodeLists.THEMATIC_MAP_DESTINATION,
      CodeLists.THEMATIC_MAP_TYPE,
      CodeLists.THEMATIC_MAP_VALUE_TYPE,
      // THEMATIC_MAP_RANGE_STYLE,
      CodeLists.USER_IDENTIFICATION_TYPE,
      CodeLists.USER_POSITION_TYPE,
      CodeLists.CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_FORMAT,
      CodeLists.CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE
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
  public void checkApplicationType() {
    assertThat(select(CodeLists.APPLICATION_TYPE))
      .containsExactlyInAnyOrder("I", "E");
  }

  @Test
  @DisplayName("Check applicationParameter.type")
  public void checkApplicationParameterType() {
    assertThat(select(CodeLists.APPLICATION_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("MOBILE", "Nomenclator", "PRINT_TEMPLATE");
  }

  @Test
  @DisplayName("Check cartography.geometryType")
  public void checkCartographyGeometryType() {
    assertThat(select(CodeLists.CARTOGRAPHY_GEOMETRY_TYPE))
      .containsExactlyInAnyOrder("POINT", "LINE", "POLYGON");
  }

  @Test
  @DisplayName("Check cartography.legendType")
  public void checkCartographyLegendType() {
    assertThat(select(CodeLists.CARTOGRAPHY_LEGEND_TYPE))
      .containsExactlyInAnyOrder("LINK", "LEGENDGRAPHIC", "CAPABILITIES");
  }

  @Test
  @DisplayName("Check cartographyFilter.Type")
  public void checkCartographyFilterType() {
    assertThat(select(CodeLists.CARTOGRAPHY_FILTER_TYPE))
      .containsExactlyInAnyOrder("C", "D");
  }

  @Test
  @DisplayName("Check cartographyFilter.ValueType")
  public void checkCartographyFilterValueType() {
    assertThat(select(CodeLists.CARTOGRAPHY_FILTER_VALUE_TYPE))
      .containsExactlyInAnyOrder("A", "N", "D");
  }

  @Test
  @DisplayName("Check cartographyParameter.format")
  public void checkCartographyParameterFormat() {
    assertThat(select(CodeLists.CARTOGRAPHY_PARAMETER_FORMAT))
      .containsExactlyInAnyOrder("I", "N", "P", "T", "U", "F");
  }

  @Test
  @DisplayName("Check cartographyParameter.type")
  public void checkCartographyParameterType() {
    assertThat(select(CodeLists.CARTOGRAPHY_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("INFO");
  }

  @Test
  @DisplayName("Check cartographySpatialSelectionParameter.type")
  public void checkCartographySpatialSelectionParameterType() {
    assertThat(select(CodeLists.CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("SELECT", "EDIT");
  }

  @Test
  @DisplayName("Check cartographyPermission.type")
  public void checkCartographyPermissionType() {
    assertThat(select(CodeLists.CARTOGRAPHY_PERMISSION_TYPE))
      .containsExactlyInAnyOrder("C", CartographyPermission.TYPE_SITUATION_MAP, CartographyPermission.TYPE_BACKGROUND_MAP, "I");
  }

  @Test
  @DisplayName("Check downloadTask.format")
  public void checkDownloadTaskFormat() {
    assertThat(select(CodeLists.DOWNLOAD_TASK_FORMAT)).isEmpty();
  }

  @Test
  @DisplayName("Check downloadTask.scope")
  public void checkDownloadTaskScope() {
    assertThat(select(CodeLists.DOWNLOAD_TASK_SCOPE))
      .containsExactlyInAnyOrder("U", "A", "C");
  }

  @Test
  @DisplayName("Check queryTask.scope")
  public void checkQueryTaskScope() {
    assertThat(select(CodeLists.QUERY_TASK_SCOPE))
      .containsExactlyInAnyOrder("URL", "SQL", "WS", "INFORME", "TAREA");
  }

  @Test
  @DisplayName("Check service.nativeProtocol")
  public void checkServiceNativeProtocol() {
    assertThat(select(CodeLists.SERVICE_NATIVE_PROTOCOL))
      .isEmpty();
  }

  @Test
  @DisplayName("Check service.parameterType")
  public void checkServiceParameterType() {
    assertThat(select(CodeLists.SERVICE_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("INFO", "WMS", "OLPARAM");
  }

  @Test
  @DisplayName("Check service.type")
  public void checkServiceType() {
    assertThat(select(CodeLists.SERVICE_TYPE))
      .containsExactlyInAnyOrder("AIMS", "FME", "TC", "WFS", "WMS");
  }

  @Test
  @DisplayName("Check service.authenticationMode")
  public void checkServiceAuthenticationMode() {
    assertThat(select(CodeLists.SERVICE_AUTHENTICATION_MODE))
      .containsExactlyInAnyOrder("None", "HTTP Basic authentication");
  }

  @Test
  @DisplayName("Check taskParameter.type")
  public void checkTaskParameterType() {
    assertThat(select(CodeLists.TASK_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("CAMPO", "CAPA", "EDIT", "FILTRO", "FME", "GEOM", "LABEL",
        "RELM", "RELS", "SQL", "TIPO", "VISTA", "DATAINPUT", "VALOR");
  }

  @Test
  @DisplayName("Check taskParameter.format")
  public void checkTaskParameterFormat() {
    assertThat(select(CodeLists.TASK_PARAMETER_FORMAT))
      .containsExactlyInAnyOrder("T", "F", "N", "L", "U", "I", "C", "R", "S", "B");
  }

  @Test
  @DisplayName("Check territory.scope")
  @Deprecated
  public void checkTerritoryScope() {
    assertThat(select(CodeLists.TERRITORY_SCOPE)).containsExactlyInAnyOrder("M", "R", "T");
  }

  @Test
  @DisplayName("Check thematicMap.type")
  public void checkThematicMapType() {
    assertThat(select(CodeLists.THEMATIC_MAP_TYPE))
      .containsExactlyInAnyOrder("VU", "RE", "RL");
  }

  @Test
  @DisplayName("Check thematicMap.destination")
  public void checkThematicMapDestination() {
    assertThat(select(CodeLists.THEMATIC_MAP_DESTINATION))
      .containsExactlyInAnyOrder("WS", "WS_HERMES", "UPLOADED");
  }

  @Test
  @DisplayName("Check thematicMap.valueType")
  public void checkThematicMapValueType() {
    assertThat(select(CodeLists.THEMATIC_MAP_VALUE_TYPE))
      .containsExactlyInAnyOrder("STR", "DOU");
  }

  @Test
  @DisplayName("Check thematicMapRange.style")
  public void checkThematicMapRangeStyle() {
    assertThat(select(CodeLists.THEMATIC_MAP_RANGE_STYLE))
      .isEmpty();
  }

  @Test
  @DisplayName("Check user.identificationType")
  public void checkUserIdentificationType() {
    assertThat(select(CodeLists.USER_IDENTIFICATION_TYPE))
      .containsExactlyInAnyOrder("DNI", "NIE", "PAS");
  }

  @Test
  @DisplayName("Check userPosition.type")
  public void checkUserPositionType() {
    assertThat(select(CodeLists.USER_POSITION_TYPE))
      .containsExactlyInAnyOrder("AJ", "AR", "DB", "DM", "EM", "EN", "ER", "EX", "GN", "PR", "TS");
  }

  @TestConfiguration
  @Import(LiquibaseConfig.class)
  static class Configuration {
    @Bean
    @Primary
    public TaskExecutor taskExecutor() {
      return new SyncTaskExecutor();
    }
  }
}