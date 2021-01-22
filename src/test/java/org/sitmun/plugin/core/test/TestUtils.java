package org.sitmun.plugin.core.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;

import java.util.Collections;

import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_PASSWORD;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;

public class TestUtils {
  public static String asJsonString(Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void withMockSitmunAdmin(Runnable func) {
    UsernamePasswordAuthenticationToken authReq =
      new UsernamePasswordAuthenticationToken(SITMUN_ADMIN_USERNAME,
        SITMUN_ADMIN_PASSWORD,
        Collections.emptyList());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authReq);
    TestSecurityContextHolder.setContext(context);
    func.run();
    TestSecurityContextHolder.clearContext();
  }
}
