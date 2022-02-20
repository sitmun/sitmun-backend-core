package org.sitmun.plugin.core.repository.handlers.stm2;

import org.junit.jupiter.api.Disabled;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.sitmun.plugin.core.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class BasicTaskEventHandlerTest {

  @Autowired
  TaskParameterRepository taskParameterRepository;

  @Autowired
  TaskRepository taskRepository;

  @Autowired
  BasicTaskEventHandler basicTaskEventHandler;

  @Autowired
  private MockMvc mvc;

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void postBasicTaskWithParameters() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/1\"," +
      "\"properties\": {" +
      "\"parameters\": [{" +
      "\"name\": \"title 1\"," +
      "\"value\": \"A title 1\"," +
      "\"type\": \"ANY\"," +
      "\"order\": 1" +
      "},{" +
      "\"name\": \"title 2\"," +
      "\"value\": \"A title 2\"," +
      "\"type\": \"ANY\"," +
      "\"order\": 2" +
      "}]" +
      "}" +
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
      List<TaskParameter> parameters = new ArrayList<>();
      taskParameterRepository.findAllByTask(opTask.get()).forEach(parameters::add);
      assertEquals(2, parameters.size());

      assertEquals("title 1", parameters.get(0).getName());
      assertEquals("A title 1", parameters.get(0).getValue());
      assertEquals("ANY", parameters.get(0).getType());
      assertEquals(1, parameters.get(0).getOrder());

      assertEquals("title 2", parameters.get(1).getName());
      assertEquals("A title 2", parameters.get(1).getValue());
      assertEquals("ANY", parameters.get(1).getType());
      assertEquals(2, parameters.get(1).getOrder());
    });
    // Cleanup
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());

  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void postBasicTaskWithNullValues() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/1\"," +
      "\"properties\": {" +
      "\"parameters\": [{" +
      "\"name\": \"title 1\"," +
      "\"value\": \"A title 1\"," +
      "\"type\": null," +
      "\"order\": 1" +
      "}]" +
      "}" +
      "}";

    String location = mvc.perform(post(URIConstants.TASKS_URI)
      .content(newTask)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    mvc.perform(get(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(jsonPath("$.properties.parameters[?(@.type)]").exists());

    // Cleanup
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());

  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void updateBasicTaskWithParameters() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/1\"," +
      "\"properties\": {" +
      "\"parameters\": [{" +
      "\"name\": \"title 1\"," +
      "\"value\": \"A title 1\"," +
      "\"type\": \"ANY\"," +
      "\"order\": 1" +
      "},{" +
      "\"name\": \"title 2\"," +
      "\"value\": \"A title 2\"," +
      "\"type\": \"ANY\"," +
      "\"order\": 2" +
      "}]" +
      "}" +
      "}";

    String location = mvc.perform(post(URIConstants.TASKS_URI)
      .content(newTask)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    String updatedTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/1\"," +
      "\"properties\": {" +
      "\"parameters\": [{" +
      "\"name\": \"title 3\"," +
      "\"value\": \"A title 3\"," +
      "\"type\": \"ANY\"," +
      "\"order\": 3" +
      "}]" +
      "}" +
      "}";

    mvc.perform(put(location)
      .content(updatedTask)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk());

    withMockSitmunAdmin(() -> {
      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Optional<Task> opTask = taskRepository.findById(taskId);
      assertTrue(opTask.isPresent());
      List<TaskParameter> parameters = new ArrayList<>();
      taskParameterRepository.findAllByTask(opTask.get()).forEach(parameters::add);
      assertEquals(1, parameters.size());

      assertEquals("title 3", parameters.get(0).getName());
      assertEquals("A title 3", parameters.get(0).getValue());
      assertEquals("ANY", parameters.get(0).getType());
      assertEquals(3, parameters.get(0).getOrder());
    });

    // Cleanup
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());

  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void syncBasicTaskWithParameters() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/1\"" +
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
      Task task = opTask.get();

      TaskParameter param1 = TaskParameter.builder().name("title 1").value("A title 1").value("ANY").order(1).task(task).build();
      taskParameterRepository.save(param1);
      TaskParameter param2 = TaskParameter.builder().name("title 2").value("A title 2").value("ANY").order(2).task(task).build();
      taskParameterRepository.save(param2);

      basicTaskEventHandler.synchronize();
    });

    mvc.perform(get(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.properties.parameters[?(@.name=='title 1')]").exists())
      .andExpect(jsonPath("$.properties.parameters[?(@.name=='title 2')]").exists());

    // Cleanup
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());

  }
}
