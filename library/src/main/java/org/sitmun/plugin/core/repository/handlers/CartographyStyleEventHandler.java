package org.sitmun.plugin.core.repository.handlers;

import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.domain.CartographyStyle;
import org.sitmun.plugin.core.domain.TreeNode;
import org.sitmun.plugin.core.repository.CartographyStyleRepository;
import org.sitmun.plugin.core.repository.TreeNodeRepository;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RepositoryEventHandler
public class CartographyStyleEventHandler {

  private final CartographyStyleRepository cartographyStyleRepository;
  private final TreeNodeRepository treeNodeRepository;
  @PersistenceContext
  private EntityManager entityManager;

  public CartographyStyleEventHandler(CartographyStyleRepository cartographyStyleRepository, TreeNodeRepository treeNodeRepository) {
    this.cartographyStyleRepository = cartographyStyleRepository;
    this.treeNodeRepository = treeNodeRepository;
  }

  @HandleBeforeDelete
  @Transactional(rollbackFor = RequirementException.class)
  public void handleCartographyStyleDelete(@NotNull CartographyStyle cartographyStyle) {
    String name = cartographyStyle.getName();
    Cartography cartography = cartographyStyle.getCartography();
    boolean inUse = cartography.getTreeNodes().stream().anyMatch(it -> Objects.equals(name, it.getName()));
    if (inUse) {
      throw new RequirementException("Cartography Style in use in a the tree node");
    }
  }

  /**
   * If the name of a cartography style is renamed,
   * all the TreeNode that reference such style
   * must update its style property to the new name.
   * <p>
   * See https://github.com/spring-projects/spring-data-rest/issues/753?focusedCommentId=127082&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#issuecomment-752908382
   *
   * @param cartographyStyle the new or changed cartography style.
   */
  @HandleBeforeSave
  public void handleChanges(@NotNull CartographyStyle cartographyStyle) {
    entityManager.detach(cartographyStyle);
    Optional<CartographyStyle> oldStyle = cartographyStyleRepository.findById(cartographyStyle.getId());

    if (oldStyle.isPresent()) {
      String oldName = oldStyle.get().getName();
      List<TreeNode> updatedNodes = oldStyle.get().getCartography().getTreeNodes().stream()
        .filter(it -> Objects.equals(it.getStyle(), oldName))
        .peek(it -> it.setStyle(cartographyStyle.getName()))
        .collect(Collectors.toList());
      treeNodeRepository.saveAll(updatedNodes);
    }

    entityManager.merge(cartographyStyle);
  }
}