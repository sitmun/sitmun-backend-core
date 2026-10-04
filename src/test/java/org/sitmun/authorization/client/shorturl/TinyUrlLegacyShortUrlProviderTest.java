package org.sitmun.authorization.client.shorturl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TinyUrlLegacyShortUrlProviderTest {

  @Test
  @DisplayName("a provider I/O failure returns a stable detail and drops the exception text")
  void ioFailureHidesExceptionMessage() throws IOException {
    OkHttpClient httpClient = mock(OkHttpClient.class);
    Call call = mock(Call.class);
    when(httpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenThrow(new IOException("connect to tinyurl.example failed"));

    ShortUrlOutcome outcome =
        new TinyUrlLegacyShortUrlProvider(new ShortUrlProperties(), httpClient)
            .shorten("http://lvh.me/public/map/12/4");

    assertThat(outcome).isInstanceOf(ShortUrlOutcome.Failed.class);
    assertThat(((ShortUrlOutcome.Failed) outcome).message())
        .isEqualTo("Short URL provider failed")
        .doesNotContain("tinyurl.example");
  }
}
