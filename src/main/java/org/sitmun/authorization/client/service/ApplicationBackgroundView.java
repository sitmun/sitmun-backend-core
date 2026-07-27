package org.sitmun.authorization.client.service;

import static org.sitmun.domain.DomainConstants.Tasks.PROFILE_GROUP_ID_PREFIX;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import org.sitmun.domain.background.Background;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.permission.CartographyPermission;

/** Application background resolved for the client profile, before DTO mapping. */
@Getter
@Builder
public class ApplicationBackgroundView {
  private final String id;
  private final String title;
  private final String thumbnail;
  private final Integer order;
  private final Integer groupId;
  @Builder.Default private final Set<Integer> layerIds = Set.of();

  public static ApplicationBackgroundView of(Background background, Integer order) {
    CartographyPermission group = background.getCartographyGroup();
    Integer groupId = group != null ? group.getId() : null;
    Set<Integer> layerIds =
        group != null && group.getMembers() != null
            ? group.getMembers().stream()
                .map(Cartography::getId)
                .collect(Collectors.toUnmodifiableSet())
            : Collections.emptySet();
    return ApplicationBackgroundView.builder()
        .id(groupId != null ? PROFILE_GROUP_ID_PREFIX + groupId : null)
        .title(background.getName())
        .thumbnail(background.getImage())
        .order(order)
        .groupId(groupId)
        .layerIds(layerIds)
        .build();
  }
}
