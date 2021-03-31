package org.sitmun.plugin.core.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.service.dto.UserDTO;
import org.sitmun.plugin.core.test.ClientHttpLoggerRequestInterceptor;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sitmun.plugin.core.security.SecurityConstants.HEADER_STRING;
import static org.sitmun.plugin.core.test.TestUtils.requestAuthorization;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
public class UserResourceIntegrationTest {

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
    HttpHeaders headers = new HttpHeaders();
    headers.set(HEADER_STRING, requestAuthorization(restTemplate, port));

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

  @Test
  public void cannotPostWithoutLogin() {
    UserDTO newUser = new UserDTO(organizacionAdmin);
    newUser.setId(null);
    newUser.setUsername(NEW_USER_USERNAME);

    Assertions.assertThrows(HttpClientErrorException.Unauthorized.class, () -> restTemplate
      .postForEntity("http://localhost:{port}/api/users", newUser, UserDTO.class, port));
  }

  @Test
  public void getAllUsers() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HEADER_STRING, requestAuthorization(restTemplate, port));

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
