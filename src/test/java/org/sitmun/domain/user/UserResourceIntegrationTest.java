package org.sitmun.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.ClientHttpLoggerRequestInterceptor;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("User Resource Integration Test")
class UserResourceIntegrationTest {

  private static final String TERRITORY1_ADMIN_USERNAME = "territory1-admin";
  private static final String NEW_USER_USERNAME = "admin_new";
  private static final String USER_PASSWORD = "admin";
  private static final String USER_FIRSTNAME = "Admin";
  private static final String USER_LASTNAME = "Admin";
  private static final Boolean USER_BLOCKED = false;
  private static final Boolean USER_ADMINISTRATOR = true;
  @Autowired HypermediaRestTemplateConfigurer configurer;
  @LocalServerPort private int port;
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
  @DisplayName("POST: Create new user and delete")
  void createNewUserAndDelete() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, TestUtils.requestAuthorization(restTemplate, port));

    User newUser = organizacionAdmin.toBuilder().id(null).username(NEW_USER_USERNAME).build();
    HttpEntity<User> entity = new HttpEntity<>(newUser, headers);

    ResponseEntity<User> createdUser =
        restTemplate.postForEntity("http://localhost:{port}/api/users", entity, User.class, port);

    assertThat(createdUser.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(createdUser.getHeaders().getLocation()).isNotNull();
    final URI location = createdUser.getHeaders().getLocation();
    final HttpEntity<User> entityHeaders = new HttpEntity<>(headers);

    Function<HttpMethod, ResponseEntity<User>> exchangeFunction =
        method -> restTemplate.exchange(location, method, entityHeaders, User.class);

    ResponseEntity<User> exists = exchangeFunction.apply(HttpMethod.GET);
    assertThat(exists.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<User> deleted = exchangeFunction.apply(HttpMethod.DELETE);
    System.out.println(deleted.getBody());
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);

    HttpClientErrorException thrown =
        assertThrows(HttpClientErrorException.class, () -> exchangeFunction.apply(HttpMethod.GET));
    assertThat(thrown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("POST: Create new user without login")
  void cannotPostWithoutLogin() {
    User newUser = organizacionAdmin.toBuilder().id(null).username(NEW_USER_USERNAME).build();

    assertThrows(
        HttpClientErrorException.Unauthorized.class,
        () ->
            restTemplate.postForEntity(
                "http://localhost:{port}/api/users", newUser, User.class, port));
  }

  @Test
  @DisplayName("GET: Get all users")
  void getAllUsers() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, TestUtils.requestAuthorization(restTemplate, port));

    ResponseEntity<CollectionModel<User>> response =
        restTemplate.exchange(
            "http://localhost:{port}/api/users",
            HttpMethod.GET,
            new HttpEntity<CollectionModel<User>>(headers),
            new TypeReferences.CollectionModelType<>() {},
            port);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent()).hasSize(5);
  }
}
