package org.sitmun.domain.cartography;

import jakarta.validation.constraints.NotNull;
import org.sitmun.domain.tree.node.TreeNodeRepository;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RepositoryEventHandler
public class CartographyEventHandler {

  private final TreeNodeRepository treeNodeRepository;

  public CartographyEventHandler(TreeNodeRepository treeNodeRepository) {
    this.treeNodeRepository = treeNodeRepository;
  }

  @HandleBeforeDelete
  @Transactional(rollbackFor = BusinessRuleException.class)
  public void handleCartographyDelete(@NotNull Cartography cartography) {
    // Check if any tree nodes reference this cartography
    boolean hasTreeNodes = treeNodeRepository.existsByCartographyId(cartography.getId());
    
    if (hasTreeNodes) {
      throw new BusinessRuleException(
          ProblemTypes.DATA_INTEGRITY_VIOLATION,
          "Cartography is in use by tree nodes and cannot be deleted");
    }
  }
}

