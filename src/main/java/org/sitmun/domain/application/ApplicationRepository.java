package org.sitmun.domain.application;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "application")
@RepositoryRestResource(
    collectionResourceRel = "applications",
    path = "applications" /*, excerptProjection = ApplicationProjection.class*/)
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

  /** Applications where the given user is the point of contact (creator). */
  List<Application> findByCreatorId(Integer creatorId);

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select application
      from Application application
      where lower(application.name) like lower(concat('%', :q, '%'))
      or lower(application.type) like lower(concat('%', :q, '%'))
      """)
  Page<Application> findByContent(@Param("q") String q, Pageable pageable);

  @RestResource(exported = false)
  @EntityGraph(attributePaths = {"territories", "territories.territory"})
  @Query(
      """
      select distinct app from Application app, Task task, Role role
      where task.id = ?1 and role member of app.availableRoles and role member of task.roles
      """)
  List<Application> findByTaskId(Integer taskId);

  @RestResource(exported = false)
  @Query(
      "select distinct app.id, uc.territory.id from Application app, UserConfiguration uc where uc.role member of app.availableRoles")
  List<Object[]> listIdApplicationsPerTerritories();

  @RestResource(exported = false)
  @Query(
      """
      select distinct app from Application app
      where app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.appliesToChildrenTerritories = false)
      or app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true)
      or app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory in (select childTerritory from Territory childTerritory where childTerritory member of uc.territory.members) and uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true)
  """)
  Page<Application> findByUser(String username, Pageable pageable);

  @RestResource(exported = false)
  @Query(
      """
      select distinct app from Application app
      where (app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.appliesToChildrenTerritories = false)
      or app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true)
      or app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory in (select childTerritory from Territory childTerritory where childTerritory member of uc.territory.members) and uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true))
      and app.appPrivate = false
  """)
  Page<Application> findByPublicUser(String username, Pageable pageable);

  @RestResource(exported = false)
  @Query(
      """
      select distinct app from Application app
      where app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?2 and uc.appliesToChildrenTerritories = false)
      or app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?2 and uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true)
      or app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and ?2 in (select childTerritory.id from Territory childTerritory where childTerritory member of uc.territory.members) and uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true)
      """)
  Page<Application> findByRestrictedUserAndTerritory(
      String username, Integer territoryId, Pageable pageable);

  @RestResource(exported = false)
  @Query(
      """
    select distinct app from Application app
    where (app.id in (select distinct app.id from Application app, UserConfiguration uc
      where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?2 and uc.appliesToChildrenTerritories = false)
    or app.id in (select distinct app.id from Application app, UserConfiguration uc
      where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?2 and uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true)
    or app.id in (select distinct app.id from Application app, UserConfiguration uc
      where uc.role member of app.availableRoles and uc.user.username = ?1 and ?2 in (select childTerritory.id from Territory childTerritory where childTerritory member of uc.territory.members) and uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true))
    and app.appPrivate = false
    """)
  Page<Application> findByPublicUserAndTerritory(
      String username, Integer territoryId, Pageable pageable);

  @RestResource(exported = false)
  @Query(
      """
      select distinct app from Application app
      where app.id in (select distinct app.id from Application app, UserConfiguration uc
        where app.id = ?2 and uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?3 and uc.appliesToChildrenTerritories = false)
      or app.id in (select distinct app.id from Application app, UserConfiguration uc
        where app.id = ?2 and uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?3 and uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true)
      or app.id in (select distinct app.id from Application app, UserConfiguration uc
        where app.id = ?2 and uc.role member of app.availableRoles and uc.user.username = ?1 and ?3 in (select childTerritory.id from Territory childTerritory where childTerritory member of uc.territory.members) and uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true)
      """)
  Optional<Application> findByRestrictedUserApplicationAndTerritory(
      String username, Integer appId, Integer territoryId);

  @RestResource(exported = false)
  @Query(
      """
    select distinct app from Application app
    where (app.id in (select distinct app.id from Application app, UserConfiguration uc
      where app.id = ?2 and uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?3 and uc.appliesToChildrenTerritories = false)
    or app.id in (select distinct app.id from Application app, UserConfiguration uc
      where app.id = ?2 and uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?3 and uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true)
    or app.id in (select distinct app.id from Application app, UserConfiguration uc
      where app.id = ?2 and uc.role member of app.availableRoles and uc.user.username = ?1 and ?3 in (select childTerritory.id from Territory childTerritory where childTerritory member of uc.territory.members) and uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true))
    and app.appPrivate = false
    """)
  Optional<Application> findByPublicUserApplicationAndTerritory(
      String username, Integer appId, Integer territoryId);

  @RestResource(exported = false)
  @Query(
      """
      select distinct app from Application app
      where (app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.appliesToChildrenTerritories = false)
      or app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true)
      or app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory in (select childTerritory from Territory childTerritory where childTerritory member of uc.territory.members) and uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true))
      and (lower(coalesce(app.title, app.name)) like lower(concat('%', ?2, '%'))
        or lower(coalesce(app.description, '')) like lower(concat('%', ?2, '%'))
        or lower(app.name) like lower(concat('%', ?2, '%')))
  """)
  Page<Application> findByUserAndKeywords(String username, String keywords, Pageable pageable);

  @RestResource(exported = false)
  @Query(
      """
      select distinct app from Application app
      where (app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.appliesToChildrenTerritories = false)
      or app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true)
      or app.id in (select distinct app.id from Application app, UserConfiguration uc
        where uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory in (select childTerritory from Territory childTerritory where childTerritory member of uc.territory.members) and uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true))
      and app.appPrivate = false
      and (lower(coalesce(app.title, app.name)) like lower(concat('%', ?2, '%'))
        or lower(coalesce(app.description, '')) like lower(concat('%', ?2, '%'))
        or lower(app.name) like lower(concat('%', ?2, '%')))
  """)
  Page<Application> findByPublicUserAndKeywords(
      String username, String keywords, Pageable pageable);
}
