package org.sitmun.domain.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * Repository interface for User entities.
 *
 * <p>This repository provides methods to: - Perform CRUD operations on User entities - Find users
 * by username or email (case-insensitive) - Support REST resource exposure with custom path
 *
 * <p>Security note: - Some methods are not exported as REST resources for security - Username and
 * email lookups are restricted to internal use
 */
@Tag(name = "user")
@RepositoryRestResource(collectionResourceRel = "users", path = "users")
public interface UserRepository extends JpaRepository<User, Integer> {

  /**
   * Find a user by their username. Not exposed as a REST resource for security reasons.
   *
   * @param username the username to search for
   * @return the user if found
   */
  @RestResource(exported = false)
  Optional<User> findByUsername(String username);

  /**
   * Find a user by their email address (case-insensitive). Not exposed as a REST resource for
   * security reasons.
   *
   * @param email the email to search for
   * @return the user if found
   */
  @RestResource(exported = false)
  @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
  Optional<User> findByEmail(@Param("email") String email);
}
