package org.sitmun.domain.cartography.style;

import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.tree.node.TreeNode;
import org.sitmun.domain.tree.node.TreeNodeRepository;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RepositoryEventHandler
public class CartographyStyleEventHandler {

  private final CartographyStyleRepository cartographyStyleRepository;
  private final TreeNodeRepository treeNodeRepository;
  private final EntityManager entityManager;

  public CartographyStyleEventHandler(
      CartographyStyleRepository cartographyStyleRepository,
      TreeNodeRepository treeNodeRepository,
      EntityManager entityManager) {
    this.cartographyStyleRepository = cartographyStyleRepository;
    this.treeNodeRepository = treeNodeRepository;
    this.entityManager = entityManager;
  }

  @HandleBeforeCreate
  @Transactional(rollbackFor = BusinessRuleException.class)
  public void handleCartographyStyleCreate(@NotNull CartographyStyle cartographyStyle) {
    rejectDuplicateName(cartographyStyle, null);
  }

  @HandleBeforeDelete
  @Transactional(rollbackFor = BusinessRuleException.class)
  public void handleCartographyStyleDelete(@NotNull CartographyStyle cartographyStyle) {
    Cartography cartography = cartographyStyle.getCartography();
    if (cartography == null || cartography.getId() == null) {
      return;
    }
    String name = cartographyStyle.getName();
    Integer cartographyId = cartography.getId();

    boolean inUse = treeNodeRepository.existsByCartographyIdAndStyle(cartographyId, name);

    if (inUse) {
      throw new BusinessRuleException(
          ProblemTypes.DATA_INTEGRITY_VIOLATION,
          "Cartography Style in use in a the tree node",
          "entity.tree-node.plural");
    }
  }

  /**
   * If the name of a cartography style is renamed, all the TreeNode that reference such style must
   * update its style property to the new name.
   *
   * <p>See <a
   * href="https://github.com/spring-projects/spring-data-rest/issues/753?focusedCommentId=127082&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#issuecomment-752908382">...</a>
   *
   * @param cartographyStyle the new or changed cartography style.
   */
  @HandleBeforeSave
  @Transactional(rollbackFor = BusinessRuleException.class)
  public void handleChanges(@NotNull CartographyStyle cartographyStyle) {
    entityManager.detach(cartographyStyle);
    Optional<CartographyStyle> oldStyle =
        cartographyStyleRepository.findById(cartographyStyle.getId());
    rejectDuplicateName(cartographyStyle, oldStyle.orElse(null));

    if (oldStyle.isPresent()) {
      Cartography cartography = oldStyle.get().getCartography();
      if (cartography != null) {
        String oldName = oldStyle.get().getName();
        List<TreeNode> updatedNodes =
            cartography.getTreeNodes().stream()
                .filter(it -> Objects.equals(it.getStyle(), oldName))
                .collect(Collectors.toList());
        updatedNodes.forEach(it -> it.setStyle(cartographyStyle.getName()));
        treeNodeRepository.saveAll(updatedNodes);
      }
    }

    entityManager.merge(cartographyStyle);
  }

  private void rejectDuplicateName(CartographyStyle style, CartographyStyle previous) {
    Cartography cartography = style.getCartography();
    if (cartography == null || cartography.getId() == null) {
      return;
    }
    if (!cartographyStyleRepository.existsByCartographyIdAndName(
        cartography.getId(), style.getName())) {
      return;
    }
    if (sameCartographyAndName(previous, style)) {
      return;
    }
    throw new BusinessRuleException(
        ProblemTypes.DATA_INTEGRITY_VIOLATION,
        "A cartography style name must be unique for the cartography");
  }

  private static boolean sameCartographyAndName(CartographyStyle previous, CartographyStyle style) {
    if (previous == null || !Objects.equals(previous.getName(), style.getName())) {
      return false;
    }
    Cartography previousCartography = previous.getCartography();
    return previousCartography != null
        && Objects.equals(previousCartography.getId(), style.getCartography().getId());
  }
}
