package org.sitmun.plugin.core;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class DefaultDataLoader implements ApplicationRunner {

  @Autowired
  UserRepository userRepository;

  @Autowired
  UserConfigurationRepository userConfigurationRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  TerritoryRepository territoryRepository;
  @Autowired
  UserService userService;

  @Value("${default.territory.name}")
  private String defaultTerritoryName;

  @Override
  public void run(ApplicationArguments args) throws Exception {

    ArrayList authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.ROLE_ADMIN));

    UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken("system", "system",
        authorities);

    SecurityContext sc = SecurityContextHolder.getContext();
    sc.setAuthentication(authReq);

    Optional<Role> optRole = roleRepository.findOneByName(AuthoritiesConstants.ADMIN_SITMUN);
    Role sitmunAdminRole = null;
    if (!optRole.isPresent()) {
      sitmunAdminRole = new Role();
      sitmunAdminRole.setName(AuthoritiesConstants.ADMIN_SITMUN);
      roleRepository.save(sitmunAdminRole);
    } else {
      sitmunAdminRole = optRole.get();
    }
    Role organizacionAdminRole = null;
    optRole = roleRepository.findOneByName(AuthoritiesConstants.ADMIN_ORGANIZACION);
    if (!optRole.isPresent()) {
      organizacionAdminRole = new Role();
      organizacionAdminRole.setName(AuthoritiesConstants.ADMIN_ORGANIZACION);
      roleRepository.save(organizacionAdminRole);
    } else {
      organizacionAdminRole = optRole.get();
    }
    optRole = roleRepository.findOneByName(AuthoritiesConstants.USUARIO_TERRITORIAL);
    Role territorialRole = null;
    if (!optRole.isPresent()) {
      territorialRole = new Role();
      territorialRole.setName(AuthoritiesConstants.USUARIO_TERRITORIAL);
      roleRepository.save(territorialRole);
    } else {
      territorialRole = optRole.get();
    }

    optRole = roleRepository.findOneByName(AuthoritiesConstants.USUARIO_PUBLICO);
    Role publicRole = null;
    if (!optRole.isPresent()) {
      publicRole = new Role();
      publicRole.setName(AuthoritiesConstants.USUARIO_PUBLICO);
      roleRepository.save(publicRole);
    } else {
      publicRole = optRole.get();
    }
    Optional<Territory> optTerritory = territoryRepository.findOneByName(this.defaultTerritoryName);
    Territory defaultTerritory = null;
    if (!optTerritory.isPresent()) {
      defaultTerritory = new Territory();
      defaultTerritory.setName(this.defaultTerritoryName);
      territoryRepository.save(defaultTerritory);
    } else {
      defaultTerritory = optTerritory.get();
    }

    // Sitmun Admin
    User sitmunAdmin = null;
    Optional<User> optUser = userRepository.findOneByUsername(SecurityConstants.SITMUN_ADMIN_USERNAME);
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

    } else {
      sitmunAdmin = optUser.get();

    }

    // Sitmun Public User
    User sitmunPublicUser = null;
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

    } else {
      sitmunPublicUser = optUser.get();

    }

    sc.setAuthentication(null);
    SecurityContextHolder.clearContext();

  }
}