package org.sitmun.domain.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

  private static final String USER_USERNAME = "user";
  private static final String USER_PASSWORD = "user";
  private static final String USER_FIRSTNAME = "Admin";
  private static final String USER_LASTNAME = "Admin";
  private static final Boolean USER_BLOCKED = false;
  private static final Boolean USER_ADMINISTRATOR = false;
  @Autowired
  JsonWebTokenService tokenProvider;
  @Autowired
  private MockMvc mvc;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private UserEventHandler userEventHandler;
  private String validToken;
  private User user;
  private String expiredToken;

  @BeforeEach
  void init() {
    withMockSitmunAdmin(() -> {
      Date expiredDate =
        Date.from(LocalDate.parse("1900-01-01").atStartOfDay(ZoneId.systemDefault()).toInstant());
      expiredToken = tokenProvider.generateToken(USER_USERNAME, expiredDate);
      validToken = tokenProvider.generateToken(USER_USERNAME, new Date());

      user = User.builder()
        .administrator(USER_ADMINISTRATOR)
        .blocked(USER_BLOCKED)
        .firstName(USER_FIRSTNAME)
        .lastName(USER_LASTNAME)
        .password(USER_PASSWORD)
        .username(USER_USERNAME)
        .build();
      userEventHandler.handleUserCreate(user);
      user = userRepository.save(user);
    });
  }

  @AfterEach
  void cleanup() {
    withMockSitmunAdmin(() -> userRepository.delete(user));
  }

  @Test
  void readAccount() throws Exception {
    mvc.perform(get(URIConstants.ACCOUNT_URI)
        .header(HttpHeaders.AUTHORIZATION, "Bearer "+validToken)
      ).andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.firstName", equalTo(USER_FIRSTNAME)))
      .andExpect(jsonPath("$.lastName", equalTo(USER_LASTNAME)));
  }

  @Test
  void readAccountWithoutToken() throws Exception {
    mvc.perform(get(URIConstants.ACCOUNT_URI))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void readAccountWithExpiredToken() throws Exception {
    mvc.perform(get(URIConstants.ACCOUNT_URI)
      .header(HttpHeaders.AUTHORIZATION, "Bearer "+expiredToken)
    ).andExpect(status().isUnauthorized());
  }

  @Test
  void updateAccountButKeepThePassword() throws Exception {
    String content = "{" +
      "\"username\":\"user\"," +
      "\"firstName\":\"NameChanged\"," +
      "\"lastName\":\"NameChanged\"," +
      "\"administrator\": false," +
      "\"blocked\": false}";

    mvc.perform(put(URIConstants.ACCOUNT_URI)
      .header(HttpHeaders.AUTHORIZATION, "Bearer "+validToken)
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
    ).andExpect(status().isOk());

    mvc.perform(get(URIConstants.ACCOUNT_URI)
        .header(HttpHeaders.AUTHORIZATION, "Bearer "+validToken)
      ).andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.firstName", equalTo("NameChanged")))
      .andExpect(jsonPath("$.lastName", equalTo("NameChanged")))
      .andExpect(jsonPath("$.passwordSet", equalTo(true)))
      .andExpect(jsonPath("$.password").doesNotExist());

    String oldPassword = user.getPassword();
    assertNotNull(oldPassword);
    withMockSitmunAdmin(() -> {
      Optional<User> updatedUser = userRepository.findById(user.getId());
      assertTrue(updatedUser.isPresent());
      assertEquals(oldPassword, updatedUser.get().getPassword());
    });
  }

  @Test
  void updateAccountButClearThePassword() throws Exception {
    String content = "{" +
      "\"username\":\"user\"," +
      "\"firstName\":\"NameChanged\"," +
      "\"lastName\":\"NameChanged\"," +
      "\"password\":\"\"," +
      "\"administrator\": false," +
      "\"blocked\": false}";

    mvc.perform(put(URIConstants.ACCOUNT_URI)
      .header(HttpHeaders.AUTHORIZATION, "Bearer "+validToken)
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
    ).andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());

    assertNotNull(user.getPassword());
    withMockSitmunAdmin(() -> {
      Optional<User> updatedUser = userRepository.findById(user.getId());
      assertTrue(updatedUser.isPresent());
      assertNull(updatedUser.get().getPassword());
    });
  }

}
