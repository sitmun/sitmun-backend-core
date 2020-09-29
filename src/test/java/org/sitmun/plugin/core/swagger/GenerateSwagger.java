package org.sitmun.plugin.core.swagger;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Ignore
public class GenerateSwagger {

  TestRestTemplate restTemplate = new TestRestTemplate();
  @LocalServerPort
  private int port;

  /**
   * There is a bug in SpringFox 3.0.0 that breaks the creation of documentation
   * when a parent-child relationship is present in some cases.
   * see https://github.com/springfox/springfox/issues/3469
   * This is planned to be fixed in SpringFox 3.0.1
   * see https://github.com/springfox/springfox/milestone/45
   * FIXME Upgrade to SpringFox 3.0.1
   */
  @Test
  public void generateSwagger() throws IOException {
    String response =
        restTemplate.getForObject("http://localhost:" + port + "/v2/api-docs", String.class);
    Files.asCharSink(new File("swagger.json"), Charset.defaultCharset()).write(response);
  }
}