package org.sitmun.domain.task.download;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.UriTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sitmun.test.TestUtils.withMockSitmunAdmin;


@SpringBootTest
@AutoConfigureMockMvc

class DownloadTaskEventHandlerTest {

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
  void postDownloadTask() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/2\"," +
      "\"properties\": {" +
      "\"format\": \"SHP\"," +
      "\"path\": \"http://www.example.com/\"," +
      "\"scope\": \"U\"}" +
      "}";

    String location = mvc.perform(MockMvcRequestBuilders.post(URIConstants.TASKS_URI)
        .content(newTask)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(MockMvcResultMatchers.status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    Assertions.assertNotNull(location);
    withMockSitmunAdmin(() -> {
      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Optional<DownloadTask> opTask = downloadTaskRepository.findById(taskId);
      Assertions.assertTrue(opTask.isPresent());

      assertEquals("SHP", opTask.get().getFormat());
      assertEquals("http://www.example.com/", opTask.get().getPath());
      assertEquals("U", opTask.get().getScope());
    });
    // Cleanup
    mvc.perform(MockMvcRequestBuilders.delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(MockMvcResultMatchers.status().isNoContent());

  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void updateDownloadTask() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/2\"," +
      "\"properties\": {" +
      "\"format\": \"SHP\"," +
      "\"path\": \"http://www.example.com/\"," +
      "\"scope\": \"U\"}" +
      "}";

    String location = mvc.perform(MockMvcRequestBuilders.post(URIConstants.TASKS_URI)
        .content(newTask)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(MockMvcResultMatchers.status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    Assertions.assertNotNull(location);

    String updatedTAsk = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/2\"," +
      "\"properties\": {" +
      "\"format\": \"ZIP\"," +
      "\"path\": \"http://www.example2.com/\"," +
      "\"scope\": \"U\"}" +
      "}";

    mvc.perform(MockMvcRequestBuilders.put(location)
        .content(updatedTAsk)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(MockMvcResultMatchers.status().isOk());

    withMockSitmunAdmin(() -> {
      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Optional<DownloadTask> opTask = downloadTaskRepository.findById(taskId);
      Assertions.assertTrue(opTask.isPresent());

      assertEquals("ZIP", opTask.get().getFormat());
      assertEquals("http://www.example2.com/", opTask.get().getPath());
      assertEquals("U", opTask.get().getScope());
    });

    // Cleanup
    mvc.perform(MockMvcRequestBuilders.delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(MockMvcResultMatchers.status().isNoContent());

  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void syncDownloadTasks() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/2\"" +
      "}";

    String location = mvc.perform(MockMvcRequestBuilders.post(URIConstants.TASKS_URI)
        .content(newTask)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(MockMvcResultMatchers.status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    Assertions.assertNotNull(location);
    withMockSitmunAdmin(() -> {
      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Optional<Task> opTask = taskRepository.findById(taskId);
      Assertions.assertTrue(opTask.isPresent());

      DownloadTask downloadTask = DownloadTask.downloadBuilder()
        .id(taskId)
        .format("DXF")
        .path("http://www.example3.com/")
        .scope("U").build();
      downloadTaskRepository.save(downloadTask);

      downloadTaskEventHandler.synchronize();
    });

    mvc.perform(MockMvcRequestBuilders.get(location)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andExpect(MockMvcResultMatchers.jsonPath("$.properties.format").value("DXF"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.properties.path").value("http://www.example3.com/"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.properties.scope").value("U"));

    // Cleanup
    mvc.perform(MockMvcRequestBuilders.delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(MockMvcResultMatchers.status().isNoContent());

  }
}
