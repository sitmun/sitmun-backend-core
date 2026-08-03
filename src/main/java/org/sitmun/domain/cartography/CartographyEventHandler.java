package org.sitmun.domain.cartography;

import jakarta.validation.constraints.NotNull;
import org.sitmun.domain.task.TaskRepository;
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
  private final TaskRepository taskRepository;

  public CartographyEventHandler(
      TreeNodeRepository treeNodeRepository, TaskRepository taskRepository) {
    this.treeNodeRepository = treeNodeRepository;
    this.taskRepository = taskRepository;
  }

  @HandleBeforeDelete
  @Transactional(rollbackFor = BusinessRuleException.class)
  public void handleCartographyDelete(@NotNull Cartography cartography) {
    Integer id = cartography.getId();

    if (treeNodeRepository.existsByCartographyId(id)) {
      throw new BusinessRuleException(
          ProblemTypes.DATA_INTEGRITY_VIOLATION,
          "Cartography is in use by tree nodes and cannot be deleted",
          "entity.tree-node.plural");
    }

    if (taskRepository.existsByCartographyId(id)) {
      throw new BusinessRuleException(
          ProblemTypes.DATA_INTEGRITY_VIOLATION,
          "Cartography is in use by tasks and cannot be deleted",
          "entity.task.plural");
    }
  }
}
