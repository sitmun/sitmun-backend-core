package org.sitmun.plugin.core.web.rest;

//import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
//import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;


import java.math.BigInteger;
import java.util.List;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.repository.CartographyGroupRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

@RepositoryRestController
public class CartographyGroupResource {

  private final CartographyGroupRepository cartographyGroupRepository;

  //@Autowired
  //private RepositoryEntityLinks links;

  public CartographyGroupResource(CartographyGroupRepository cartographyGroupRepository) {
    super();
    this.cartographyGroupRepository = cartographyGroupRepository;
  }

  @GetMapping("/cartography-groups/{id}/members")
  public ResponseEntity<List<Cartography>> getCartographyGroupMembers(@PathVariable BigInteger id) {
    List<Cartography> cartographies = cartographyGroupRepository.findCartographyMembers(id);
    // Resources<ResourceSupport> resources = new Resources<ResourceSupport>(
    //   cartographies.stream().map(cartography -> cartography.toResource(links)).collect(Collectors.toList()));
    // resources.add(linkTo(methodOn(CartographyGroupResource.class).getCartographyGroupMembers(id)).withSelfRel());
    return ResponseEntity.ok(cartographies);
  }


  @ResponseStatus(value = HttpStatus.CONFLICT, reason = "Data integrity violation") // 409
  @ExceptionHandler(DataIntegrityViolationException.class)
  public void conflict() {
    // Nothing to do
  }

}
