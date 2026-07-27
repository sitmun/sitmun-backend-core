package org.sitmun.authorization.client.service;

import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MobileEditionAccessService {

  private final ApplicationRepository applicationRepository;

  public MobileEditionAccessService(ApplicationRepository applicationRepository) {
    this.applicationRepository = applicationRepository;
  }

  public boolean hasAccessibleEditionApplication(String username) {
    return applicationRepository.findByUser(username, Pageable.unpaged()).stream()
        .anyMatch(MobileEditionAccessService::isEditionApplication);
  }

  public boolean isEditionApplicationId(Integer appId) {
    return applicationRepository.findById(appId).filter(this::isEdition).isPresent();
  }

  public boolean isEdition(Application application) {
    return isEditionApplication(application);
  }

  public static boolean isEditionApplication(Application application) {
    return application != null
        && DomainConstants.Applications.TYPE_EDITION_CODE.equalsIgnoreCase(application.getType());
  }
}
