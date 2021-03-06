package org.sitmun.plugin.core.repository.handlers.stm2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.Task;
import org.sitmun.plugin.core.domain.TaskParameter;
import org.sitmun.plugin.core.repository.TaskParameterRepository;
import org.sitmun.plugin.core.repository.TaskRepository;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.sitmun.plugin.core.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ExtractFMETaskEventHandler Test")
public class ExtractFMETaskEventHandlerTest {

  @Autowired
  TaskParameterRepository taskParameterRepository;

  @Autowired
  TaskRepository taskRepository;

  @Autowired
  ExtractFMETaskEventHandler extractFMETaskEventHandler;

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("POST: Update legacy task parameter table")
  public void postExtractFMETask() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/10\"," +
      "\"properties\": {" +
      "\"layers\": [\"1\",\"2\"]}" +
      "}";

    String location = mvc.perform(post(URIConstants.TASKS_URI)
      .content(newTask)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);
    withMockSitmunAdmin(() -> {
      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Task task = taskRepository.findById(taskId).orElseGet(() -> fail("Task not found"));
      List<TaskParameter> list = new ArrayList<>();
      taskParameterRepository.findAllByTask(task).forEach(list::add);
      assertEquals(1, list.size());
      assertEquals("CAPAS", list.get(0).getName());
      assertEquals("FME", list.get(0).getType());
      assertEquals("1,2", list.get(0).getValue());
    });
    // Cleanup
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());

  }

  @Test
  @DisplayName("PUT: Update legacy task parameter table")
  public void updateDownloadTask() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/10\"," +
      "\"properties\": {" +
      "\"layers\": [\"1\",\"2\"]}" +
      "}";

    String location = mvc.perform(post(URIConstants.TASKS_URI)
      .content(newTask)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    String updatedTAsk = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/10\"," +
      "\"properties\": {" +
      "\"layers\": [\"3\",\"4\"]}" +
      "}";

    mvc.perform(put(location)
      .content(updatedTAsk)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk());

    withMockSitmunAdmin(() -> {
      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Task task = taskRepository.findById(taskId).orElseGet(() -> fail("Task not found"));
      List<TaskParameter> list = new ArrayList<>();
      taskParameterRepository.findAllByTask(task).forEach(list::add);
      assertEquals(1, list.size());
      assertEquals("CAPAS", list.get(0).getName());
      assertEquals("FME", list.get(0).getType());
      assertEquals("3,4", list.get(0).getValue());
    });

    // Cleanup
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());

  }

  @Test
  @DisplayName("Update from legacy task parameter table")
  public void syncExtractFMETasks() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/10\"" +
      "}";

    String location = mvc.perform(post(URIConstants.TASKS_URI)
      .content(newTask)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);
    withMockSitmunAdmin(() -> {
      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Optional<Task> opTask = taskRepository.findById(taskId);
      assertTrue(opTask.isPresent());

      TaskParameter taskParameter = TaskParameter.builder()
        .task(opTask.get())
        .type("FME")
        .name("CAPAS")
        .value("6,7")
        .order(1).build();
      taskParameterRepository.save(taskParameter);

      extractFMETaskEventHandler.synchronize();
    });

    mvc.perform(get(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.properties.layers").isArray())
      .andExpect(jsonPath("$.properties.layers", hasSize(2)))
      .andExpect(jsonPath("$.properties.layers", hasItem("6")))
      .andExpect(jsonPath("$.properties.layers", hasItem("7")));

    // Cleanup
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());

  }
}
