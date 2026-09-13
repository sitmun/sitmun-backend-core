package org.sitmun.infrastructure.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.territory.Territory;

class SystemVariableSupportTest {

  @Test
  void munInes_nullTerritory_isEmpty() {
    assertThat(SystemVariableSupport.munInes(mock(Application.class), null)).isEmpty();
  }

  @Test
  void munInes_withoutChildrenFlag_isSelfOnly() {
    Territory territory = mock(Territory.class);
    when(territory.getCode()).thenReturn("08019");
    Application application = mock(Application.class);
    when(application.getAccessChildrenTerritory()).thenReturn(false);

    assertThat(SystemVariableSupport.munInes(application, territory)).isEqualTo("08019");
  }

  @Test
  void munInes_withChildrenFlag_selfThenSortedMembers() {
    Territory childA = mock(Territory.class);
    when(childA.getCode()).thenReturn("08298");
    Territory childB = mock(Territory.class);
    when(childB.getCode()).thenReturn("08021");
    Territory territory = mock(Territory.class);
    when(territory.getCode()).thenReturn("08019");
    when(territory.getMembers()).thenReturn(Set.of(childA, childB));
    Application application = mock(Application.class);
    when(application.getAccessChildrenTerritory()).thenReturn(true);

    assertThat(SystemVariableSupport.munInes(application, territory))
        .isEqualTo("08019,08021,08298");
  }
}
