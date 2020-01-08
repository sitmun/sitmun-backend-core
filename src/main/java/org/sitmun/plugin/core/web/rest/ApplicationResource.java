package org.sitmun.plugin.core.web.rest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;


import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import org.sitmun.plugin.core.domain.ApplicationBackground;
import org.sitmun.plugin.core.domain.CartographyGroup;
import org.sitmun.plugin.core.domain.Tree;
import org.sitmun.plugin.core.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

@RepositoryRestController
public class ApplicationResource {

  private ApplicationRepository applicationRepository;

  @Autowired
  private RepositoryEntityLinks links;

  public ApplicationResource(ApplicationRepository applicationRepository) {
    super();
    this.applicationRepository = applicationRepository;
  }

  @GetMapping("/applications/{id}/trees")
  public ResponseEntity<?> getApplicationTrees(@PathVariable BigInteger id) {
    List<Tree> trees = applicationRepository.findApplicationTrees(id);

    Resources<ResourceSupport> resources = new Resources<ResourceSupport>(
        trees.stream().map(tree -> tree.toResource(links)).collect(Collectors.toList()));

    resources.add(linkTo(methodOn(ApplicationResource.class).getApplicationTrees(id)).withSelfRel());
    return ResponseEntity.ok(resources);
  }

  @GetMapping("/applications/{id}/backgrounds")
  public ResponseEntity<?> getBackgrounds(@PathVariable BigInteger id) {
    List<ApplicationBackground> backgrounds = applicationRepository.findApplicationBackgrounds(id);

    Resources<ResourceSupport> resources = new Resources<ResourceSupport>(
        backgrounds.stream().map(background -> background.toResource(links)).collect(Collectors.toList()));

    resources.add(linkTo(methodOn(ApplicationResource.class).getBackgrounds(id)).withSelfRel());
    return ResponseEntity.ok(resources);
  }

  @GetMapping("/applications/{id}/situationMap")
  public ResponseEntity<?> getApplicationSituationMap(@PathVariable BigInteger id) {
    CartographyGroup situationMap = null;
    List<CartographyGroup> situationMaps = applicationRepository.findSituationMap(id);
    if (situationMaps.size() > 0) {
      situationMap = situationMaps.get(0);
    }

    if (situationMap != null) {

      Resource<ResourceSupport> resource = new Resource<ResourceSupport>(situationMap.toResource(links));

      //resource.add(linkTo(methodOn(ApplicationResource.class).getApplicationSituationMap(id)).withSelfRel());
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
