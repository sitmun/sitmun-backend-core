package org.sitmun.domain.application;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;
import java.net.URISyntaxException;

/** Validates external application URL requirements on {@link Application}. */
public class ExternalApplicationUrlValidator
    implements ConstraintValidator<ValidExternalApplicationUrl, Application> {

  private static final String TYPE_EXTERNAL = "E";
  private static final String FIELD_JSP_TEMPLATE = "jspTemplate";

  @Override
  public boolean isValid(Application app, ConstraintValidatorContext context) {
    if (app == null || app.getType() == null || !TYPE_EXTERNAL.equals(app.getType())) {
      return true;
    }
    String url = app.getJspTemplate();
    if (url == null || url.isBlank()) {
      return fail(context, "entity.application.error.externalUrlRequired");
    }
    if (!isHttpOrHttpsUrl(url.trim())) {
      return fail(context, "entity.application.error.optionalHttpUrl");
    }
    return true;
  }

  private static boolean isHttpOrHttpsUrl(String value) {
    try {
      URI uri = new URI(value);
      String scheme = uri.getScheme();
      return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
          && uri.getHost() != null;
    } catch (URISyntaxException e) {
      return false;
    }
  }

  private static boolean fail(ConstraintValidatorContext context, String message) {
    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(message)
        .addPropertyNode(FIELD_JSP_TEMPLATE)
        .addConstraintViolation();
    return false;
  }
}
