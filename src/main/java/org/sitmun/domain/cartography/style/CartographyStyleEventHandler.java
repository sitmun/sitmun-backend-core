package org.sitmun.domain.cartography.style;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.tree.node.TreeNode;
import org.sitmun.domain.tree.node.TreeNodeRepository;
import org.sitmun.infrastructure.persistence.exception.RequirementException;
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
  @PersistenceContext private EntityManager entityManager;

  public CartographyStyleEventHandler(
      CartographyStyleRepository cartographyStyleRepository,
      TreeNodeRepository treeNodeRepository) {
    this.cartographyStyleRepository = cartographyStyleRepository;
    this.treeNodeRepository = treeNodeRepository;
  }

  @HandleBeforeDelete
  @Transactional(rollbackFor = RequirementException.class)
  public void handleCartographyStyleDelete(@NotNull CartographyStyle cartographyStyle) {
    String name = cartographyStyle.getName();
    Cartography cartography = cartographyStyle.getCartography();
    boolean inUse =
        cartography.getTreeNodes().stream().anyMatch(it -> Objects.equals(name, it.getName()));
    if (inUse) {
      throw new RequirementException("Cartography Style in use in a the tree node");
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
  @Transactional
  public void handleChanges(@NotNull CartographyStyle cartographyStyle) {
    entityManager.detach(cartographyStyle);
    Optional<CartographyStyle> oldStyle =
        cartographyStyleRepository.findById(cartographyStyle.getId());

    if (oldStyle.isPresent()) {
      String oldName = oldStyle.get().getName();
      List<TreeNode> updatedNodes =
          oldStyle.get().getCartography().getTreeNodes().stream()
              .filter(it -> Objects.equals(it.getStyle(), oldName))
              .collect(Collectors.toList());
      updatedNodes.forEach(it -> it.setStyle(cartographyStyle.getName()));
      treeNodeRepository.saveAll(updatedNodes);
    }

    entityManager.merge(cartographyStyle);
  }
}
