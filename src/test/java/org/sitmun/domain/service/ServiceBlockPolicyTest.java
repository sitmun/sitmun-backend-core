package org.sitmun.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ServiceBlockPolicyTest {

  @Test
  @DisplayName("null service is not accessible")
  void nullServiceNotAccessible() {
    assertThat(ServiceBlockPolicy.isAccessibleInClientProfile(null)).isFalse();
  }

  @Test
  @DisplayName("service with blocked=false is accessible")
  void notBlockedServiceAccessible() {
    Service service = Service.builder().id(1).blocked(false).build();
    assertThat(ServiceBlockPolicy.isAccessibleInClientProfile(service)).isTrue();
  }

  @Test
  @DisplayName("service with blocked=true is not accessible")
  void blockedServiceNotAccessible() {
    Service service = Service.builder().id(2).blocked(true).build();
    assertThat(ServiceBlockPolicy.isAccessibleInClientProfile(service)).isFalse();
  }

  @Test
  @DisplayName("service with blocked=null is accessible")
  void nullBlockedServiceAccessible() {
    Service service = Service.builder().id(3).blocked(null).build();
    assertThat(ServiceBlockPolicy.isAccessibleInClientProfile(service)).isTrue();
  }

  @Test
  @DisplayName("null service is accessible when null is allowed")
  void nullServiceAccessibleWhenAllowed() {
    assertThat(ServiceBlockPolicy.isAccessibleInClientProfileOrNull(null)).isTrue();
  }

  @Test
  @DisplayName("blocked service is not accessible even when null is allowed")
  void blockedServiceNotAccessibleWhenNullAllowed() {
    Service service = Service.builder().id(2).blocked(true).build();
    assertThat(ServiceBlockPolicy.isAccessibleInClientProfileOrNull(service)).isFalse();
  }

  @Test
  @DisplayName("accessible service is accessible when null is allowed")
  void accessibleServiceWhenNullAllowed() {
    Service service = Service.builder().id(1).blocked(false).build();
    assertThat(ServiceBlockPolicy.isAccessibleInClientProfileOrNull(service)).isTrue();
  }
}
