package org.sitmun.domain.user_token;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@Tag(name = "token_user")
public interface UserTokenRepository extends JpaRepository<UserToken, Integer> {
  Optional<UserToken> findByCodeOTP(String codeOTP);

  Optional<UserToken> findByUserID(int userID);

  void deleteByUserID(int userID);
}
