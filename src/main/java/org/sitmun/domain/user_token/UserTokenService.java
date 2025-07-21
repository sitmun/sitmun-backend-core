package org.sitmun.domain.user_token;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UserTokenService {

  private final UserTokenRepository userTokenRepository;

  public UserTokenService(UserTokenRepository userTokenRepository) {
    this.userTokenRepository = userTokenRepository;
  }

  public UserToken saveUserToken(UserTokenDTO userTokenDTO) {
    UserToken userToken =
        UserToken.builder()
            .userMail(userTokenDTO.getUserMail())
            .tokenId(userTokenDTO.getTokenId())
            .expireAt(userTokenDTO.getExpireAt())
            .build();

    return this.userTokenRepository.save(userToken);
  }

  public UserTokenDTO getUserTokenByToken(String token) {
    Optional<UserToken> userTokenOptional = this.userTokenRepository.findByTokenId(token);
    return userTokenOptional.map(this::toUserDTO).orElse(null);
  }

  public void deleteUserToken(UserTokenDTO userTokenDTO) {
    this.userTokenRepository.deleteById(userTokenDTO.getId());
  }

  public UserTokenDTO updateUserToken(String userMail, String token, boolean newIsActive) {
    Optional<UserToken> userTokenOptional =
        this.userTokenRepository.findByUserMailAndTokenId(userMail, token);
    if (userTokenOptional.isEmpty()) {
      return null;
    }
    UserToken userToken = userTokenOptional.get();

    this.userTokenRepository.save(userToken);

    return toUserDTO(userToken);
  }

  private UserTokenDTO toUserDTO(UserToken userToken) {
    if (userToken == null) {
      return null;
    }

    return UserTokenDTO.builder()
        .id(userToken.getId())
        .userMail(userToken.getUserMail())
        .tokenId(userToken.getTokenId())
        .expireAt(userToken.getExpireAt())
        .build();
  }
}
