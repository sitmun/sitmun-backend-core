package org.sitmun.domain.tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.tree.node.TreeNodeRepository;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;
import org.sitmun.infrastructure.web.dto.ProblemTypes;

@DisplayName("TreeRadioTypePolicy")
class TreeRadioTypePolicyTest {

  @Test
  @DisplayName("rejects cartography to edition when radio folders exist")
  void rejectsCartographyToEditionWithRadioFolders() {
    TreeNodeRepository repository = mock(TreeNodeRepository.class);
    when(repository.existsRadioFolderInTree(1)).thenReturn(true);

    assertThatThrownBy(
            () ->
                TreeRadioTypePolicy.validateRadioFoldersBeforeLeavingCartography(
                    "cartography", "edition", 1, repository))
        .isInstanceOfSatisfying(
            BusinessRuleException.class,
            ex ->
                assertThat(ex.getProblemType()).isEqualTo(ProblemTypes.TREE_TYPE_CHANGE_CONSTRAINT))
        .hasMessage("Cartography tree type cannot be changed while radio folders are configured");
  }

  @Test
  @DisplayName("allows cartography to edition when no radio folders exist")
  void allowsCartographyToEditionWithoutRadioFolders() {
    TreeNodeRepository repository = mock(TreeNodeRepository.class);
    when(repository.existsRadioFolderInTree(2)).thenReturn(false);

    assertThatCode(
            () ->
                TreeRadioTypePolicy.validateRadioFoldersBeforeLeavingCartography(
                    "cartography", "edition", 2, repository))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("skips validation when prior or new type still allows radio")
  void skipsWhenTypeStillAllowsRadio() {
    TreeNodeRepository repository = mock(TreeNodeRepository.class);

    assertThatCode(
            () ->
                TreeRadioTypePolicy.validateRadioFoldersBeforeLeavingCartography(
                    "cartography", "cartography", 1, repository))
        .doesNotThrowAnyException();
  }
}
