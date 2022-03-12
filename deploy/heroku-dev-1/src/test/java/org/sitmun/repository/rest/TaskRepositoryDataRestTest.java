package org.sitmun.repository.rest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.task.Task;
import org.sitmun.common.domain.task.TaskRepository;
import org.sitmun.common.domain.task.availability.TaskAvailability;
import org.sitmun.common.domain.task.availability.TaskAvailabilityRepository;
import org.sitmun.common.domain.territory.Territory;
import org.sitmun.common.domain.territory.TerritoryRepository;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.test.TestUtils.asJsonString;
import static org.sitmun.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class TaskRepositoryDataRestTest {

  private static final String TASK_NAME = "Task Name";

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
        .with(user(Fixtures.admin()))
      ).andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    Assertions.assertThat(location).isNotNull();

    mvc.perform(get(location)
        .with(user(Fixtures.admin())))
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
  public void getTasksAvailableForApplication() throws Exception {
    mvc.perform(get(URIConstants.TASKS_AVAILABLE_URI, 1)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tasks", hasSize(1289)));

    mvc.perform(get(URIConstants.TASKS_AVAILABLE_URI, 2)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tasks", hasSize(446)));
  }

  @Test
  @Disabled
  public void getTasksAsPublic() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tasks", hasSize(0)));
  }

  @Test
  @Disabled
  public void getTasksAsSitmunAdmin() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI_PROJECTION_VIEW)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tasks", hasSize(1758)));
  }

  @Test
  public void getTaskFilteredByTypeAsSitmunAdmin() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI_FILTER, "type.id", "2", "10")
        .with(user(Fixtures.admin())))
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
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.roles", hasSize(39)));
  }

  @Test
  public void getPermissionsOfATask() throws Exception {
    mvc.perform(get(URIConstants.TASK_ROLE_URI, 1)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.roles", hasSize(39)));
  }

}
