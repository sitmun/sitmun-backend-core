package org.sitmun.authorization.client.shorturl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config/client")
@Validated
public class ShortUrlController {

  private final ShortUrlService shortUrlService;

  public ShortUrlController(ShortUrlService shortUrlService) {
    this.shortUrlService = shortUrlService;
  }

  @PostMapping("/short-url")
  public ResponseEntity<ShortUrlResponse> shorten(
      @Valid @RequestBody ShortUrlRequest body,
      @CurrentSecurityContext SecurityContext context,
      HttpServletRequest request) {
    String url =
        shortUrlService.shorten(
            context.getAuthentication().getName(), body.url(), request.getServerName());
    return ResponseEntity.ok(new ShortUrlResponse(url));
  }

  public record ShortUrlRequest(@NotBlank String url) {}

  public record ShortUrlResponse(String url) {}
}
