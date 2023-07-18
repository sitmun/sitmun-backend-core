package org.sitmun.authorization.service;

import org.sitmun.domain.application.Application;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user.configuration.UserConfiguration;
import org.sitmun.domain.user.configuration.UserConfigurationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.sitmun.authorization.service.ProfileUtils.isAppPartOfUserConfiguration;

@Service
public class ProfileService {

  private final UserRepository userRepository;
  private final UserConfigurationRepository userConfigurationRepository;

  public ProfileService(UserRepository userRepository, UserConfigurationRepository userConfigurationRepository) {
    this.userRepository = userRepository;
    this.userConfigurationRepository = userConfigurationRepository;
  }


  /**
   * Get the list of territories for the user in a given application.
   *
   * @param username the username
   * @param appId    the identifier of the application
   * @param pageable pagination information
   * @return a page of territories
   */
  public Page<Territory> getApplicationTerritories(String username, Integer appId, Pageable pageable) {
    return userRepository.findByUsername(username)
      .filter(user -> Boolean.FALSE.equals(user.getBlocked()))
      .map(user -> {
        List<Territory> territories = getApplicationTerritories(user, appId);
        final int start = Math.min((int) pageable.getOffset(), territories.size());
        final int end = Math.min((start + pageable.getPageSize()), territories.size());
        return new PageImpl<>(territories.subList(start, end), pageable, territories.size());
      }).orElseGet(() -> new PageImpl<>(Collections.emptyList(), pageable, 0));
  }

  private List<Territory> getApplicationTerritories(User user, Integer appId) {
    return userConfigurationRepository.findByUser(user).stream()
      .filter(uc -> isAppPartOfUserConfiguration(appId, uc))
      .map(UserConfiguration::getTerritory)
      .distinct()
      .collect(Collectors.toList());
  }


  /**
   * Get the list of territories for the user.
   *
   * @param username the username
   * @param pageable pagination information
   * @return a page of territories
   */
  public Page<Territory> getTerritories(String username, Pageable pageable) {
    return userRepository.findByUsername(username)
      .filter(user -> Boolean.FALSE.equals(user.getBlocked()))
      .map(user -> {
        List<Territory> territories = getTerritories(user);
        final int start = Math.min((int) pageable.getOffset(), territories.size());
        final int end = Math.min((start + pageable.getPageSize()), territories.size());
        return new PageImpl<>(territories.subList(start, end), pageable, territories.size());
      }).orElseGet(() -> new PageImpl<>(Collections.emptyList(), pageable, 0));
  }

  private List<Territory> getTerritories(User user) {
    return userConfigurationRepository.findByUser(user).stream()
      .map(UserConfiguration::getTerritory)
      .distinct()
      .collect(Collectors.toList());
  }

  /**
   * Get the applications per user.
   *
   * @param username username
   * @param pageable pagination information
   * @return a page of applications
   */
  public Page<Application> getApplications(String username, Pageable pageable) {
    return userRepository.findByUsername(username)
      .filter(user -> Boolean.FALSE.equals(user.getBlocked()))
      .map(user -> {
        List<Application> applications = getApplications(user);
        final int start = Math.min((int) pageable.getOffset(), applications.size());
        final int end = Math.min((start + pageable.getPageSize()), applications.size());
        return new PageImpl<>(applications.subList(start, end), pageable, applications.size());
      })
      .orElseGet(() -> new PageImpl<>(Collections.emptyList(), pageable, 0));
  }

  private List<Application> getApplications(User user) {
    return userConfigurationRepository.findByUser(user).stream()
      .map(UserConfiguration::getRole)
      .distinct()
      .map(Role::getApplications)
      .flatMap(Set::stream)
      .distinct()
      .collect(Collectors.toList());
  }

  /**
   * Get the list of applications for a given territory.
   *
   * @param username username
   * @param terrId   territory identifier
   * @param pageable pagination information
   * @return a page of applications
   */
  public Page<Application> getTerritoryApplications(String username, Integer terrId, Pageable pageable) {
    return userRepository.findByUsername(username)
      .filter(user -> Boolean.FALSE.equals(user.getBlocked()))
      .map(user -> {
        List<Application> applications = getApplications(user, terrId);
        final int start = Math.min((int) pageable.getOffset(), applications.size());
        final int end = Math.min((start + pageable.getPageSize()), applications.size());
        return new PageImpl<>(applications.subList(start, end), pageable, applications.size());
      })
      .orElseGet(() -> new PageImpl<>(Collections.emptyList(), pageable, 0));
  }

  private List<Application> getApplications(User user, Integer terrId) {
    return userConfigurationRepository.findByUser(user).stream()
      .filter(uc -> Objects.equals(uc.getTerritory().getId(), terrId))
      .map(UserConfiguration::getRole)
      .distinct()
      .map(Role::getApplications)
      .flatMap(Set::stream)
      .distinct()
      .collect(Collectors.toList());
  }

  /**
   * Get the profile for the given user, application and territory.
   *
   * @param username the username
   * @param appId    the application id
   * @param terrId   the territory id
   * @return the profile
   */
  public Optional<Profile> buildProfile(String username, String appId, String terrId) {
    return userRepository.findByUsername(username)
      .filter(user -> !user.getBlocked())
      .flatMap(user -> ProfileUtils.buildProfile(user, appId, terrId));
  }

}
