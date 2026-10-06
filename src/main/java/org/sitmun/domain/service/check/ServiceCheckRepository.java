package org.sitmun.domain.service.check;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;
import org.sitmun.administration.service.access.DueService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ServiceCheckRepository extends JpaRepository<ServiceCheck, Integer> {

  Optional<ServiceCheck> findByService_IdAndObserver(Integer serviceId, String observer);

  @Query(
      """
      select new org.sitmun.administration.service.access.DueService(
        s.id, s.serviceURL, c.observedAt)
      from Service s
      left join ServiceCheck c on c.service = s and c.observer = :observer
      where c is null or c.observedAt < :dueBefore
      order by c.observedAt asc nulls first, s.id asc
      """)
  Stream<DueService> streamDue(
      @Param("observer") String observer, @Param("dueBefore") Instant dueBefore);

  @Query(
      """
      select min(c.observedAt) from ServiceCheck c
      where c.observer = :observer and c.observedAt < :dueBefore
      """)
  Instant oldestDueObservedAt(
      @Param("observer") String observer, @Param("dueBefore") Instant dueBefore);

  @Query(
      """
      select min(s.createdDate) from Service s
      where not exists (
        select c.id from ServiceCheck c
        where c.service = s and c.observer = :observer)
      """)
  Date oldestUnprobedCreated(@Param("observer") String observer);
}
