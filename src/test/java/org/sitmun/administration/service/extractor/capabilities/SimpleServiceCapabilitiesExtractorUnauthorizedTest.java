package org.sitmun.administration.service.extractor.capabilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.administration.service.extractor.HttpClientFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimpleServiceCapabilitiesExtractor unauthorized")
class SimpleServiceCapabilitiesExtractorUnauthorizedTest {

  @Mock private HttpClientFactory httpClientFactory;
  @InjectMocks private SimpleServiceCapabilitiesExtractor extractor;

  @Test
  @DisplayName("Upstream 401 is a credentials reason, not an XML parse failure")
  void unauthorizedIsNotXmlParseFailure() throws IOException {
    Request request =
        new Request.Builder().url("https://example.com/wms").header("Accept", "*/*").build();
    Response response =
        new Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body(ResponseBody.create("html login", MediaType.parse("text/html")))
            .build();
    when(httpClientFactory.executeRequest(any(Request.class))).thenReturn(response);

    ExtractedMetadata doc = extractor.extract(request);

    assertThat(doc.getSuccess()).isFalse();
    assertThat(doc.getReason()).isEqualTo("Unauthorized");
    assertThat(doc.getAsText()).isEqualTo("html login");
  }
}
