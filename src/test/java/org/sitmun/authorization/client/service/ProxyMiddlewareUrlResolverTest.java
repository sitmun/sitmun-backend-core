package org.sitmun.authorization.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.SitmunConstants;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProxyMiddlewareUrlResolver")
class ProxyMiddlewareUrlResolverTest {

  private static final String SPRING_FALLBACK = "http://localhost:8080/middleware";

  @Mock private ConfigurationParameterRepository configurationParameterRepository;
  @Mock private PlatformTransactionManager transactionManager;

  private ProxyMiddlewareUrlResolver resolver;

  @BeforeEach
  void setUp() {
    lenient()
        .when(transactionManager.getTransaction(any(TransactionDefinition.class)))
        .thenReturn(new SimpleTransactionStatus());
    resolver =
        new ProxyMiddlewareUrlResolver(
            configurationParameterRepository, SPRING_FALLBACK, transactionManager);
  }

  @Test
  @DisplayName("DB non-blank wins over Spring, is normalized, and persisted when unnormalized")
  void dbWinsNormalizedAndPersisted() {
    ConfigurationParameter row = parameter("https://cdn.example.com:443/middleware/");
    when(configurationParameterRepository.findByName(SitmunConstants.PROXY_CONF_KEY))
        .thenReturn(Optional.of(row));
    when(configurationParameterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    assertThat(resolver.resolve()).isEqualTo("https://cdn.example.com/middleware");

    ArgumentCaptor<ConfigurationParameter> captor =
        ArgumentCaptor.forClass(ConfigurationParameter.class);
    verify(configurationParameterRepository).save(captor.capture());
    assertThat(captor.getValue().getValue()).isEqualTo("https://cdn.example.com/middleware");
  }

  @Test
  @DisplayName("Already-normalized DB value is not rewritten")
  void alreadyNormalizedNotRewritten() {
    when(configurationParameterRepository.findByName(SitmunConstants.PROXY_CONF_KEY))
        .thenReturn(Optional.of(parameter("https://cdn.example.com/middleware")));

    assertThat(resolver.resolve()).isEqualTo("https://cdn.example.com/middleware");
    verify(configurationParameterRepository, never()).save(any());
  }

  @Test
  @DisplayName("Missing DB row falls back to normalized Spring URL without insert")
  void missingDbUsesSpring() {
    when(configurationParameterRepository.findByName(SitmunConstants.PROXY_CONF_KEY))
        .thenReturn(Optional.empty());

    ProxyMiddlewareUrlResolver custom =
        new ProxyMiddlewareUrlResolver(
            configurationParameterRepository, "http://host:80/middleware", transactionManager);

    assertThat(custom.resolve()).isEqualTo("http://host/middleware");
    verify(configurationParameterRepository, never()).save(any());
  }

  @Test
  @DisplayName("Blank DB value falls back to Spring URL and persists it")
  void blankDbUsesSpringAndPersists() {
    ConfigurationParameter row = parameter("   ");
    when(configurationParameterRepository.findByName(SitmunConstants.PROXY_CONF_KEY))
        .thenReturn(Optional.of(row));
    when(configurationParameterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    assertThat(resolver.resolve()).isEqualTo(SPRING_FALLBACK);

    ArgumentCaptor<ConfigurationParameter> captor =
        ArgumentCaptor.forClass(ConfigurationParameter.class);
    verify(configurationParameterRepository).save(captor.capture());
    assertThat(captor.getValue().getValue()).isEqualTo(SPRING_FALLBACK);
    verify(transactionManager).commit(any(TransactionStatus.class));
  }

  @Test
  @DisplayName("Empty DB value falls back to Spring URL and persists it")
  void emptyDbUsesSpringAndPersists() {
    ConfigurationParameter row = parameter("");
    when(configurationParameterRepository.findByName(SitmunConstants.PROXY_CONF_KEY))
        .thenReturn(Optional.of(row));
    when(configurationParameterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    assertThat(resolver.resolve()).isEqualTo(SPRING_FALLBACK);
    verify(configurationParameterRepository).save(any());
  }

  @Test
  @DisplayName("Invalid DB URI falls back to Spring URL and persists it")
  void invalidDbUriUsesSpringAndPersists() {
    ConfigurationParameter row = parameter("not a uri");
    when(configurationParameterRepository.findByName(SitmunConstants.PROXY_CONF_KEY))
        .thenReturn(Optional.of(row));
    when(configurationParameterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    assertThat(resolver.resolve()).isEqualTo(SPRING_FALLBACK);
    verify(configurationParameterRepository).save(any());
  }

  @Test
  @DisplayName("DB URI without scheme or host falls back to Spring URL and persists it")
  void dbUriWithoutHostUsesSpringAndPersists() {
    ConfigurationParameter row = parameter("/middleware/only");
    when(configurationParameterRepository.findByName(SitmunConstants.PROXY_CONF_KEY))
        .thenReturn(Optional.of(row));
    when(configurationParameterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    assertThat(resolver.resolve()).isEqualTo(SPRING_FALLBACK);
    verify(configurationParameterRepository).save(any());
  }

  @Test
  @DisplayName("Non-default port is preserved")
  void nonDefaultPortPreserved() {
    when(configurationParameterRepository.findByName(SitmunConstants.PROXY_CONF_KEY))
        .thenReturn(Optional.empty());

    ProxyMiddlewareUrlResolver custom =
        new ProxyMiddlewareUrlResolver(
            configurationParameterRepository,
            "http://localhost:9000/middleware",
            transactionManager);

    assertThat(custom.resolve()).isEqualTo("http://localhost:9000/middleware");
  }

  @Test
  @DisplayName("sanitizeStoredProxyValue replaces blank with Spring default")
  void sanitizeBlankUsesSpring() {
    assertThat(resolver.sanitizeStoredProxyValue("  ")).isEqualTo(SPRING_FALLBACK);
  }

  @Test
  @DisplayName("sanitizeStoredProxyValue replaces invalid URI with Spring default")
  void sanitizeInvalidUsesSpring() {
    assertThat(resolver.sanitizeStoredProxyValue("not a uri")).isEqualTo(SPRING_FALLBACK);
  }

  @Test
  @DisplayName("sanitizeStoredProxyValue normalizes a valid URI")
  void sanitizeValidNormalizes() {
    assertThat(resolver.sanitizeStoredProxyValue("https://cdn.example.com:443/middleware/"))
        .isEqualTo("https://cdn.example.com/middleware");
  }

  @Test
  @DisplayName("sanitizeStoredProxy reports defaulted info when blank")
  void sanitizeBlankReportsDefaulted() {
    assertThat(resolver.sanitizeStoredProxy("  ").infos())
        .containsExactly("entity.configurationParameter.warning.proxy-defaulted");
  }

  @Test
  @DisplayName("sanitizeStoredProxy reports normalized info when URI is cleaned")
  void sanitizeUnnormalizedReportsNormalized() {
    assertThat(resolver.sanitizeStoredProxy("https://cdn.example.com:443/middleware/").infos())
        .containsExactly("entity.configurationParameter.warning.proxy-normalized");
  }

  @Test
  @DisplayName("sanitizeStoredProxy reports no infos when already canonical")
  void sanitizeCanonicalReportsNoInfos() {
    assertThat(resolver.sanitizeStoredProxy("https://cdn.example.com/middleware").infos())
        .isEmpty();
  }

  private static ConfigurationParameter parameter(String value) {
    return ConfigurationParameter.builder()
        .id(6)
        .name(SitmunConstants.PROXY_CONF_KEY)
        .value(value)
        .build();
  }
}
