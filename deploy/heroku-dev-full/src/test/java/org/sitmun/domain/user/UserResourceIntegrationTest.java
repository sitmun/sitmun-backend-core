package org.sitmun.domain.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sitmun.test.ClientHttpLoggerRequestInterceptor;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.config.HypermediaRestTemplateConfigurer;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;



@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

class UserResourceIntegrationTest {

  private static final String TERRITORY1_ADMIN_USERNAME = "territory1-admin";
  private static final String NEW_USER_USERNAME = "admin_new";
  private static final String USER_PASSWORD = "admin";
  private static final String USER_FIRSTNAME = "Admin";
  private static final String USER_LASTNAME = "Admin";
  private static final Boolean USER_BLOCKED = false;
  private static final Boolean USER_ADMINISTRATOR = true;
  @Autowired
  HypermediaRestTemplateConfigurer configurer;
  @LocalServerPort
  private int port;
  private RestTemplate restTemplate;
  private User organizacionAdmin;

  @BeforeEach
  void init() {
    ClientHttpRequestFactory factory =
      new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
    restTemplate = new RestTemplate(factory);
    List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
    if (CollectionUtils.isEmpty(interceptors)) {
      interceptors = new ArrayList<>();
    }
    interceptors.add(new ClientHttpLoggerRequestInterceptor());
    restTemplate.setInterceptors(interceptors);
    configurer.registerHypermediaTypes(restTemplate);

    organizacionAdmin = new User();
    organizacionAdmin.setAdministrator(USER_ADMINISTRATOR);
    organizacionAdmin.setBlocked(USER_BLOCKED);
    organizacionAdmin.setFirstName(USER_FIRSTNAME);
    organizacionAdmin.setLastName(USER_LASTNAME);
    organizacionAdmin.setPassword(USER_PASSWORD);
    organizacionAdmin.setUsername(TERRITORY1_ADMIN_USERNAME);
  }


  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void createNewUserAndDelete() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, TestUtils.requestAuthorization(restTemplate, port));

    User newUser = organizacionAdmin.toBuilder().id(null).username(NEW_USER_USERNAME).build();
    HttpEntity<User> entity = new HttpEntity<>(newUser, headers);

    ResponseEntity<User> createdUser =
      restTemplate
        .postForEntity("http://localhost:{port}/api/users", entity, User.class, port);

    assertThat(createdUser.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(createdUser.getHeaders().getLocation()).isNotNull();

    ResponseEntity<User> exists = restTemplate
      .exchange(createdUser.getHeaders().getLocation(), HttpMethod.GET, new HttpEntity<>(headers),
        User.class);
    assertThat(exists.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<String> deleted = restTemplate
      .exchange(createdUser.getHeaders().getLocation(), HttpMethod.DELETE,
        new HttpEntity<>(headers), String.class);
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    URI location = createdUser.getHeaders().getLocation();
    HttpEntity<User> newEntity = new HttpEntity<>(headers);
    try {
      restTemplate.exchange(location, HttpMethod.GET, newEntity, User.class);
      fail("404 is expected");
    } catch (HttpClientErrorException e) {
      assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }

  @Test
  void cannotPostWithoutLogin() {
    User newUser = organizacionAdmin.toBuilder().id(null).username(NEW_USER_USERNAME).build();

    assertThrows(HttpClientErrorException.Unauthorized.class, () -> restTemplate
      .postForEntity("http://localhost:{port}/api/users", newUser, User.class, port));
  }

  @Test
  @Disabled("Requires further investigation")
  void getAllUsers() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, TestUtils.requestAuthorization(restTemplate, port));

    ResponseEntity<CollectionModel<User>> response = restTemplate
      .exchange("http://localhost:{port}/api/users", HttpMethod.GET,
        new HttpEntity<CollectionModel<User>>(headers),
        new TypeReferences.CollectionModelType<>() {
        }, port);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent()).hasSize(1332);
  }

}
