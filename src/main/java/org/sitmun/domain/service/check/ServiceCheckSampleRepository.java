package org.sitmun.domain.service.check;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ServiceCheckSampleRepository extends JpaRepository<ServiceCheckSample, Integer> {

  List<ServiceCheckSample> findByService_IdAndObserverOrderByIdAsc(
      Integer serviceId, String observer);

  List<ServiceCheckSample> findByObservedAtGreaterThanEqualAndObservedAtLessThan(
      Instant fromInclusive, Instant toExclusive);

  List<ServiceCheckSample> findByService_IdAndObservedAtGreaterThanEqualAndObservedAtLessThan(
      Integer serviceId, Instant fromInclusive, Instant toExclusive);

  List<ServiceCheckSample>
      findByService_IdAndObservedAtGreaterThanEqualAndObservedAtLessThanEqualOrderByObservedAtAscIdAsc(
          Integer serviceId, Instant fromInclusive, Instant toInclusive);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from ServiceCheckSample sample where sample.observedAt < :cutoff")
  int deleteObservedBefore(@Param("cutoff") Instant cutoff);
}
