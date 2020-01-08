package org.sitmun.plugin.core.web;

import static org.assertj.core.api.Assertions.assertThat;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.hateoas.UriTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CoreJSControllerIntegrationTests {

  private static final String REQUIREJS_CONFIG_KEY = "requirejs.config";
  private static final String CONFIG_ENDPOINT = "http://localhost:{port}/app/config";

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  public void requirejsIsPresentInConfig() {
    UriTemplate uri = new UriTemplate(CONFIG_ENDPOINT);
    String configRepresentation = restTemplate.getForObject(uri.expand(port), String.class);
    assertThat(configRepresentation).contains(REQUIREJS_CONFIG_KEY);
  }
}
