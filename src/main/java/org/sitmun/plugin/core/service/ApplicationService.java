package org.sitmun.plugin.core.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sitmun.plugin.core.domain.Application;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.repository.ApplicationRepository;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.PermissionResolver;
import org.sitmun.plugin.core.security.SecurityConstants;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService implements PermissionResolver<Application> {

  private final ApplicationRepository applicationApplicationRepository;

  /*
  private final RoleRepository roleRepository;
  private final TreeRepository treeRepository;
  private final TreeNodeRepository treeNodeRepository;
  private final CartographyRepository cartographyRepository;
  private final ServiceRepository serviceRepository;
*/
  public ApplicationService(ApplicationRepository applicationApplicationRepository /*, RoleRepository roleRepository,
			TreeRepository treeRepository, TreeNodeRepository treeNodeRepository,
			CartographyRepository cartographyRepository, ServiceRepository serviceRepository*/) {
    super();
    this.applicationApplicationRepository = applicationApplicationRepository;
		/*
		this.roleRepository = roleRepository;
		this.treeRepository = treeRepository;
		this.treeNodeRepository = treeNodeRepository;
		this.cartographyRepository = cartographyRepository;
		this.serviceRepository = serviceRepository;
		*/
  }

  public Optional<Application> findApplication(BigInteger id) {
    return Optional.of(applicationApplicationRepository.findOne(id));
  }

  public List<Application> findAllApplications() {
    ArrayList<Application> res = new ArrayList<>();
    applicationApplicationRepository.findAll().forEach(res::add);
    return res;
  }

  /*
      public Application findApplicationWithFullData(Long id) {
          Application application = applicationApplicationRepository.findOneWithEagerRelationships(id);
          // TODO
          Set<Tree> trees = application.getTrees();
          for (Tree tree : trees) {
              tree = treeRepository.findOneWithEagerRelationships(tree.getId());
              Set<TreeNode> nodes = tree.getNodes();
              for (TreeNode treeNode : nodes) {
                  treeNode = treeNodeRepository.findOneWithEagerRelationships(treeNode.getId());
                  Cartography cartography = treeNode.getCartography();
                  cartography = cartographyRepository.findOneWithEagerRelationships(cartography.getId());
                  org.sitmun.plugin.core.domain.Service service = cartography.getService();
                  service = serviceRepository.findOneWithEagerRelationships(service.getId());
              }

          }
          Set<ApplicationBackground> backgrounds = application.getBackgrounds();
          return application;
      }
  */
  @Override
  public boolean resolvePermission(User authUser, Application entity, String permission) {
    Set<UserConfiguration> permissions = authUser.getPermissions();
    boolean isAdminSitmun = permissions.stream()
                                .anyMatch(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_SITMUN));
    if (isAdminSitmun) {
      return true;
    }

    if (permission.equalsIgnoreCase(SecurityConstants.CREATE_PERMISSION)
            || permission.equalsIgnoreCase(SecurityConstants.UPDATE_PERMISSION)
            || permission.equalsIgnoreCase(SecurityConstants.DELETE_PERMISSION)
            || permission.equalsIgnoreCase(SecurityConstants.ADMIN_PERMISSION)) {

      return false;
    } else if (permission.equalsIgnoreCase(SecurityConstants.READ_PERMISSION)) {
      return (permissions.stream().map(p -> p.getRole()).filter(entity.getAvailableRoles()::contains).count() > 0);
    }

    return false;
  }


}
