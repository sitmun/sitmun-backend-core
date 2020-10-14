package org.sitmun.plugin.core.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class TestUtils {
  public static String asJsonString(Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void asSystem(Runnable func) {
    List<SimpleGrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority(AuthoritiesConstants.ROLE_ADMIN));

    UsernamePasswordAuthenticationToken authReq =
        new UsernamePasswordAuthenticationToken("system", "system",
            authorities);

    SecurityContext sc = SecurityContextHolder.getContext();
    sc.setAuthentication(authReq);
    func.run();
    sc.setAuthentication(null);
    SecurityContextHolder.clearContext();
  }

  public static void asAdmin(Runnable func) {
    List<SimpleGrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority(AuthoritiesConstants.ROLE_ADMIN));

    UsernamePasswordAuthenticationToken authReq =
        new UsernamePasswordAuthenticationToken("admin", "admin",
            authorities);

    SecurityContext sc = SecurityContextHolder.getContext();
    sc.setAuthentication(authReq);
    func.run();
    sc.setAuthentication(null);
    SecurityContextHolder.clearContext();
  }
}
