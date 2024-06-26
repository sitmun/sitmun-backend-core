package org.sitmun.domain.task.fme;

import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ExtractFMETaskEventHandler Test")
class ExtractFMETaskEventHandlerTest {

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
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void postExtractFMETask() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/10\"," +
      "\"properties\": {" +
      "\"layers\": [\"1\",\"2\"]}" +
      "}";

    String location = mvc.perform(MockMvcRequestBuilders.post(URIConstants.TASKS_URI)
        .content(newTask)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(MockMvcResultMatchers.status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    Assertions.assertNotNull(location);
      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Task task = taskRepository.findById(taskId).orElseGet(() -> Assertions.fail("Task not found"));
      List<TaskParameter> list = new ArrayList<>();
      taskParameterRepository.findAllByTask(task).forEach(list::add);
      Assertions.assertEquals(1, list.size());
      assertEquals("CAPAS", list.get(0).getName());
      assertEquals("FME", list.get(0).getType());
      assertEquals("1,2", list.get(0).getValue());
    // Cleanup
    mvc.perform(MockMvcRequestBuilders.delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(MockMvcResultMatchers.status().isNoContent());

  }

  @Test
  @DisplayName("PUT: Update legacy task parameter table")
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void updateDownloadTask() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/10\"," +
      "\"properties\": {" +
      "\"layers\": [\"1\",\"2\"]}" +
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
      "\"type\": \"http://localhost/api/task-types/10\"," +
      "\"properties\": {" +
      "\"layers\": [\"3\",\"4\"]}" +
      "}";

    mvc.perform(MockMvcRequestBuilders.put(location)
        .content(updatedTAsk)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(MockMvcResultMatchers.status().isOk());

      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Task task = taskRepository.findById(taskId).orElseGet(() -> Assertions.fail("Task not found"));
      List<TaskParameter> list = new ArrayList<>();
      taskParameterRepository.findAllByTask(task).forEach(list::add);
      Assertions.assertEquals(1, list.size());
      assertEquals("CAPAS", list.get(0).getName());
      assertEquals("FME", list.get(0).getType());
      assertEquals("3,4", list.get(0).getValue());

    // Cleanup
    mvc.perform(MockMvcRequestBuilders.delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(MockMvcResultMatchers.status().isNoContent());

  }

  @Test
  @DisplayName("Update from legacy task parameter table")
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void syncExtractFMETasks() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/10\"" +
      "}";

    String location = mvc.perform(MockMvcRequestBuilders.post(URIConstants.TASKS_URI)
        .content(newTask)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(MockMvcResultMatchers.status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    Assertions.assertNotNull(location);

    String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Optional<Task> opTask = taskRepository.findById(taskId);
      Assertions.assertTrue(opTask.isPresent());

      TaskParameter taskParameter = TaskParameter.builder()
        .task(opTask.get())
        .type("FME")
        .name("CAPAS")
        .value("6,7")
        .order(1).build();
      taskParameterRepository.save(taskParameter);

      extractFMETaskEventHandler.synchronize();

    mvc.perform(MockMvcRequestBuilders.get(location)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andExpect(MockMvcResultMatchers.jsonPath("$.properties.layers").isArray())
      .andExpect(MockMvcResultMatchers.jsonPath("$.properties.layers", IsCollectionWithSize.hasSize(2)))
      .andExpect(MockMvcResultMatchers.jsonPath("$.properties.layers", IsIterableContaining.hasItem("6")))
      .andExpect(MockMvcResultMatchers.jsonPath("$.properties.layers", IsIterableContaining.hasItem("7")));

    // Cleanup
    mvc.perform(MockMvcRequestBuilders.delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(MockMvcResultMatchers.status().isNoContent());

  }
}
