package org.sitmun.domain.cartography.style;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.tree.node.TreeNode;
import org.sitmun.domain.tree.node.TreeNodeRepository;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartographyStyleEventHandler")
class CartographyStyleEventHandlerTest {

  @Mock private CartographyStyleRepository cartographyStyleRepository;
  @Mock private TreeNodeRepository treeNodeRepository;
  @Mock private EntityManager entityManager;
  @InjectMocks private CartographyStyleEventHandler handler;
  @Captor private ArgumentCaptor<Iterable<TreeNode>> savedNodes;

  @Test
  @DisplayName("save with a null cartography does not throw or rewrite nodes")
  void saveWithNullCartographyDoesNotThrow() {
    CartographyStyle stored = style(10, "old", null);
    CartographyStyle incoming = style(10, "renamed", null);
    when(cartographyStyleRepository.findById(10)).thenReturn(Optional.of(stored));

    assertThatCode(() -> handler.handleChanges(incoming)).doesNotThrowAnyException();

    verify(treeNodeRepository, never()).saveAll(any());
    verify(entityManager).merge(incoming);
    verify(cartographyStyleRepository, never()).existsByCartographyIdAndName(anyInt(), anyString());
  }

  @Test
  @DisplayName("delete with a null cartography does not throw")
  void deleteWithNullCartographyDoesNotThrow() {
    assertThatCode(() -> handler.handleCartographyStyleDelete(style(10, "old", null)))
        .doesNotThrowAnyException();

    verify(treeNodeRepository, never()).existsByCartographyIdAndStyle(anyInt(), anyString());
  }

  @Test
  @DisplayName("create rejects a name already used on the same cartography")
  void createRejectsDuplicateName() {
    Cartography cartography = cartography(1);
    when(cartographyStyleRepository.existsByCartographyIdAndName(1, "shared")).thenReturn(true);

    assertThatThrownBy(
            () -> handler.handleCartographyStyleCreate(style(null, "shared", cartography)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  @DisplayName("save rejects a name already used by another style on the same cartography")
  void saveRejectsDuplicateName() {
    Cartography cartography = cartography(1);
    when(cartographyStyleRepository.findById(10))
        .thenReturn(Optional.of(style(10, "old", cartography)));
    when(cartographyStyleRepository.existsByCartographyIdAndName(1, "shared")).thenReturn(true);

    assertThatThrownBy(() -> handler.handleChanges(style(10, "shared", cartography)))
        .isInstanceOf(BusinessRuleException.class);

    verify(treeNodeRepository, never()).saveAll(any());
    verify(entityManager, never()).merge(any());
  }

  @Test
  @DisplayName("save keeps the current name when it is already stored on that cartography")
  void saveAcceptsUnchangedName() {
    Cartography cartography = cartography(1);
    TreeNode node = TreeNode.builder().style("shared").build();
    cartography.setTreeNodes(Set.of(node));
    when(cartographyStyleRepository.findById(10))
        .thenReturn(Optional.of(style(10, "shared", cartography)));
    when(cartographyStyleRepository.existsByCartographyIdAndName(1, "shared")).thenReturn(true);

    assertThatCode(() -> handler.handleChanges(style(10, "shared", cartography)))
        .doesNotThrowAnyException();

    verify(entityManager).merge(any(CartographyStyle.class));
  }

  @Test
  @DisplayName("rename updates only nodes that stored the old style name")
  void renameUpdatesOnlyMatchingNodes() {
    TreeNode matching = TreeNode.builder().id(1).style("old").build();
    TreeNode other = TreeNode.builder().id(2).style("other").build();
    Cartography cartography = cartography(1);
    cartography.setTreeNodes(Set.of(matching, other));
    when(cartographyStyleRepository.findById(10))
        .thenReturn(Optional.of(style(10, "old", cartography)));

    handler.handleChanges(style(10, "renamed", cartography));

    assertThat(matching.getStyle()).isEqualTo("renamed");
    assertThat(other.getStyle()).isEqualTo("other");
    verify(treeNodeRepository).saveAll(savedNodes.capture());
    assertThat(savedNodes.getValue()).containsExactly(matching);
  }

  private static Cartography cartography(int id) {
    return Cartography.builder().id(id).build();
  }

  private static CartographyStyle style(Integer id, String name, Cartography cartography) {
    return CartographyStyle.builder()
        .id(id)
        .name(name)
        .defaultStyle(false)
        .cartography(cartography)
        .build();
  }
}
