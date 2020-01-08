package org.sitmun.plugin.core.web.rest;

import java.math.BigInteger;
import java.util.List;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.repository.TreeNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

@RepositoryRestController
public class TreeNodeResource {

  private TreeNodeRepository treeNodeRepository;

  @Autowired
  private RepositoryEntityLinks links;

  public TreeNodeResource(TreeNodeRepository treeNodeRepository) {
    super();
    this.treeNodeRepository = treeNodeRepository;
  }

  @GetMapping("/tree-nodes/{id}/cartography")
  public ResponseEntity<?> getTreeNodeCartography(@PathVariable BigInteger id) {
    Cartography cartography = null;
    List<Cartography> cartographys = treeNodeRepository.findCartography(id);
    if (cartographys.size() > 0) {
      cartography = cartographys.get(0);
    }
    if (cartography != null) {
      Resource<ResourceSupport> resource = new Resource<ResourceSupport>(cartography.toResource(links));
      //resource.add(linkTo(methodOn(TreeNodeResource.class).getTreeNodeCartography(id)).withSelfRel());
      return ResponseEntity.ok(resource);
    } else {
      return ResponseEntity.notFound().build();
    }
  }


  @ResponseStatus(value = HttpStatus.CONFLICT, reason = "Data integrity violation") // 409
  @ExceptionHandler(DataIntegrityViolationException.class)
  public void conflict() {
    // Nothing to do
  }

}
