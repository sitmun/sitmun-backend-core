package org.sitmun.authorization.service;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;

import java.util.List;

@Getter @Setter @Builder
public class Profile {
  private Application application;
  private Territory territory;
  private List<CartographyPermission> groups;
  private List<Cartography> layers;
  private List<Task> tasks;
}
