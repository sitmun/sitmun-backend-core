package org.sitmun.authorization.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.sitmun.domain.user.User;

@DisplayName("ApplicationMapper")
class ApplicationMapperTest {

  private ApplicationMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(ApplicationMapper.class);
  }

  @Test
  @DisplayName("map(User): returns user email when email is set")
  void mapUserWithEmailReturnsEmail() {
    User user = User.builder().username("admin").email("admin@example.com").build();
    assertThat(mapper.map(user)).isEqualTo("admin@example.com");
  }

  @Test
  @DisplayName("map(User): returns null when email is null")
  void mapUserWithNullEmailReturnsNull() {
    User user = User.builder().username("admin").email(null).build();
    assertThat(mapper.map(user)).isNull();
  }

  @Test
  @DisplayName("map(User): returns null when user is null")
  void mapNullUserReturnsNull() {
    assertThat(mapper.map((User) null)).isNull();
  }
}
