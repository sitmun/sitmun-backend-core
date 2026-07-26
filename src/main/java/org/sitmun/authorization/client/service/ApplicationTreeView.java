package org.sitmun.authorization.client.service;

import lombok.Builder;
import lombok.Getter;
import org.sitmun.domain.tree.Tree;

@Getter
@Builder
public class ApplicationTreeView {
  private final Tree tree;
  private final Integer order;

  public static ApplicationTreeView of(Tree tree, Integer order) {
    return ApplicationTreeView.builder().tree(tree).order(order).build();
  }
}
