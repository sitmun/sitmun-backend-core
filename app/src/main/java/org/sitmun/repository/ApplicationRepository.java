package org.sitmun.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.Application;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;


@Tag(name = "application")
@RepositoryRestResource(collectionResourceRel = "applications", path = "applications"/*, excerptProjection = ApplicationProjection.class*/)
public interface ApplicationRepository extends PagingAndSortingRepository<Application, Integer> {

  /**
   * The SQL equivalent is:
   * <pre>
   * SELECT DISTINCT app.app_id, uc.uco_terid
   * FROM STM_APP app CROSS JOIN STM_USR_CONF uc
   * WHERE uc.uco_roleid IN (SELECT app_rol.aro_roleid
   *                         FROM STM_APP_ROL app_rol
   *                         WHERE app.app_id=app_rol.aro_appid)
   * </pre>
   */
  @RestResource(exported = false)
  @Query("select distinct app.id, uc.territory.id from Application app, UserConfiguration uc where uc.role member app.availableRoles")
  List<Object[]> listIdApplicationsPerTerritories();

}