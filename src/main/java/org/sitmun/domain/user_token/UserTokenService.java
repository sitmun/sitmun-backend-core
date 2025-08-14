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
    UserToken userToken = this.toUser(userTokenDTO);

    return this.userTokenRepository.save(userToken);
  }

  public UserTokenDTO getUserTokenByUserId(int userId) {
    Optional<UserToken> userTokenOptional = this.userTokenRepository.findByUserID(userId);
    return userTokenOptional.map(this::toUserDTO).orElse(null);
  }

  public UserTokenDTO getUserTokenByCodeOTP(String codeOTP) {
    Optional<UserToken> userTokenOptional = this.userTokenRepository.findByCodeOTP(codeOTP);
    return userTokenOptional.map(this::toUserDTO).orElse(null);
  }

  public void deleteUserToken(UserTokenDTO userTokenDTO) {
    this.userTokenRepository.deleteById(userTokenDTO.getId());
  }

  public void deleteUserTokenByUserId(int userId) {
    this.userTokenRepository.deleteByUserID(userId);
  }

  public UserTokenDTO updateUserToken(UserTokenDTO userTokenDTO) {
    Optional<UserToken> userTokenOptional =
        this.userTokenRepository.findByUserID(userTokenDTO.getUserID());

    if (userTokenOptional.isEmpty()) {
      return null;
    }
    UserToken userToken = userTokenOptional.get();

    userToken.setCodeOTP(userTokenDTO.getCodeOTP());
    userToken.setExpireAt(userTokenDTO.getExpireAt());
    userToken.setAttemptCounter(userTokenDTO.getAttemptCounter());
    userToken.setActive(userTokenDTO.isActive());

    this.userTokenRepository.save(userToken);

    return toUserDTO(userToken);
  }

  private UserTokenDTO toUserDTO(UserToken userToken) {
    if (userToken == null) {
      return null;
    }

    return UserTokenDTO.builder()
        .id(userToken.getId())
        .userID(userToken.getUserID())
        .codeOTP(userToken.getCodeOTP())
        .expireAt(userToken.getExpireAt())
        .attemptCounter(userToken.getAttemptCounter())
        .active(userToken.isActive())
        .build();
  }

  private UserToken toUser(UserTokenDTO userTokenDTO) {
    if (userTokenDTO == null) {
      return null;
    }

    return UserToken.builder()
        .codeOTP(userTokenDTO.getCodeOTP())
        .userID(userTokenDTO.getUserID())
        .expireAt(userTokenDTO.getExpireAt())
        .attemptCounter(userTokenDTO.getAttemptCounter())
        .active(userTokenDTO.isActive())
        .build();
  }
}
