package org.sitmun.domain.user;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/** Projections for REST views of an user. */
@Projection(name = "view", types = User.class)
public interface UserProjection {
  /** User ID. */
  @Value("#{target.id}")
  Integer getId();

  /** Username of the user. */
  @Value("#{target.username}")
  String getUsername();

  @Value("#{target.firstName}")
  String getFirstName();

  @Value("#{target.lastName}")
  String getLastName();

  @Value("#{target.identificationNumber}")
  String getIdentificationNumber();

  @Value("#{target.identificationType}")
  String getIdentificationType();

  @Value("#{target.administrator}")
  Boolean isAdministrator();

  @Value("#{target.blocked}")
  Boolean isBlocked();

  /** Email of the user. */
  @Value("#{target.email}")
  String getEmail();

  @Value("#{target.createdDate}")
  Date getCreatedDate();

  @Value("#{target.lastModifiedDate}")
  Date getLastModifiedDate();

  @Value("#{target.getPasswordSet()}")
  Boolean isPasswordSet();

  /** Application configuration warnings. */
  @Value("#{@userChecksService.getWarnings(target)}")
  List<String> getWarnings();
}
