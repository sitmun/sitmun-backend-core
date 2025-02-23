package org.sitmun.authorization.service;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class ProfileContext {
  private String username;
  private Integer appId;
  private Integer territoryId;
  private NodeSectionBehaviour nodeSectionBehaviour;
  private Integer nodeId;

  public enum NodeSectionBehaviour {
    VIRTUAL_ROOT_ALL_NODES,
    VIRTUAL_ROOT_NODE_PAGE,
    ANY_NODE_PAGE;
    public boolean nodePageMode() {
      return this == VIRTUAL_ROOT_NODE_PAGE || this == ANY_NODE_PAGE;
    }
  }
}
