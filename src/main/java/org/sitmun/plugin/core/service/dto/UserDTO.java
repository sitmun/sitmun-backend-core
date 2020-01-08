package org.sitmun.plugin.core.service.dto;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sitmun.plugin.core.domain.User;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.core.Relation;

@Relation(value = "user", collectionRelation = "users")
public class UserDTO implements Identifiable<BigInteger> {

  private BigInteger id;
  private String username;
  private String firstName;
  private String lastName;
  private Boolean administrator;
  private Boolean blocked;

  private Set<String> authorities;

  private Set<String> territories;

  private Map<String, Set<String>> authoritiesPerTerritory;

  public UserDTO() {
    super();
  }

  public UserDTO(User user) {
    this.id = user.getId();

    this.username = user.getUsername();

    this.firstName = user.getFirstName();

    this.lastName = user.getLastName();

    this.administrator = user.getAdministrator();

    this.blocked = user.getBlocked();
    authorities = new HashSet<>();
    territories = new HashSet<>();
    authoritiesPerTerritory = new HashMap<>();
    if (!user.getPermissions().isEmpty()) {
      user.getPermissions().stream().forEach(p -> {
        Set<String> territoryAuths = authoritiesPerTerritory.get(p.getTerritory().getName());
        if (territoryAuths == null) {
          authoritiesPerTerritory.put(p.getTerritory().getName(), new HashSet<>());
        }
        authoritiesPerTerritory.get(p.getTerritory().getName()).add(p.getRole().getName());
        authorities.add(p.getRole().getName());
        territories.add(p.getTerritory().getName());
      });
    }
  }

  public Set<String> getTerritories() {
    return territories;
  }

  public Set<String> getAuthorities() {
    return authorities;
  }

  public Map<String, Set<String>> getAuthoritiesPerTerritory() {
    return authoritiesPerTerritory;
  }

  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public Boolean getAdministrator() {
    return administrator;
  }

  public void setAdministrator(Boolean administrator) {
    this.administrator = administrator;
  }

  public Boolean getBlocked() {
    return blocked;
  }

  public void setBlocked(Boolean blocked) {
    this.blocked = blocked;
  }
  /*
   *
   * private Set<UserPosition> positions = new HashSet<>();
   *
   * private Set<UserConfiguration> permissions = new HashSet<>();
   */

}
