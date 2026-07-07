package org.sitmun.domain.background;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "background")
@RepositoryRestResource(collectionResourceRel = "backgrounds", path = "backgrounds")
public interface BackgroundRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Background, Integer> {

  @RestResource(exported = false)
  @Query(
      """
      select new org.sitmun.domain.background.OrderedBackground(ab.order, back)
      from Application app, ApplicationBackground ab, Background back
      where app.id = ?1
      and ab.application = app
      and ab.background = back
      and back.active = true
      order by ab.order
      """)
  List<OrderedBackground> findActiveByApplication(Integer appId);

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select background
      from Background background
      where lower(background.name) like lower(concat('%', :q, '%'))
      or lower(background.description) like lower(concat('%', :q, '%'))
      """)
  Page<Background> findByContent(@Param("q") String q, Pageable pageable);
}
