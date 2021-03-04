package org.sitmun.plugin.core.constraints;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.CartographyPermission;
import org.sitmun.plugin.core.domain.CodeListValue;
import org.sitmun.plugin.core.domain.QCodeListValue;
import org.sitmun.plugin.core.repository.CodeListValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.plugin.core.constraints.CodeLists.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("dev")
public class CodeListsTest {

  @Autowired
  private CodeListValueRepository codeListValueRepository;

  @Test
  public void checkAvailableCodeLists() {
    assertThat(codeListValueRepository.findDistinctCodeListName()).containsExactlyInAnyOrder(
      APPLICATION_TYPE,
      TERRITORY_SCOPE,
      USER_IDENTIFICATION_TYPE,
      CARTOGRAPHY_LEGEND_TYPE,
      CARTOGRAPHY_GEOMETRY_TYPE,
      CARTOGRAPHY_FILTER_TYPE,
      CARTOGRAPHY_FILTER_VALUE_TYPE,
      CARTOGRAPHY_PARAMETER_FORMAT,
      CARTOGRAPHY_PARAMETER_TYPE,
      CARTOGRAPHY_PERMISSION_TYPE,
      APPLICATION_PARAMETER_TYPE,
      SERVICE_TYPE,
      // SERVICE_NATIVE_PROTOCOL,
      SERVICE_PARAMETER_TYPE,
      TASK_PARAMETER_TYPE,
      TASK_PARAMETER_FORMAT,
      // DOWNLOAD_TASK_FORMAT,
      DOWNLOAD_TASK_SCOPE,
      QUERY_TASK_SCOPE,
      USER_POSITION_TYPE,
      THEMATIC_MAP_TYPE,
      THEMATIC_MAP_VALUE_TYPE,
      THEMATIC_MAP_DESTINATION
      // THEMATIC_MAP_RANGE_STYLE
    );
  }

  private Iterable<String> select(String list) {
    return StreamSupport.stream(
      codeListValueRepository.findAll(QCodeListValue.codeListValue.codeListName.eq(list))
        .spliterator(), false)
      .map(CodeListValue::getValue).collect(Collectors.toList());
  }

  @Test
  public void checkApplicationType() {
    assertThat(select(APPLICATION_TYPE))
      .containsExactlyInAnyOrder("I", "E");
  }

  @Test
  public void checkTerritoryScope() {
    assertThat(select(TERRITORY_SCOPE)).containsExactlyInAnyOrder("M", "R", "T");
  }

  @Test
  public void checkUserIdentificationType() {
    assertThat(select(USER_IDENTIFICATION_TYPE))
      .containsExactlyInAnyOrder("DNI", "NIE", "PAS");
  }

  @Test
  public void checkCartographyLegendType() {
    assertThat(select(CARTOGRAPHY_LEGEND_TYPE))
      .containsExactlyInAnyOrder("LINK", "LEGENDGRAPHIC", "CAPABILITIES");
  }

  @Test
  public void checkCartographyGeometryType() {
    assertThat(select(CARTOGRAPHY_GEOMETRY_TYPE))
      .containsExactlyInAnyOrder("POINT", "LINE", "POLYGON");
  }

  @Test
  public void checkCartographyFilterType() {
    assertThat(select(CARTOGRAPHY_FILTER_TYPE))
      .containsExactlyInAnyOrder("C", "D");
  }

  @Test
  public void checkCartographyFilterValueType() {
    assertThat(select(CARTOGRAPHY_FILTER_VALUE_TYPE))
      .containsExactlyInAnyOrder("A", "N", "D");
  }

  @Test
  public void checkCartographyParameterFormat() {
    assertThat(select(CARTOGRAPHY_PARAMETER_FORMAT))
      .containsExactlyInAnyOrder("Imagen", "número", "porcentaje", "texto", "URL", "fecha");
  }

  @Test
  public void checkCartographyParameterType() {
    assertThat(select(CARTOGRAPHY_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("INFO", "SELECT", "INFOSELECT", "FILTRO_INFO", "FILTRO_ESPACIAL",
        "FILTRO", "EDIT");
  }

  @Test
  public void checkCartographyPermissionType() {
    assertThat(select(CARTOGRAPHY_PERMISSION_TYPE))
      .containsExactlyInAnyOrder("C", CartographyPermission.TYPE_SITUATION_MAP, CartographyPermission.TYPE_BACKGROUND_MAP, "I");
  }

  @Test
  public void checkApplicationParameterType() {
    assertThat(select(APPLICATION_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("MOBILE", "Nomenclator", "PRINT_TEMPLATE");
  }

  @Test
  public void checkServiceType() {
    assertThat(select(SERVICE_TYPE))
      .containsExactlyInAnyOrder("AIMS", "FME", "TC", "WFS", "WMS");
  }

  @Test
  public void checkServiceNativeProtocol() {
    assertThat(select(SERVICE_NATIVE_PROTOCOL))
      .isEmpty();
  }

  @Test
  public void checkServiceParameterType() {
    assertThat(select(SERVICE_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("INFO", "WMS", "OLPARAM");
  }

  @Test
  public void checkTaskParameterType() {
    assertThat(select(TASK_PARAMETER_TYPE))
      .containsExactlyInAnyOrder("CAMPO", "CAPA", "EDIT", "FILTRO", "FME", "GEOM", "LABEL",
        "RELM", "RELS", "SQL", "TIPO", "VISTA", "DATAINPUT", "VALOR");
  }

  @Test
  public void checkTaskParameterFormat() {
    assertThat(select(TASK_PARAMETER_FORMAT))
      .containsExactlyInAnyOrder("T", "F", "N", "L", "U", "I", "C", "R", "S", "B");
  }

  @Test
  public void checkDownloadTaskFormat() {
    assertThat(select(DOWNLOAD_TASK_FORMAT)).isEmpty();
  }

  @Test
  public void checkDownloadTaskScope() {
    assertThat(select(DOWNLOAD_TASK_SCOPE))
      .containsExactlyInAnyOrder("U", "A", "C");
  }

  @Test
  public void checkQueryTaskScope() {
    assertThat(select(QUERY_TASK_SCOPE))
      .containsExactlyInAnyOrder("URL", "SQL", "WS", "INFORME", "TAREA");
  }

  @Test
  public void checkUserPositionType() {
    assertThat(select(USER_POSITION_TYPE))
      .containsExactlyInAnyOrder("RE");
  }

  @Test
  public void checkThematicMapType() {
    assertThat(select(THEMATIC_MAP_TYPE))
      .containsExactlyInAnyOrder("VU", "RE", "RL");
  }

  @Test
  public void checkThematicMapValueType() {
    assertThat(select(THEMATIC_MAP_VALUE_TYPE))
      .containsExactlyInAnyOrder("STR", "DOU");
  }

  @Test
  public void checkThematicMapDestination() {
    assertThat(select(THEMATIC_MAP_DESTINATION))
      .containsExactlyInAnyOrder("WS", "WS_HERMES", "UPLOADED");
  }

  @Test
  public void checkThematicMapRangeStyle() {
    assertThat(select(THEMATIC_MAP_RANGE_STYLE))
      .isEmpty();
  }
}