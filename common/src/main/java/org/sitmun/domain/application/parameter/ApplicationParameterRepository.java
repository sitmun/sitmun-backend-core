package org.sitmun.domain.application.parameter;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;

@Tag(name = "application parameter")
@RepositoryRestResource(collectionResourceRel = "application-parameters", path = "application-parameters")
public interface ApplicationParameterRepository
  extends PagingAndSortingRepository<ApplicationParameter, Integer>,
  QuerydslPredicateExecutor<ApplicationParameter>,
  QuerydslBinderCustomizer<QApplicationParameter> {
  default void customize(@NonNull QuerydslBindings querydslBindings, @NonNull QApplicationParameter root) {
  }
}