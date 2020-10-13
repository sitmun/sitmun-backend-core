package org.sitmun.plugin.core.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DocumentationGenerationTest {

  TestRestTemplate restTemplate = new TestRestTemplate();
  @LocalServerPort
  private int port;

  /**
   * There is a bug in SpringFox 3.0.0 that breaks the creation of documentation
   * when a parent-child relationship is present in some cases.
   * see https://github.com/springfox/springfox/issues/3469
   * This is planned to be fixed in SpringFox 3.0.1
   * see https://github.com/springfox/springfox/milestone/45
   * <p>
   * The current workaround is to add the annotation {@link JsonIgnore} to the parent-child
   * properties involved.
   */
  @Test
  public void generateSwagger() throws IOException {
    String response =
        restTemplate.getForObject("http://localhost:" + port + "/v2/api-docs", String.class);
    File file = FileSystems.getDefault().getPath("build", "swagger", "swagger.json").toFile();
    Files.createParentDirs(file);
    Files.asCharSink(file, Charset.defaultCharset()).write(response);
  }
}