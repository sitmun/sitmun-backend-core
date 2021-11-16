package org.sitmun.plugin.core.repository.handlers;

import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.domain.CartographyStyle;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@Component
@RepositoryEventHandler
public class CartographyStyleEventHandler {

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
}