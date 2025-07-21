package org.sitmun.domain.user_token;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/api/userTokenValid")
@Tag(name = "userToken", description = "user token controller")
public class UserTokenController {
  private final UserTokenService userTokenService;

  public UserTokenController(UserTokenService userTokenService){
    this.userTokenService = userTokenService;
  }

  @GetMapping
  public ResponseEntity<Boolean> isTokenValid(String token) {
    UserTokenDTO userToken = this.userTokenService.getUserTokenByToken(token);
    if(userToken == null){
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
    boolean isTokenValid = false;
    if(new Date().before(userToken.getExpireAt())){
      isTokenValid = true;
    }
    return ResponseEntity.status(HttpStatus.OK).body(isTokenValid);
  }
}
