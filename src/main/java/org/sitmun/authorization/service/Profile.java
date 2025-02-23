package org.sitmun.authorization.service;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.background.Background;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.tree.Tree;
import org.sitmun.domain.tree.node.TreeNode;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class Profile {
  private Application application;
  private Territory territory;
  private List<Background> backgrounds;
  private List<CartographyPermission> groups;
  private List<Service> services;
  private List<Cartography> layers;
  private List<Task> tasks;
  private List<Tree> trees;
  private Map<Tree, List<TreeNode>> treeNodes;
  private List<ConfigurationParameter> global;
  private ProfileContext context;
}
