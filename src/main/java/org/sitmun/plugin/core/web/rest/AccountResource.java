package org.sitmun.plugin.core.web.rest;

import java.util.Optional;
import javax.validation.Valid;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.repository.UserRepository;
import org.sitmun.plugin.core.security.SecurityUtils;
import org.sitmun.plugin.core.service.UserService;
import org.sitmun.plugin.core.service.dto.UserDTO;
import org.sitmun.plugin.core.web.rest.dto.PasswordDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@RepositoryRestController
@RequestMapping("/api/account")
public class AccountResource {

  private UserService userService;

  private UserRepository userRepository;

  @Autowired
  private RepositoryEntityLinks links;

  public AccountResource(UserService userService, UserRepository userRepository) {
    super();
    this.userService = userService;
    this.userRepository = userRepository;
  }

  @PostMapping("")
  @ResponseBody
  public ResponseEntity<Void> saveAccount(@Valid @RequestBody UserDTO userDTO) {
    Optional<String> optLogin = SecurityUtils.getCurrentUserLogin();
    if (optLogin.isPresent()) {
      Optional<User> user = userRepository.findOneByUsername(optLogin.get());
      if (user.isPresent()) {
        userService.updateUser(user.get().getId(), userDTO.getFirstName(), userDTO.getLastName());
        return ResponseEntity.ok().build();
      } else {
        return ResponseEntity.notFound().build();
      }
    } else {
      return ResponseEntity.notFound().build();
    }

  }

  @GetMapping("")
  @ResponseBody
  public ResponseEntity<ResourceSupport> getAccount() {
    Optional<String> optLogin = SecurityUtils.getCurrentUserLogin();
    if (optLogin.isPresent()) {
      Optional<User> user = userRepository.findOneWithPermissionsByUsername(optLogin.get());
      if (user.isPresent()) {
        return ResponseEntity.ok(
            toResource(user.get()));
      } else {
        return ResponseEntity.notFound().build();
      }

    } else {
      return ResponseEntity.notFound().build();
    }
  }

  private ResourceSupport toResource(User user) {
    UserDTO dto = new UserDTO(user);
    Link selfLink = links.linkForSingleResource(user).withSelfRel();

    return new Resource<>(dto);
  }

  @PostMapping(path = "/change-password")
  @ResponseBody
  public ResponseEntity<Void> changePassword(@RequestBody PasswordDTO password) {
    Optional<String> optLogin = SecurityUtils.getCurrentUserLogin();
    if (optLogin.isPresent()) {
      Optional<User> user = userRepository.findOneByUsername(optLogin.get());
      if (user.isPresent()) {
        userService.changeUserPassword(user.get().getId(), password.getPassword());
        return ResponseEntity.ok().build();
      } else {
        return ResponseEntity.notFound().build();
      }

    } else {
      return ResponseEntity.notFound().build();
    }


  }

}
