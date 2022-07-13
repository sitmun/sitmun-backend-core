package org.sitmun.legacy.domain.task.basic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.task.Task;
import org.sitmun.common.domain.task.TaskRepository;
import org.sitmun.legacy.domain.task.basic.BasicTaskEventHandler;
import org.sitmun.legacy.domain.task.parameter.TaskParameter;
import org.sitmun.legacy.domain.task.parameter.TaskParameterRepository;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.UriTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sitmun.test.TestUtils.withMockSitmunAdmin;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

class BasicTaskEventHandlerTest {

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
  void postBasicTaskWithParameters() throws Exception {
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
      List<TaskParameter> parameters = new ArrayList<>();
      taskParameterRepository.findAllByTask(opTask.get()).forEach(parameters::add);
      Assertions.assertEquals(2, parameters.size());

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
    mvc.perform(MockMvcRequestBuilders.delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(MockMvcResultMatchers.status().isNoContent());

  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void postBasicTaskWithNullValues() throws Exception {
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

    String location = mvc.perform(MockMvcRequestBuilders.post(URIConstants.TASKS_URI)
        .content(newTask)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(MockMvcResultMatchers.status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    Assertions.assertNotNull(location);

    mvc.perform(MockMvcRequestBuilders.get(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(MockMvcResultMatchers.jsonPath("$.properties.parameters[?(@.type)]").exists());

    // Cleanup
    mvc.perform(MockMvcRequestBuilders.delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(MockMvcResultMatchers.status().isNoContent());

  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void updateBasicTaskWithParameters() throws Exception {
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

    String location = mvc.perform(MockMvcRequestBuilders.post(URIConstants.TASKS_URI)
        .content(newTask)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(MockMvcResultMatchers.status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    Assertions.assertNotNull(location);

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

    mvc.perform(MockMvcRequestBuilders.put(location)
        .content(updatedTask)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(MockMvcResultMatchers.status().isOk());

    withMockSitmunAdmin(() -> {
      String id = new UriTemplate("http://localhost/api/tasks/{id}").match(location).get("id");
      Integer taskId = Integer.parseInt(id);
      Optional<Task> opTask = taskRepository.findById(taskId);
      Assertions.assertTrue(opTask.isPresent());
      List<TaskParameter> parameters = new ArrayList<>();
      taskParameterRepository.findAllByTask(opTask.get()).forEach(parameters::add);
      Assertions.assertEquals(1, parameters.size());

      assertEquals("title 3", parameters.get(0).getName());
      assertEquals("A title 3", parameters.get(0).getValue());
      assertEquals("ANY", parameters.get(0).getType());
      assertEquals(3, parameters.get(0).getOrder());
    });

    // Cleanup
    mvc.perform(MockMvcRequestBuilders.delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(MockMvcResultMatchers.status().isNoContent());

  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void syncBasicTaskWithParameters() throws Exception {
    String newTask = "{" +
      "\"name\": \"A name\"," +
      "\"type\": \"http://localhost/api/task-types/1\"" +
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
      Task task = opTask.get();

      TaskParameter param1 = TaskParameter.builder().name("title 1").value("A title 1").value("ANY").order(1).task(task).build();
      taskParameterRepository.save(param1);
      TaskParameter param2 = TaskParameter.builder().name("title 2").value("A title 2").value("ANY").order(2).task(task).build();
      taskParameterRepository.save(param2);

      basicTaskEventHandler.synchronize();
    });

    mvc.perform(MockMvcRequestBuilders.get(location)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andExpect(MockMvcResultMatchers.jsonPath("$.properties.parameters[?(@.name=='title 1')]").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.properties.parameters[?(@.name=='title 2')]").exists());

    // Cleanup
    mvc.perform(MockMvcRequestBuilders.delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(MockMvcResultMatchers.status().isNoContent());

  }
}
