package org.sitmun.plugin.core.annotation;

import org.junit.Test;
import org.springframework.boot.SpringApplication;

public class SitmunApplicationTests {

  @Test
  public void runStimunApplication() {
    SpringApplication.run(Application.class);
  }

  @SitmunApplication
  public static class Application {

  }
}
