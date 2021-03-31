package org.sitmun.plugin.core.repository.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.Task;
import org.sitmun.plugin.core.domain.TaskAvailability;
import org.sitmun.plugin.core.domain.TaskParameter;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.repository.TaskAvailabilityRepository;
import org.sitmun.plugin.core.repository.TaskParameterRepository;
import org.sitmun.plugin.core.repository.TaskRepository;
import org.sitmun.plugin.core.repository.TerritoryRepository;
import org.sitmun.plugin.core.repository.handlers.BasicTaskEventHandler;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;

import java.net.URI;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.sitmun.plugin.core.test.TestUtils.asJsonString;
import static org.sitmun.plugin.core.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class TaskRepositoryDataRestTest {

  private static final String TASK_NAME = "Task Name";

  @Autowired
  TaskParameterRepository taskParameterRepository;
  @Autowired
  BasicTaskEventHandler basicTaskEventHandler;
  @Autowired
  TaskRepository taskRepository;
  @Autowired
  TaskAvailabilityRepository taskAvailabilityRepository;
  @Autowired
  TerritoryRepository territoryRepository;
  @Autowired
  private MockMvc mvc;

  private Territory territory;
  private Task task;
  private ArrayList<Task> tasks;
  private ArrayList<TaskAvailability> availabilities;

  @BeforeEach
  public void init() {

    withMockSitmunAdmin(() -> {

      territory = Territory.builder()
        .name("Territorio 1")
        .code("")
        .blocked(false)
        .build();
      territoryRepository.save(territory);
      tasks = new ArrayList<>();

      Map<String, Object> parameters = new HashMap<>();
      parameters.put("string", "value");
      parameters.put("real", 1.0);
      parameters.put("integer", 1);
      parameters.put("array", new String[]{"one", "two", "three"});

      task = Task.builder()
        .name(TASK_NAME)
        .properties(parameters)
        .build();
      tasks.add(task);
      Task taskWithAvailabilities = new Task();
      taskWithAvailabilities.setName("Task with availabilities");
      tasks.add(taskWithAvailabilities);
      taskRepository.saveAll(tasks);

      availabilities = new ArrayList<>();
      TaskAvailability taskAvailability1 = new TaskAvailability();
      taskAvailability1.setTask(taskWithAvailabilities);
      taskAvailability1.setTerritory(territory);
      taskAvailability1.setCreatedDate(new Date());
      availabilities.add(taskAvailability1);
      taskAvailabilityRepository.saveAll(availabilities);
    });
  }

  @AfterEach
  public void cleanup() {
    withMockSitmunAdmin(() -> {
      taskAvailabilityRepository.deleteAll(availabilities);
      taskRepository.deleteAll(tasks);
      territoryRepository.delete(territory);
    });
  }

  @Test
  public void postTask() throws Exception {
    String location = mvc.perform(post(URIConstants.TASKS_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(asJsonString(task))
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertThat(location).isNotNull();

    mvc.perform(get(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andDo(print())
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$.name", equalTo(TASK_NAME)))
      .andExpect(jsonPath("$.properties.string", equalTo("value")));

    withMockSitmunAdmin(() -> {
      String[] paths = URI.create(location).getPath().split("/");
      Integer id = Integer.parseInt(paths[paths.length - 1]);
      taskRepository.findById(id).ifPresent((it) -> tasks.add(it));
    });
  }

  @Test
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

    String updatedTAsk = "{" +
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
      .content(updatedTAsk)
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

      basicTaskEventHandler.syncTaskProperties();
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

  @Test
  @Disabled
  public void getTasksAsPublic() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tasks", hasSize(0)));
  }

  @Test
  public void getTasksAsSitmunAdmin() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tasks", hasSize(1758)));
  }

  @Test
  public void getTaskFilteredByTypeAsSitmunAdmin() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI_FILTER, "type.id", "2", "10")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tasks", hasSize(10)));
  }

  @Test
  public void postTaskAsPublicUserFails() throws Exception {
    mvc.perform(post(URIConstants.TASKS_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(asJsonString(task))
    ).andExpect(status().is4xxClientError()).andReturn();
  }

  @Test
  public void getRolesOfATask() throws Exception {
    mvc.perform(get(URIConstants.TASK_ROLE_URI, 1)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.roles", hasSize(39)));
  }

  @Test
  public void getPermissionsOfATask() throws Exception {
    mvc.perform(get(URIConstants.TASK_ROLE_URI, 1)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.roles", hasSize(39)));
  }

}
