package org.sitmun.plugin.core.web.rest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;


import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

@RepositoryRestController
public class ServiceResource {

  private ServiceRepository serviceRepository;

  @Autowired
  private RepositoryEntityLinks links;

  public ServiceResource(ServiceRepository serviceRepository) {
    super();
    this.serviceRepository = serviceRepository;
  }

  @GetMapping("/services/{id}/layers")
  public ResponseEntity<?> getServiceMembers(@PathVariable BigInteger id) {
    List<Cartography> cartographys = serviceRepository.findLayers(id);

    Resources<ResourceSupport> resources = new Resources<ResourceSupport>(
        cartographys.stream().map(cartography -> cartography.toResource(links)).collect(Collectors.toList()));

    resources.add(linkTo(methodOn(ServiceResource.class).getServiceMembers(id)).withSelfRel());
    return ResponseEntity.ok(resources);
  }


  @ResponseStatus(value = HttpStatus.CONFLICT, reason = "Data integrity violation") // 409
  @ExceptionHandler(DataIntegrityViolationException.class)
  public void conflict() {
    // Nothing to do
  }

}
