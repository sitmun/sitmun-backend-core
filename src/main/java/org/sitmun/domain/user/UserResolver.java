package org.sitmun.domain.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserResolver {

    @Autowired
    private UserRepository userRepository;

    public String resolveUsername(int userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse(null);
    }
}
