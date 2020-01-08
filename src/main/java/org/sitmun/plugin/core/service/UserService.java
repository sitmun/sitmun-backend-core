package org.sitmun.plugin.core.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sitmun.plugin.core.domain.Role;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.repository.RoleRepository;
import org.sitmun.plugin.core.repository.UserConfigurationRepository;
import org.sitmun.plugin.core.repository.UserRepository;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.PermissionResolver;
import org.sitmun.plugin.core.security.SecurityUtils;
import org.sitmun.plugin.core.service.dto.UserDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements PermissionResolver<User> {

  private UserRepository applicationUserRepository;
  private UserConfigurationRepository userConfigurationRepository;
  private RoleRepository roleRepository;
  private PasswordEncoder bcryptPasswordEncoder;

  public UserService(UserRepository applicationUserRepository, PasswordEncoder bcryptPasswordEncoder,
                     UserConfigurationRepository userConfigurationRepository, RoleRepository roleRepository) {
    super();
    this.applicationUserRepository = applicationUserRepository;
    this.bcryptPasswordEncoder = bcryptPasswordEncoder;
    this.userConfigurationRepository = userConfigurationRepository;
    this.roleRepository = roleRepository;
  }

  public User createUser(User user) {
    if (user.getPassword() != null) {
      user.setPassword(bcryptPasswordEncoder.encode(user.getPassword()));
    }
    boolean newUser = (user.getId() == null);
    user = applicationUserRepository.save(user);

    // fix for creation with "ORGANIZATION ADMIN" role
    if (newUser) {
      Optional<String> currentUserLogin = SecurityUtils.getCurrentUserLogin();
      if (currentUserLogin.isPresent()) {
        Optional<User> currentUser = getUserWithPermissionsByUsername(currentUserLogin.get());

        if (currentUser.isPresent()) {

          Set<UserConfiguration> permissions = currentUser.get().getPermissions();
          boolean isAdminSitmun = permissions.stream()
                                      .anyMatch(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_SITMUN));
          Optional<UserConfiguration> baseConfiguration = permissions.stream()
                                                              .filter(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_ORGANIZACION))
                                                              .findFirst();
          Optional<Role> territorialRole = this.roleRepository.findOneByName(AuthoritiesConstants.USUARIO_TERRITORIAL);

          if (!isAdminSitmun && baseConfiguration.isPresent() && territorialRole.isPresent()) {
            // get the first territory with ADMIN_ORGANIZACION role
            Territory territory = baseConfiguration.get().getTerritory();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setTerritory(territory);
            userConfiguration.setUser(user);
            userConfiguration.setRole(territorialRole.get());
            this.userConfigurationRepository.save(userConfiguration);
          }
        }
      }
    }
    return user;
  }

  public Optional<UserDTO> updateUser(UserDTO userDTO) {
    return Optional.of(applicationUserRepository.findOne(userDTO.getId())).map(user -> {
      user.setUsername(userDTO.getUsername());
      user.setFirstName(userDTO.getFirstName());
      user.setLastName(userDTO.getLastName());
      user.setAdministrator(userDTO.getAdministrator());
      user.setBlocked(userDTO.getBlocked());
      // user.setPositions(userDTO.getPositions());
      // user.setPermissions(userDTO.getPermissions());
      applicationUserRepository.save(user);
      return user;
    }).map(UserDTO::new);

  }

  public Optional<User> findUser(BigInteger id) {
    return Optional.of(applicationUserRepository.findOne(id));
  }

  public void changeUserPassword(BigInteger id, String password) {
    User user = applicationUserRepository.findOne(id);
    user.setPassword(bcryptPasswordEncoder.encode(password));
    applicationUserRepository.save(user);
  }

  public void updateUser(BigInteger id, String firstName, String lastName) {
    User user = applicationUserRepository.findOne(id);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    applicationUserRepository.save(user);
  }

  public List<User> findAllUsers() {
    ArrayList<User> res = new ArrayList<>();
    applicationUserRepository.findAll().forEach(res::add);
    return res;
  }

  public boolean resolvePermission(User authUser, User user, String permission) {
	  if (authUser.getId().equals(user.getId())) {
		  return true;
	  }
    Set<UserConfiguration> permissions = authUser.getPermissions();
    boolean isAdminSitmun = permissions.stream()
                                .anyMatch(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_SITMUN));
    boolean isAdminOrganization = permissions.stream()
                                      .anyMatch(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_ORGANIZACION));

	  if (isAdminSitmun) {
		  return true;
	  }

    // si tengo el rol de admin de territorio y el usuario tiene permisos asociados
    // a este territorio
    if (isAdminOrganization) {
      if (user.getId() != null) {
        return this.getUserWithPermissionsByUsername(user.getUsername()).map(u ->
                                                                                 u.getPermissions().stream()
                                                                                     .anyMatch(targetDomainObjectPermissions ->
                                                                                                   permissions.stream()
                                                                                                       .filter(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_ORGANIZACION))
                                                                                                       .map(UserConfiguration::getTerritory).map(Territory::getId).collect(Collectors.toList())
                                                                                                       .contains(targetDomainObjectPermissions.getTerritory().getId())
                                                                                     )
        ).orElse(false);
      } else {
        return true;
      }
    }

    return false;
  }

  public Optional<User> getUserWithPermissionsByUsername(String name) {
    return applicationUserRepository.findOneWithPermissionsByUsername(name);
  }

}
