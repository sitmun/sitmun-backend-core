package org.sitmun.domain.cartography;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartographyBlockPolicyTest {

  @Test
  @DisplayName("null cartography is directly blocked")
  void nullCartographyBlocked() {
    assertThat(CartographyBlockPolicy.isNotDirectlyBlocked(null)).isFalse();
  }

  @Test
  @DisplayName("cartography with blocked=false is not directly blocked")
  void notBlockedCartography() {
    Cartography cartography = Cartography.builder().id(1).blocked(false).build();
    assertThat(CartographyBlockPolicy.isNotDirectlyBlocked(cartography)).isTrue();
  }

  @Test
  @DisplayName("cartography with blocked=true is directly blocked")
  void blockedCartography() {
    Cartography cartography = Cartography.builder().id(2).blocked(true).build();
    assertThat(CartographyBlockPolicy.isNotDirectlyBlocked(cartography)).isFalse();
  }

  @Test
  @DisplayName("cartography with blocked=null is not directly blocked")
  void nullBlockedCartography() {
    Cartography cartography = Cartography.builder().id(3).blocked(null).build();
    assertThat(CartographyBlockPolicy.isNotDirectlyBlocked(cartography)).isTrue();
  }
}
