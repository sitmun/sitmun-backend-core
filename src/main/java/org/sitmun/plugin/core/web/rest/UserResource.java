package org.sitmun.plugin.core.web.rest;

import java.math.BigInteger;
import java.net.URI;
import java.util.Optional;
import javax.validation.Valid;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.service.UserService;
import org.sitmun.plugin.core.service.dto.UserDTO;
import org.sitmun.plugin.core.web.rest.dto.PasswordDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RepositoryRestController
public class UserResource {

  private UserService userService;

  @Autowired
  private RepositoryEntityLinks links;

  //@Autowired
  //private ResourceAssemblerSupport<User,ResourceSupport> assembler;

  public UserResource(UserService userService) {
    super();
    this.userService = userService;
  }

  @ResponseStatus(value = HttpStatus.CONFLICT, reason = "Data integrity violation")  // 409
  @ExceptionHandler(DataIntegrityViolationException.class)
  public void conflict() {
    // Nothing to do
  }

  /**
   * TODO: Replace User (persistent entity) with a simple POJO or DTO object (squid:S4684)
   *
   * @param user a user
   * @return a response
   */
  @PostMapping("/users")
  @SuppressWarnings("squid:S4684")
  @PreAuthorize("hasPermission(#user, 'administration') or hasPermission(#user, 'write')")
  public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
    User result = userService.createUser(user);
    URI location = ServletUriComponentsBuilder
                       .fromCurrentRequest().path("/{id}")
                       .buildAndExpand(result.getId()).toUri();

    return ResponseEntity.created(location).build();
  }

  @PutMapping("/users/{id}")
  public ResponseEntity<User> updateUser(@PathVariable BigInteger id, @Valid @RequestBody UserDTO userDTO) {
    Optional<User> optUser = userService.findUser(id);
    if (optUser.isPresent()) {
      userDTO.setId(optUser.get().getId());
      Optional<UserDTO> updatedUser = userService.updateUser(userDTO);
      if (updatedUser.isPresent()) {
        return ResponseEntity.noContent().build();
      } else {
        return ResponseEntity.notFound().build();
      }
    } else {
      return ResponseEntity.notFound().build();
    }

  }

  /*	
	@GetMapping("/users")
    public ResponseEntity<?> getPagedUsers(Pageable pageable) {
        Page<User> users = userService.findAllUsers(pageable);
    
        Link pageSelfLink = links.linkToPagedResource(User.class,pageable).withSelfRel();
                
        PagedResources<?> resources = assembler.toResource(users , this::toResource, pageSelfLink);
        //Link pageSearchLink = links.linkToSearchResource(User.class,"search",pageable);
        //resources.add(pageSearchLink);

        return ResponseEntity.ok(resources);

    }
    
  @GetMapping("/users")
  public ResponseEntity<?> getUsers() {
	  UserResourceAssembler assembler = new UserResourceAssembler(UserRepository.class, ResourceSupport.class,links);
      List<User> users = userService.findAllUsers();
      return ResponseEntity.ok(assembler.toResources(users ));
  }
*/


  @PostMapping(path = "/users/{id}/change-password")
  public ResponseEntity<Void> changePassword(@PathVariable BigInteger id, @RequestBody PasswordDTO password) {
    Optional<User> optUser = userService.findUser(id);
    if (optUser.isPresent()) {
      userService.changeUserPassword(id, password.getPassword());

      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.notFound().build();
    }

  }

}
