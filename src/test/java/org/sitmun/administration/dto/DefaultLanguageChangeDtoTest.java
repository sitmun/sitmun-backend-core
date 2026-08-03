package org.sitmun.administration.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class DefaultLanguageChangeDtoTest {

  @Autowired private JacksonTester<DefaultLanguageChangeRequest> requestTester;

  @Autowired private JacksonTester<DefaultLanguageChangePreview> previewTester;

  @Autowired private JacksonTester<DefaultLanguageChangeResult> resultTester;

  @Autowired private JacksonTester<MissingTranslationDto> missingTester;

  @Test
  void shouldSerializeAndDeserializeRequest() throws Exception {
    var request = new DefaultLanguageChangeRequest("en", "ca", false);

    var json = requestTester.write(request);
    assertThat(json).extractingJsonPathStringValue("$.from").isEqualTo("en");
    assertThat(json).extractingJsonPathStringValue("$.to").isEqualTo("ca");
    assertThat(json)
        .extractingJsonPathBooleanValue("$.continueOnMissingTranslations")
        .isEqualTo(false);

    var parsed = requestTester.parseObject(json.getJson());
    assertThat(parsed.from()).isEqualTo("en");
    assertThat(parsed.to()).isEqualTo("ca");
    assertThat(parsed.continueOnMissingTranslations()).isEqualTo(false);
  }

  @Test
  void shouldSerializeAndDeserializePreview() throws Exception {
    var missing =
        List.of(
            new MissingTranslationDto("Application", 1, "Application.name", "Test App"),
            new MissingTranslationDto("Language", 2, "Language.name", "English"));

    var preview = new DefaultLanguageChangePreview("en", "ca", 25, 25, 20, 2, missing, 0);

    var json = previewTester.write(preview);
    assertThat(json).extractingJsonPathStringValue("$.currentDefault").isEqualTo("en");
    assertThat(json).extractingJsonPathStringValue("$.requestedDefault").isEqualTo("ca");
    assertThat(json).extractingJsonPathNumberValue("$.affectedValues").isEqualTo(25);
    assertThat(json).extractingJsonPathNumberValue("$.backupUpserts").isEqualTo(25);
    assertThat(json).extractingJsonPathNumberValue("$.restoredValues").isEqualTo(20);
    assertThat(json).extractingJsonPathNumberValue("$.missingTranslations").isEqualTo(2);
    assertThat(json).extractingJsonPathArrayValue("$.missing").hasSize(2);

    var parsed = previewTester.parseObject(json.getJson());
    assertThat(parsed.currentDefault()).isEqualTo("en");
    assertThat(parsed.requestedDefault()).isEqualTo("ca");
    assertThat(parsed.affectedValues()).isEqualTo(25);
    assertThat(parsed.backupUpserts()).isEqualTo(25);
    assertThat(parsed.restoredValues()).isEqualTo(20);
    assertThat(parsed.missingTranslations()).isEqualTo(2);
    assertThat(parsed.missing()).hasSize(2);
  }

  @Test
  void shouldSerializeAndDeserializeResult() throws Exception {
    var preserved =
        List.of(new MissingTranslationDto("Service", 5, "Service.description", "WMS Service"));

    var result = new DefaultLanguageChangeResult("en", "ca", 30, 28, 2, preserved, 0);

    var json = resultTester.write(result);
    assertThat(json).extractingJsonPathStringValue("$.previousDefault").isEqualTo("en");
    assertThat(json).extractingJsonPathStringValue("$.currentDefault").isEqualTo("ca");
    assertThat(json).extractingJsonPathNumberValue("$.backupUpserts").isEqualTo(30);
    assertThat(json).extractingJsonPathNumberValue("$.restoredValues").isEqualTo(28);
    assertThat(json).extractingJsonPathNumberValue("$.preservedValues").isEqualTo(2);
    assertThat(json).extractingJsonPathArrayValue("$.preservedMissing").hasSize(1);

    var parsed = resultTester.parseObject(json.getJson());
    assertThat(parsed.previousDefault()).isEqualTo("en");
    assertThat(parsed.currentDefault()).isEqualTo("ca");
    assertThat(parsed.backupUpserts()).isEqualTo(30);
    assertThat(parsed.restoredValues()).isEqualTo(28);
    assertThat(parsed.preservedValues()).isEqualTo(2);
    assertThat(parsed.preservedMissing()).hasSize(1);
  }

  @Test
  void shouldSerializeAndDeserializeMissingTranslation() throws Exception {
    var missing = new MissingTranslationDto("Territory", 10, "Territory.name", "Catalonia");

    var json = missingTester.write(missing);
    assertThat(json).extractingJsonPathStringValue("$.entity").isEqualTo("Territory");
    assertThat(json).extractingJsonPathNumberValue("$.element").isEqualTo(10);
    assertThat(json).extractingJsonPathStringValue("$.column").isEqualTo("Territory.name");
    assertThat(json).extractingJsonPathStringValue("$.currentValue").isEqualTo("Catalonia");

    var parsed = missingTester.parseObject(json.getJson());
    assertThat(parsed.entity()).isEqualTo("Territory");
    assertThat(parsed.element()).isEqualTo(10);
    assertThat(parsed.column()).isEqualTo("Territory.name");
    assertThat(parsed.currentValue()).isEqualTo("Catalonia");
  }
}
