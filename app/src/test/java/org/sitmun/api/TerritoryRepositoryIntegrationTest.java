package org.sitmun.api;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.test.ClientHttpLoggerRequestInterceptor;
import org.sitmun.test.TestUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

public class TerritoryRepositoryIntegrationTest {

  private RestTemplate restTemplate;
  @LocalServerPort
  private int port;

  @BeforeEach
  public void setup() {
    ClientHttpRequestFactory factory =
      new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
    restTemplate = new RestTemplate(factory);
    List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
    if (CollectionUtils.isEmpty(interceptors)) {
      interceptors = new ArrayList<>();
    }
    interceptors.add(new ClientHttpLoggerRequestInterceptor());
    restTemplate.setInterceptors(interceptors);

  }

  @Test
  public void requestMembers() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, TestUtils.requestAuthorization(restTemplate, port));
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
      restTemplate
        .exchange("http://localhost:" + port + "/api/territories/330/members", HttpMethod.GET, entity, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext context = JsonPath.parse(response.getBody());

    assertThat(context.read("$._embedded.territories[*]", JSONArray.class))
      .hasSize(33);
  }

  @Test
  public void requestMemberOf() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, TestUtils.requestAuthorization(restTemplate, port));
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
      restTemplate
        .exchange("http://localhost:" + port + "/api/territories/74/memberOf", HttpMethod.GET, entity, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext context = JsonPath.parse(response.getBody());

    assertThat(context.read("$._embedded.territories[*]", JSONArray.class))
      .hasSize(1);
  }
}