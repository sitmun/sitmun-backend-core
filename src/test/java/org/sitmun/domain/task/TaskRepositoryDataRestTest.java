package org.sitmun.domain.task;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.sitmun.domain.task.availability.TaskAvailability;
import org.sitmun.domain.task.availability.TaskAvailabilityRepository;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.task.type.TaskTypeRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.test.BaseTest;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.test.TestUtils.asJsonString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Task Repository Data REST Test")
class TaskRepositoryDataRestTest extends BaseTest {

  private static final String TASK_NAME = "Task Name";

  @Autowired
  TaskTypeRepository taskTypeRepository;

  @Autowired
  TaskRepository taskRepository;

  @Autowired
  TaskAvailabilityRepository taskAvailabilityRepository;

  @Autowired
  TerritoryRepository territoryRepository;

  private Territory territory;
  private Task task;
  private ArrayList<Task> tasks;
  private ArrayList<TaskAvailability> availabilities;

  @BeforeEach
  @WithMockUser(roles = "ADMIN")
  void init() {

    TaskType basic = taskTypeRepository.findById(1).orElseThrow();

    territory = Territory.builder()
      .name("Territorio 1")
      .code("")
      .blocked(false)
      .build();
    territoryRepository.save(territory);
    tasks = new ArrayList<>();

    Map<String, Object> container = fixtureProperties();

    task = Task.builder()
      .name(TASK_NAME)
      .properties(container)
      .build();
    tasks.add(task);
    Task taskWithAvailabilities = new Task();
    taskWithAvailabilities.setName("Task with availabilities");
    tasks.add(taskWithAvailabilities);
    taskRepository.saveAll(tasks);

    assertNotNull(task.getId());
    task.setType(basic);
    taskRepository.save(task);
    assertEquals(TASK_NAME, task.getName());

    availabilities = new ArrayList<>();
    TaskAvailability taskAvailability1 = new TaskAvailability();
    taskAvailability1.setTask(taskWithAvailabilities);
    taskAvailability1.setTerritory(territory);
    taskAvailability1.setCreatedDate(new Date());
    availabilities.add(taskAvailability1);
    taskAvailabilityRepository.saveAll(availabilities);
  }

  @NotNull
  private static Map<String, Object> fixtureProperties() {
    Map<String, Object> string = new HashMap<>();
    string.put("name", "string");
    string.put("type", "string");
    string.put("value", "value");

    Map<String, Object> number = new HashMap<>();
    number.put("name", "number");
    number.put("type", "number");
    number.put("value", "1.0");

    Map<String, Object> integer = new HashMap<>();
    integer.put("name", "number");
    integer.put("type", "number");
    integer.put("value", "1");

    Map<String, Object> array = new HashMap<>();
    array.put("name", "array");
    array.put("type", "array");
    array.put("value", "[\"one\", \"two\", \"three\"]");

    Map<String, Object> object = new HashMap<>();
    object.put("name", "object");
    object.put("type", "object");
    object.put("value", "{\"one\": \"two\", \"three\": 3}");

    Map<String, Object> bool = new HashMap<>();
    bool.put("name", "boolean");
    bool.put("type", "boolean");
    bool.put("value", "true");

    Map<String, Object> none = new HashMap<>();
    none.put("name", "null");
    none.put("type", "null");
    none.put("value", null);

    List<Map<String, Object>> list = new ArrayList<>();
    list.add(string);
    list.add(number);
    list.add(integer);
    list.add(array);
    list.add(object);
    list.add(bool);
    list.add(none);

    Map<String, Object> container = new HashMap<>();
    container.put("parameters", list);
    return container;
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() {
    taskAvailabilityRepository.deleteAll(availabilities);
    taskRepository.deleteAll(tasks);
    territoryRepository.delete(territory);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Create a new task")
  @Transactional
  void postTask() throws Exception {
    Task task = Task.builder()
      .name(TASK_NAME)
      .properties(fixtureProperties())
      .build();
    
    String location = mvc.perform(post(URIConstants.TASKS_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(task))
      )
      .andDo(print())
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    Assertions.assertThat(location).isNotNull();

    mvc.perform(get(location))
      .andDo(print())
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$.name", equalTo(TASK_NAME)))
      .andExpect(jsonPath("$.properties.parameters[0].name", equalTo("string")));

      String[] paths = URI.create(location).getPath().split("/");
      Integer id = Integer.parseInt(paths[paths.length - 1]);
      taskRepository.findById(id).ifPresent((it) -> tasks.add(it));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Get tasks per application")
  void getTasksAvailableForApplication() throws Exception {
    mvc.perform(get(URIConstants.TASKS_AVAILABLE_URI, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tasks", hasSize(35)));

    mvc.perform(get(URIConstants.TASKS_AVAILABLE_URI, 2))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tasks", hasSize(35)));
  }

  @Test
  @DisplayName("This endpoint is disabled for anonymous access")
  void getTasksAsPublic() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI))
      .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("This endpoint is enabled for ROLE_ADMIN")
  void getTasksAsSitmunAdmin() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI_PROJECTION_VIEW))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tasks", hasSize(37)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Access enabled to the cartography of a task")
  @Disabled("Requires additional test data")
  void getCartographyView() throws Exception {
    mvc.perform(get(URIConstants.TASK_PROJECTION_CARTOGRAPHY_VIEW, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", is(88)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Tasks can be filtered")
  @Disabled("Requires additional test data")
  void getTaskFilteredByTypeAsSitmunAdmin() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI_FILTER, "type.id", "2", "10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tasks", hasSize(10)));
  }

  @Test
  @DisplayName("This endpoint is disabled for anonymous creation")
  @Transactional
  void postTaskAsPublicUserFails() throws Exception {
    mvc.perform(post(URIConstants.TASKS_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(asJsonString(task))
    ).andExpect(status().is4xxClientError()).andReturn();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Access enabled to the roles of a task")
  void getRolesOfATask() throws Exception {
    mvc.perform(get(URIConstants.TASK_ROLE_URI, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.roles", hasSize(1)));
  }

}
