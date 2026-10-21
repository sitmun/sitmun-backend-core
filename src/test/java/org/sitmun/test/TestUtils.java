package org.sitmun.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class TestUtils {

  private static final ObjectMapper mapper =
      JsonMapper.builder()
          .changeDefaultPropertyInclusion(
              incl ->
                  JsonInclude.Value.construct(
                      JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS))
          .build();

  public static String asJsonString(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Integer extractId(String url) {
    String[] paths = URI.create(url).getPath().split("/");
    return Integer.parseInt(paths[paths.length - 1]);
  }
}
