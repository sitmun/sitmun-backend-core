package org.sitmun.domain.application.territory;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;

@Tag(name = "application territory")
@RepositoryRestResource(
    collectionResourceRel = "application-territory",
    path = "application-territory")
public interface ApplicationTerritoryRepository
    extends JpaRepository<ApplicationTerritory, Integer>,
        QuerydslPredicateExecutor<ApplicationTerritory>,
        QuerydslBinderCustomizer<QApplicationTerritory> {
  default void customize(
      @NonNull QuerydslBindings querydslBindings, @NonNull QApplicationTerritory root) {}
}
