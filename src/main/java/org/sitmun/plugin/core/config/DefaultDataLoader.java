package org.sitmun.plugin.core.config;

import java.util.ArrayList;
import java.util.Optional;
import org.sitmun.plugin.core.domain.Role;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.repository.RoleRepository;
import org.sitmun.plugin.core.repository.TerritoryRepository;
import org.sitmun.plugin.core.repository.UserConfigurationRepository;
import org.sitmun.plugin.core.repository.UserRepository;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.SecurityConstants;
import org.sitmun.plugin.core.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Profile("diba")
@Deprecated
public class DefaultDataLoader implements ApplicationRunner {

  final UserRepository userRepository;
  final UserConfigurationRepository userConfigurationRepository;
  final RoleRepository roleRepository;
  final TerritoryRepository territoryRepository;
  final UserService userService;

  @Value("${default.territory.name}")
  private String defaultTerritoryName;

  /**
   * Default data loader.
   *
   * @param userRepository              user repository
   * @param userConfigurationRepository user configuration repository
   * @param roleRepository              role repository
   * @param territoryRepository         territory repository
   * @param userService                 user service
   */
  public DefaultDataLoader(UserRepository userRepository,
                           UserConfigurationRepository userConfigurationRepository,
                           RoleRepository roleRepository,
                           TerritoryRepository territoryRepository,
                           UserService userService) {
    this.userRepository = userRepository;
    this.userConfigurationRepository = userConfigurationRepository;
    this.roleRepository = roleRepository;
    this.territoryRepository = territoryRepository;
    this.userService = userService;
  }

  @Override
  public void run(ApplicationArguments args) {

    ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.ROLE_ADMIN));

    UsernamePasswordAuthenticationToken authReq;
    authReq = new UsernamePasswordAuthenticationToken("system", "system", authorities);

    SecurityContext sc = SecurityContextHolder.getContext();
    sc.setAuthentication(authReq);

    Optional<Role> optRole = roleRepository.findOneByName(AuthoritiesConstants.ADMIN_SITMUN);
    Role sitmunAdminRole;
    if (!optRole.isPresent()) {
      sitmunAdminRole = Role.builder()
          .setName(AuthoritiesConstants.ADMIN_SITMUN)
          .build();
      roleRepository.save(sitmunAdminRole);
    } else {
      sitmunAdminRole = optRole.get();
    }
    Role organizacionAdminRole;
    optRole = roleRepository.findOneByName(AuthoritiesConstants.ADMIN_ORGANIZACION);
    if (!optRole.isPresent()) {
      organizacionAdminRole = Role.builder()
          .setName(AuthoritiesConstants.ADMIN_ORGANIZACION)
          .build();
      roleRepository.save(organizacionAdminRole);
    }
    optRole = roleRepository.findOneByName(AuthoritiesConstants.USUARIO_TERRITORIAL);
    Role territorialRole;
    if (!optRole.isPresent()) {
      territorialRole = Role.builder()
          .setName(AuthoritiesConstants.USUARIO_TERRITORIAL)
          .build();
      roleRepository.save(territorialRole);
    } else {
      territorialRole = optRole.get();
    }

    optRole = roleRepository.findOneByName(AuthoritiesConstants.USUARIO_PUBLICO);
    Role publicRole;
    if (!optRole.isPresent()) {
      publicRole = Role.builder()
          .setName(AuthoritiesConstants.USUARIO_PUBLICO)
          .build();
      roleRepository.save(publicRole);
    } else {
      publicRole = optRole.get();
    }
    Optional<Territory> optTerritory = territoryRepository.findOneByName(this.defaultTerritoryName);
    Territory defaultTerritory;
    if (!optTerritory.isPresent()) {
      defaultTerritory = Territory.builder()
          .setName(this.defaultTerritoryName)
          .setCode("")
          .setBlocked(false)
          .build();
      territoryRepository.save(defaultTerritory);
    } else {
      defaultTerritory = optTerritory.get();
    }

    // Sitmun Admin
    User sitmunAdmin;
    Optional<User> optUser;
    optUser = userRepository.findOneByUsername(SecurityConstants.SITMUN_ADMIN_USERNAME);
    if (!optUser.isPresent()) {
      sitmunAdmin = new User();
      sitmunAdmin.setAdministrator(true);
      sitmunAdmin.setBlocked(false);
      sitmunAdmin.setFirstName("Admin");
      sitmunAdmin.setLastName("Sitmun");
      sitmunAdmin.setUsername(SecurityConstants.SITMUN_ADMIN_USERNAME);
      sitmunAdmin.setPassword("admin");
      sitmunAdmin = userService.createUser(sitmunAdmin);
      UserConfiguration userConf = new UserConfiguration();
      userConf.setTerritory(defaultTerritory);
      userConf.setRole(sitmunAdminRole);
      userConf.setUser(sitmunAdmin);
      this.userConfigurationRepository.save(userConf);
      userConf = new UserConfiguration();
      userConf.setTerritory(defaultTerritory);
      userConf.setRole(territorialRole);
      userConf.setUser(sitmunAdmin);
      this.userConfigurationRepository.save(userConf);
      userConf = new UserConfiguration();
      userConf.setTerritory(defaultTerritory);
      userConf.setRole(publicRole);
      userConf.setUser(sitmunAdmin);
      this.userConfigurationRepository.save(userConf);
    }

    // Sitmun Public User
    User sitmunPublicUser;
    optUser = userRepository.findOneByUsername(SecurityConstants.SITMUN_PUBLIC_USERNAME);
    if (!optUser.isPresent()) {
      sitmunPublicUser = new User();
      sitmunPublicUser.setAdministrator(false);
      sitmunPublicUser.setBlocked(false);
      sitmunPublicUser.setFirstName("Public");
      sitmunPublicUser.setLastName("Sitmun");
      sitmunPublicUser.setUsername(SecurityConstants.SITMUN_PUBLIC_USERNAME);
      sitmunPublicUser.setPassword("public");
      sitmunPublicUser = userService.createUser(sitmunPublicUser);
      UserConfiguration userConf = new UserConfiguration();
      userConf.setTerritory(defaultTerritory);
      userConf.setRole(publicRole);
      userConf.setUser(sitmunPublicUser);
      this.userConfigurationRepository.save(userConf);
    }

    sc.setAuthentication(null);
    SecurityContextHolder.clearContext();

  }
}