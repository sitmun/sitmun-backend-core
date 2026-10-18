package org.sitmun.domain.cartography;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.cartography.parameter.CartographyServiceParameterRepository;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.tree.node.TreeNodeRepository;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;

@DisplayName("CartographyEventHandler")
class CartographyEventHandlerTest {

  private final CartographyServiceParameterRepository parameters =
      mock(CartographyServiceParameterRepository.class);
  private final CartographyEventHandler handler =
      new CartographyEventHandler(
          mock(TreeNodeRepository.class), mock(TaskRepository.class), parameters);

  @Test
  @DisplayName("Rejects moving a parameterized layer onto a service that is not proxied")
  void rejectsMoveOntoDirectService() {
    when(parameters.existsByCartography_Id(8)).thenReturn(true);
    Service service = Service.builder().blocked(false).isProxied(false).build();
    Cartography cartography =
        Cartography.builder()
            .id(8)
            .name("roads")
            .layers(java.util.List.of("roads"))
            .service(service)
            .queryableFeatureAvailable(false)
            .queryableFeatureEnabled(false)
            .blocked(false)
            .build();

    assertThatThrownBy(() -> handler.handleBeforeSave(cartography))
        .isInstanceOf(BusinessRuleException.class);
  }
}
