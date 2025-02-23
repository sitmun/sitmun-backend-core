package org.sitmun.domain.background;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

@Tag(name = "background")
@RepositoryRestResource(collectionResourceRel = "backgrounds", path = "backgrounds")
public interface BackgroundRepository extends PagingAndSortingRepository<Background, Integer> {

  @RestResource(exported = false)
  @Query("select ab.order, back from Application app, ApplicationBackground ab, Background back where" +
    " app.id = ?1 and ab.application = app and ab.background = back and back.active = true " +
    " order by ab.order ")
  List<Object[]> findActiveByApplication(Integer appId);
}