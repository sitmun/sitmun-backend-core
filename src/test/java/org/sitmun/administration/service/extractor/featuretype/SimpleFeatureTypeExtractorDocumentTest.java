package org.sitmun.administration.service.extractor.featuretype;

import static org.assertj.core.api.Assertions.assertThat;

import org.json.XML;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.service.extractor.featuretype.ExtractedMetadata.ExtractedMetadataBuilder;

@DisplayName("Feature type document classification")
class SimpleFeatureTypeExtractorDocumentTest {

  @Test
  @DisplayName("A DescribeLayer response is a successful document")
  void acceptsDescribeLayer() {
    ExtractedMetadata metadata =
        classify(
            """
            <sld:DescribeLayerResponse>
              <sld:LayerDescription name="roads" owsURL="https://example.test/wfs" owsType="WFS">
                <sld:Query typeName="app:roads"/>
              </sld:LayerDescription>
            </sld:DescribeLayerResponse>
            """);

    assertThat(metadata.getSuccess()).isTrue();
    assertThat(metadata.getType()).isEqualTo("DescribeLayer");
  }

  @Test
  @DisplayName("A service exception stays an unmanaged response")
  void rejectsServiceException() {
    ExtractedMetadata metadata =
        classify(
            """
            <ows:ExceptionReport>
              <ows:Exception exceptionCode="OperationNotSupported"/>
            </ows:ExceptionReport>
            """);

    assertThat(metadata.getSuccess()).isFalse();
    assertThat(metadata.getReason()).isEqualTo("Unmanaged XML response");
  }

  private static ExtractedMetadata classify(String xml) {
    ExtractedMetadataBuilder builder = ExtractedMetadata.builder().success(false);
    SimpleFeatureTypeExtractor.acceptDocument(builder, XML.toJSONObject(xml));
    return builder.build();
  }
}
