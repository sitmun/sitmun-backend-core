package org.sitmun.plugin.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sitmun.plugin.core.security.SecurityConstants.HEADER_STRING;
import static org.sitmun.plugin.core.security.SecurityConstants.TOKEN_PREFIX;


import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.service.dto.UserDTO;
import org.sitmun.plugin.core.test.ClientHttpLoggerRequestInterceptor;
import org.sitmun.plugin.core.web.rest.AuthController.JWTToken;
import org.sitmun.plugin.core.web.rest.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.config.HypermediaRestTemplateConfigurer;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserResourceIntegrationTest {

  private static final String TERRITORY1_ADMIN_USERNAME = "territory1-admin";
  private static final String NEW_USER_USERNAME = "admin_new";
  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_PASSWORD = "admin";
  private static final String USER_PASSWORD = "admin";
  private static final String USER_FIRSTNAME = "Admin";
  private static final String USER_LASTNAME = "Admin";
  private static final Boolean USER_BLOCKED = false;
  private static final Boolean USER_ADMINISTRATOR = true;
  @LocalServerPort
  private int port;

  @Autowired
  HypermediaRestTemplateConfigurer configurer;

  private RestTemplate restTemplate;
  private User organizacionAdmin;

  @Before
  public void init() {
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
  public void createNewUserAndDelete() {
    LoginRequest login = new LoginRequest();
    login.setUsername(ADMIN_USERNAME);
    login.setPassword(ADMIN_PASSWORD);
    ResponseEntity<JWTToken> loginResponse =
        restTemplate
            .postForEntity("http://localhost:{port}/api/authenticate", login, JWTToken.class, port);
    assertThat(loginResponse.getBody()).isNotNull();
    HttpHeaders headers = new HttpHeaders();
    headers.set(HEADER_STRING, TOKEN_PREFIX + loginResponse.getBody().getIdToken());

    UserDTO newUser = new UserDTO(organizacionAdmin);
    newUser.setId(null);
    newUser.setUsername(NEW_USER_USERNAME);
    HttpEntity<UserDTO> entity = new HttpEntity<>(newUser, headers);

    ResponseEntity<UserDTO> createdUser =
        restTemplate
            .postForEntity("http://localhost:{port}/api/users", entity, UserDTO.class, port);

    assertThat(createdUser.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(createdUser.getHeaders().getLocation()).isNotNull();

    ResponseEntity<UserDTO> exists = restTemplate
        .exchange(createdUser.getHeaders().getLocation(), HttpMethod.GET, new HttpEntity<>(headers),
            UserDTO.class);
    assertThat(exists.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<String> deleted = restTemplate
        .exchange(createdUser.getHeaders().getLocation(), HttpMethod.DELETE,
            new HttpEntity<>(headers), String.class);
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    try {
      restTemplate.exchange(createdUser.getHeaders().getLocation(), HttpMethod.GET,
          new HttpEntity<>(headers), UserDTO.class);
      fail("404 is expected");
    } catch (HttpClientErrorException e) {
      assertThat(e.getRawStatusCode()).isEqualTo(404);
    }
  }

  @Test(expected = HttpClientErrorException.Unauthorized.class)
  public void cannotPostWithoutLogin() {
    UserDTO newUser = new UserDTO(organizacionAdmin);
    newUser.setId(null);
    newUser.setUsername(NEW_USER_USERNAME);

    restTemplate
        .postForEntity("http://localhost:{port}/api/users", newUser, UserDTO.class, port);
  }

  @Test
  public void getAllUsers() {
    LoginRequest login = new LoginRequest();
    login.setUsername(ADMIN_USERNAME);
    login.setPassword(ADMIN_PASSWORD);
    ResponseEntity<JWTToken> loginResponse =
        restTemplate
            .postForEntity("http://localhost:{port}/api/authenticate", login, JWTToken.class, port);
    assertThat(loginResponse.getBody()).isNotNull();

    HttpHeaders headers = new HttpHeaders();
    headers.set(HEADER_STRING, TOKEN_PREFIX + loginResponse.getBody().getIdToken());

    ResponseEntity<CollectionModel<User>> response = restTemplate
        .exchange("http://localhost:{port}/api/users", HttpMethod.GET,
            new HttpEntity<CollectionModel<User>>(headers),
            new TypeReferences.CollectionModelType<User>() {
            }, port);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent().size()).isEqualTo(1332);
  }

}