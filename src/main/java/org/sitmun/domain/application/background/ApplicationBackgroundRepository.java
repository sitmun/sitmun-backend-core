package org.sitmun.domain.application.background;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "application background")
@RepositoryRestResource(
    collectionResourceRel = "application-backgrounds",
    path = "application-backgrounds")
public interface ApplicationBackgroundRepository
    extends JpaRepository<ApplicationBackground, Integer> {}
