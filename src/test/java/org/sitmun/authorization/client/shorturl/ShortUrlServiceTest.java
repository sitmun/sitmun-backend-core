package org.sitmun.authorization.client.shorturl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.service.AuthorizationService;
import org.sitmun.domain.application.Application;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

class ShortUrlServiceTest {

  private final AuthorizationService authorizationService = mock(AuthorizationService.class);

  @Test
  @DisplayName("hash and path map routes on this host parse the application and territory")
  void parsesViewerMapRoutes() {
    assertThat(
            ViewerMapUrl.parse(
                "http://localhost:9000/viewer/#/public/map/12/4?mapState=tok", "localhost"))
        .contains(new ViewerMapUrl(12, 4));
    assertThat(ViewerMapUrl.parse("http://localhost:9000/viewer/user/map/12/34", "localhost"))
        .contains(new ViewerMapUrl(12, 34));
    assertThat(ViewerMapUrl.parse("http://localhost:9000/viewer/embedded-map/3/8/ca", "localhost"))
        .contains(new ViewerMapUrl(3, 8));
    assertThat(ViewerMapUrl.parse("https://evil.example/viewer/public/map/12/4", "localhost"))
        .isEmpty();
    assertThat(ViewerMapUrl.parse("http://localhost:9000/viewer/auth/login", "localhost"))
        .isEmpty();
  }

  @Test
  @DisplayName("a public visitor who can open the map receives the original URL and no shortener")
  void publicVisitorWithAccessKeepsTheUrl() {
    when(authorizationService.findApplicationByUserApplicationAndTerritory("public", 12, 4))
        .thenReturn(Optional.of(mock(Application.class)));
    ShortUrlProvider provider = new NoneShortUrlProvider();
    ShortUrlService service =
        new ShortUrlService(authorizationService, new SelectedShortUrlProvider(provider));

    String url =
        service.shorten(
            "public", "http://localhost:9000/viewer/public/map/12/4?mapState=tok", "localhost");

    assertThat(url).isEqualTo("http://localhost:9000/viewer/public/map/12/4?mapState=tok");
  }

  @Test
  @DisplayName("a public visitor who cannot open the map is forbidden before the provider runs")
  void publicVisitorWithoutAccessIsForbidden() {
    when(authorizationService.findApplicationByUserApplicationAndTerritory("public", 9, 9))
        .thenReturn(Optional.empty());
    ShortUrlProvider provider = mock(ShortUrlProvider.class);
    ShortUrlService service =
        new ShortUrlService(authorizationService, new SelectedShortUrlProvider(provider));

    assertThatThrownBy(
            () ->
                service.shorten(
                    "public", "http://localhost:9000/viewer/public/map/9/9", "localhost"))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(provider);
  }

  @Test
  @DisplayName("a URL that is not this viewer map is rejected before the provider runs")
  void foreignUrlIsRejected() {
    ShortUrlProvider provider = mock(ShortUrlProvider.class);
    ShortUrlService service =
        new ShortUrlService(authorizationService, new SelectedShortUrlProvider(provider));

    assertThatThrownBy(
            () -> service.shorten("public", "https://evil.example/public/map/12/4", "localhost"))
        .isInstanceOf(ResponseStatusException.class);
    verifyNoInteractions(provider);
    verifyNoInteractions(authorizationService);
  }

  @Test
  @DisplayName("a failed provider does not publish the original URL")
  void providerFailureDoesNotReturnTheOriginal() {
    when(authorizationService.findApplicationByUserApplicationAndTerritory("anna", 12, 4))
        .thenReturn(Optional.of(mock(Application.class)));
    ShortUrlProvider provider =
        new ShortUrlProvider() {
          @Override
          public String id() {
            return "broken";
          }

          @Override
          public ShortUrlOutcome shorten(String url) {
            return new ShortUrlOutcome.Failed("tinyurl down");
          }
        };
    ShortUrlService service =
        new ShortUrlService(authorizationService, new SelectedShortUrlProvider(provider));

    assertThatThrownBy(
            () ->
                service.shorten("anna", "http://localhost:9000/viewer/user/map/12/4", "localhost"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("tinyurl down");
  }

  @Test
  @DisplayName("startup fails when the configured provider id is missing or duplicated")
  void registryRejectsAMissingOrDuplicateId() {
    assertThatThrownBy(
            () -> ShortUrlConfiguration.select(List.of(new NoneShortUrlProvider()), "missing"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("missing");
    assertThatThrownBy(
            () ->
                ShortUrlConfiguration.select(
                    List.of(new NoneShortUrlProvider(), new NoneShortUrlProvider()), "none"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("none");
  }
}
