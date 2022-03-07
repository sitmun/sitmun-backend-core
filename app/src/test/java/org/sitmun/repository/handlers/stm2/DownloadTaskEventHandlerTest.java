package org.sitmun.repository.handlers.stm2;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.task.Task;
import org.sitmun.common.domain.task.TaskRepository;
import org.sitmun.legacy.domain.task.download.DownloadTask;
import org.sitmun.legacy.domain.task.download.DownloadTaskEventHandler;
import org.sitmun.legacy.domain.task.download.DownloadTaskRepository;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class DownloadTaskEventHandlerTest {

  @Autowired
  DownloadTaskRepository downloadTaskRepository;

  @Autowired
  TaskRepository taskRepository;

  @Autowired
  DownloadTaskEventHandler downloadTaskEventHandler;

  @Autowired
  private MockMvc mvc;

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void postDownloadTask() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/2\"," +
      "\"properties\": {" +
      "\"format\": \"SHP\"," +
      "\"path\": \"http://www.example.com/\"," +
      "\"scope\": \"U\"}" +
      "}";

    String location = mvc.perform(post(URIConstants.TASKS_URI)
        .content(newTask)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);
    withMockSitmunAdmin(() -> {
      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Optional<DownloadTask> opTask = downloadTaskRepository.findById(taskId);
      assertTrue(opTask.isPresent());

      assertEquals("SHP", opTask.get().getFormat());
      assertEquals("http://www.example.com/", opTask.get().getPath());
      assertEquals("U", opTask.get().getScope());
    });
    // Cleanup
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().isNoContent());

  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void updateDownloadTask() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/2\"," +
      "\"properties\": {" +
      "\"format\": \"SHP\"," +
      "\"path\": \"http://www.example.com/\"," +
      "\"scope\": \"U\"}" +
      "}";

    String location = mvc.perform(post(URIConstants.TASKS_URI)
        .content(newTask)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    String updatedTAsk = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/2\"," +
      "\"properties\": {" +
      "\"format\": \"ZIP\"," +
      "\"path\": \"http://www.example2.com/\"," +
      "\"scope\": \"U\"}" +
      "}";

    mvc.perform(put(location)
        .content(updatedTAsk)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(status().isOk());

    withMockSitmunAdmin(() -> {
      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Optional<DownloadTask> opTask = downloadTaskRepository.findById(taskId);
      assertTrue(opTask.isPresent());

      assertEquals("ZIP", opTask.get().getFormat());
      assertEquals("http://www.example2.com/", opTask.get().getPath());
      assertEquals("U", opTask.get().getScope());
    });

    // Cleanup
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().isNoContent());

  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void syncDownloadTasks() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/2\"" +
      "}";

    String location = mvc.perform(post(URIConstants.TASKS_URI)
        .content(newTask)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);
    withMockSitmunAdmin(() -> {
      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Optional<Task> opTask = taskRepository.findById(taskId);
      assertTrue(opTask.isPresent());

      DownloadTask downloadTask = DownloadTask.downloadBuilder()
        .id(taskId)
        .format("DXF")
        .path("http://www.example3.com/")
        .scope("U").build();
      downloadTaskRepository.save(downloadTask);

      downloadTaskEventHandler.synchronize();
    });

    mvc.perform(get(location)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.properties.format").value("DXF"))
      .andExpect(jsonPath("$.properties.path").value("http://www.example3.com/"))
      .andExpect(jsonPath("$.properties.scope").value("U"));

    // Cleanup
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().isNoContent());

  }
}
