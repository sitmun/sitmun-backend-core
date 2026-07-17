package org.sitmun.infrastructure.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

@DisplayName("OpenAPI application contact documentation")
class OpenApiApplicationContactDocumentationTest {

  @Test
  @DisplayName("api-docs-admin documents Application.responsibleInstitutionName")
  void adminDocumentsResponsibleInstitution() throws Exception {
    Map<String, Object> application = schema("static/v3/api-docs-admin.yaml", "Application");
    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) application.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> field = (Map<String, Object>) properties.get("responsibleInstitutionName");

    assertThat(field).isNotNull();
    assertThat(field.get("type")).isEqualTo("string");
    assertThat(field.get("maxLength")).isEqualTo(250);
    assertThat(String.valueOf(field.get("description")))
        .containsIgnoringCase("institution responsible");
  }

  @Test
  @DisplayName("api-docs-conf documents Application.responsibleInstitutionName and pointOfContact")
  void confDocumentsInstitutionAndPointOfContact() throws Exception {
    Map<String, Object> application = schema("static/v3/api-docs-conf.yaml", "Application");
    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) application.get("properties");

    @SuppressWarnings("unchecked")
    Map<String, Object> institution =
        (Map<String, Object>) properties.get("responsibleInstitutionName");
    assertThat(institution).isNotNull();
    assertThat(institution.get("type")).isEqualTo("string");
    assertThat(institution.get("maxLength")).isEqualTo(250);

    @SuppressWarnings("unchecked")
    Map<String, Object> pointOfContact = (Map<String, Object>) properties.get("pointOfContact");
    assertThat(pointOfContact).isNotNull();
    assertThat(pointOfContact.get("type")).isEqualTo("string");
    assertThat(String.valueOf(pointOfContact.get("description"))).containsIgnoringCase("email");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> schema(String classpath, String schemaName) throws Exception {
    try (InputStream in =
        OpenApiApplicationContactDocumentationTest.class
            .getClassLoader()
            .getResourceAsStream(classpath)) {
      assertThat(in).as(classpath).isNotNull();
      Map<String, Object> root = new Yaml().load(in);
      Map<String, Object> components = (Map<String, Object>) root.get("components");
      Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
      Map<String, Object> schema = (Map<String, Object>) schemas.get(schemaName);
      assertThat(schema).as(schemaName).isNotNull();
      return schema;
    }
  }
}
