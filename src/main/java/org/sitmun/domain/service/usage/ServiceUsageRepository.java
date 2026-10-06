package org.sitmun.domain.service.usage;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ServiceUsageRepository extends JpaRepository<ServiceUsage, ServiceUsageKey> {

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update ServiceUsage u
      set u.requests = u.requests + :requests, u.failed = u.failed + :failed
      where u.id.serviceId = :serviceId
        and u.id.applicationId = :applicationId
        and u.id.usageDay = :usageDay
        and u.id.operation = :operation
      """)
  int add(
      @Param("serviceId") Integer serviceId,
      @Param("applicationId") Integer applicationId,
      @Param("usageDay") LocalDate usageDay,
      @Param("operation") String operation,
      @Param("requests") long requests,
      @Param("failed") long failed);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from ServiceUsage u where u.id.usageDay < :cutoff")
  int deleteByUsageDayBefore(@Param("cutoff") LocalDate cutoff);

  @Query(
      """
      select u from ServiceUsage u
      where u.id.usageDay >= :from and u.id.usageDay < :to
      """)
  List<ServiceUsage> findInWindow(@Param("from") LocalDate from, @Param("to") LocalDate to);

  List<ServiceUsage> findByIdServiceIdAndIdOperation(Integer serviceId, String operation);

  List<ServiceUsage> findByIdServiceIdAndIdUsageDayGreaterThanEqualAndIdUsageDayLessThan(
      Integer serviceId, LocalDate from, LocalDate to);
}
