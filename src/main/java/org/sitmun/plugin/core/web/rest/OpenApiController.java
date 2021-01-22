package org.sitmun.plugin.core.web.rest;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.*;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.webmvc.api.OpenApiResource;
import org.springdoc.webmvc.api.RouterFunctionProvider;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@Profile({"!openapi-annotation"})
public class OpenApiController extends OpenApiResource {

  private final ConfigurableEnvironment environment;

  @Value("classpath:openapi/api.yaml")
  private Resource openAPIResource;

  private OpenAPI openAPI;

  /**
   * Instantiates a new Open api resource.
   *
   * @param openAPIBuilderObjectFactory    the open api builder object factory
   * @param requestBuilder                 the request builder
   * @param responseBuilder                the response builder
   * @param operationParser                the operation parser
   * @param requestMappingHandlerMapping   the request mapping handler mapping
   * @param actuatorProvider               the actuator provider
   * @param operationCustomizers           the operation customizers
   * @param openApiCustomisers             the open api customisers
   * @param springDocConfigProperties      the spring doc config properties
   * @param springSecurityOAuth2Provider   the spring security o auth 2 provider
   * @param routerFunctionProvider         the router function provider
   * @param repositoryRestResourceProvider the repository rest resource provider
   * @param environment                    the environment
   */
  public OpenApiController(
    ObjectFactory<OpenAPIBuilder> openAPIBuilderObjectFactory,
    AbstractRequestBuilder requestBuilder,
    GenericResponseBuilder responseBuilder, OperationBuilder operationParser,
    RequestMappingInfoHandlerMapping requestMappingHandlerMapping,
    Optional<ActuatorProvider> actuatorProvider,
    Optional<List<OperationCustomizer>> operationCustomizers,
    Optional<List<OpenApiCustomiser>> openApiCustomisers,
    SpringDocConfigProperties springDocConfigProperties,
    Optional<SecurityOAuth2Provider> springSecurityOAuth2Provider,
    Optional<RouterFunctionProvider> routerFunctionProvider,
    Optional<RepositoryRestResourceProvider> repositoryRestResourceProvider,
    ConfigurableEnvironment environment) {
    super(openAPIBuilderObjectFactory, requestBuilder, responseBuilder, operationParser,
      requestMappingHandlerMapping, actuatorProvider, operationCustomizers, openApiCustomisers,
      springDocConfigProperties, springSecurityOAuth2Provider, routerFunctionProvider,
      repositoryRestResourceProvider);
    this.environment = environment;
  }

  /**
   * Initialize OpenAPI 3.0 from a YAML description stored in {@code classpath:openapi/api.yaml.}
   *
   * @throws IOException if the content cannot be opened
   */
  @PostConstruct
  public void initOpenAPI() throws IOException {
    openAPI = getYamlMapper().readValue(openAPIResource.getInputStream(), OpenAPI.class);
    String currentDescription = openAPI.getInfo().getDescription();
    String activeProfiles = String.join(", ", environment.getActiveProfiles());
    openAPI.getInfo()
      .setDescription(currentDescription + "\n\n**Active profiles**: *" + activeProfiles + "*");
  }

  @Override
  protected synchronized OpenAPI getOpenApi() {
    return openAPI;
  }
}