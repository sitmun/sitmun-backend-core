package org.sitmun.domain.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Optional;

@Tag(name = "user")
@RepositoryRestResource(collectionResourceRel = "users", path = "users")
public interface UserRepository extends JpaRepository<User, Integer> {

  @RestResource(exported = false)
  Optional<User> findByUsername(String username);

  @RestResource(exported = false)
  @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
  Optional<User> findByEmail(@Param("email") String email);
}