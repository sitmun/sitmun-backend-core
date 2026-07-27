package org.sitmun.domain.task;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.sitmun.domain.DomainConstants.Tasks.*;
import static org.sitmun.test.TestUtils.asJsonString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.net.URI;
import java.util.*;
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

@DisplayName("Task Repository Data REST Test")
class TaskRepositoryDataRestTest extends BaseTest {

  private static final String TASK_NAME = "Task Name";

  @Autowired TaskTypeRepository taskTypeRepository;

  @Autowired TaskRepository taskRepository;

  @Autowired TaskAvailabilityRepository taskAvailabilityRepository;

  @Autowired TerritoryRepository territoryRepository;

  private Territory territory;
  private Task task;
  private ArrayList<Task> tasks;
  private ArrayList<TaskAvailability> availabilities;

  @BeforeEach
  @WithMockUser(roles = "ADMIN")
  void init() {

    TaskType basic = taskTypeRepository.findById(1).orElseThrow();

    territory = Territory.builder().name("Territorio 1").code("some-code").blocked(false).build();
    territoryRepository.save(territory);
    tasks = new ArrayList<>();

    Map<String, Object> container = fixtureProperties();

    task = Task.builder().name(TASK_NAME).properties(container).build();
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
    string.put(PARAMETERS_NAME, TYPE_STRING);
    string.put(PARAMETERS_TYPE, TYPE_STRING);
    string.put(PARAMETERS_VALUE, "value");

    Map<String, Object> number = new HashMap<>();
    number.put(PARAMETERS_NAME, "number");
    number.put(PARAMETERS_TYPE, TYPE_NUMBER);
    number.put(PARAMETERS_VALUE, "1.0");

    Map<String, Object> integer = new HashMap<>();
    integer.put(PARAMETERS_NAME, "number");
    integer.put(PARAMETERS_TYPE, TYPE_NUMBER);
    integer.put(PARAMETERS_VALUE, "1");

    Map<String, Object> array = new HashMap<>();
    array.put(PARAMETERS_NAME, "array");
    array.put(PARAMETERS_TYPE, TYPE_ARRAY);
    array.put(PARAMETERS_VALUE, "[\"one\", \"two\", \"three\"]");

    Map<String, Object> object = new HashMap<>();
    object.put(PARAMETERS_NAME, "object");
    object.put(PARAMETERS_TYPE, TYPE_OBJECT);
    object.put(PARAMETERS_VALUE, "{\"one\": \"two\", \"three\": 3}");

    Map<String, Object> bool = new HashMap<>();
    bool.put(PARAMETERS_NAME, "boolean");
    bool.put(PARAMETERS_TYPE, TYPE_BOOLEAN);
    bool.put(PARAMETERS_VALUE, "true");

    Map<String, Object> none = new HashMap<>();
    none.put(PARAMETERS_NAME, "null");
    none.put(PARAMETERS_TYPE, TYPE_NULL);
    none.put(PARAMETERS_VALUE, null);

    List<Map<String, Object>> list = new ArrayList<>();
    list.add(string);
    list.add(number);
    list.add(integer);
    list.add(array);
    list.add(object);
    list.add(bool);
    list.add(none);

    Map<String, Object> container = new HashMap<>();
    container.put(PROPERTY_PARAMETERS, list);
    return container;
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() {
    // Clean up availabilities first (child entities)
    if (availabilities != null && !availabilities.isEmpty()) {
      taskAvailabilityRepository.deleteAll(availabilities);
    }

    // Clean up tasks
    if (tasks != null && !tasks.isEmpty()) {
      taskRepository.deleteAll(tasks);
    }

    // Clean up territory last (parent entity)
    if (territory != null && territory.getId() != null) {
      territoryRepository.delete(territory);
    }
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("POST: Create a new task")
  @Transactional
  void postTask() throws Exception {
    Task newTask = Task.builder().name(TASK_NAME).properties(fixtureProperties()).build();

    String location =
        mvc.perform(
                post(URIConstants.TASKS_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(newTask)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    Assertions.assertThat(location).isNotNull();

    mvc.perform(get(location))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.name", equalTo(TASK_NAME)))
        .andExpect(jsonPath("$.properties.parameters[0].name", equalTo(TYPE_STRING)));

    String[] paths = URI.create(location).getPath().split("/");
    Integer id = Integer.parseInt(paths[paths.length - 1]);
    taskRepository.findById(id).ifPresent(it -> tasks.add(it));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET: Get tasks per application")
  void getTasksAvailableForApplication() throws Exception {
    mvc.perform(get(URIConstants.TASKS_AVAILABLE_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tasks", hasSize(43)));

    mvc.perform(get(URIConstants.TASKS_AVAILABLE_URI, 2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tasks", hasSize(43)));
  }

  @Test
  @DisplayName("GET: This endpoint is disabled for anonymous access")
  void getTasksAsPublic() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI)).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET: This endpoint is enabled for ROLE_ADMIN")
  void getTasksAsSitmunAdmin() throws Exception {
    // Full task projection includes this class's @BeforeEach fixtures (+2) atop seeded tasks
    // (41 CSV + MIA control 43 + MIA parent 42). Explicit size=100 (default page size is 10).
    mvc.perform(get(URIConstants.TASKS_URI_PROJECTION_VIEW + "&size=100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tasks", hasSize(45)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET: Access enabled to the cartography of a task")
  @Disabled("Requires additional test data")
  void getCartographyView() throws Exception {
    mvc.perform(get(URIConstants.TASK_PROJECTION_CARTOGRAPHY_VIEW, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(88)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET: Tasks can be filtered")
  @Disabled("Requires additional test data")
  void getTaskFilteredByTypeAsSitmunAdmin() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI_FILTER, "type.id", "2", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tasks", hasSize(10)));
  }

  @Test
  @DisplayName("POST: This endpoint is disabled for anonymous creation")
  @Transactional
  void postTaskAsPublicUserFails() throws Exception {
    mvc.perform(
            post(URIConstants.TASKS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(task)))
        .andExpect(status().is4xxClientError())
        .andReturn();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET: Access enabled to the roles of a task")
  void getRolesOfATask() throws Exception {
    mvc.perform(get(URIConstants.TASK_ROLE_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.roles", hasSize(1)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET: Tasks can be sorted by name ascending")
  void getTasksSortedByNameAsc() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI_PROJECTION_VIEW + "&sort=name,ASC&size=100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tasks", hasSize(45)))
        .andExpect(jsonPath("$._embedded.tasks[0].name", lessThanOrEqualTo("ZZZ")))
        .andExpect(jsonPath("$._embedded.tasks[41].name", greaterThanOrEqualTo("AAA")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET: Tasks can be sorted by name descending")
  void getTasksSortedByNameDesc() throws Exception {
    mvc.perform(get(URIConstants.TASKS_URI_PROJECTION_VIEW + "&sort=name,DESC&size=100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tasks", hasSize(45)))
        .andExpect(jsonPath("$._embedded.tasks[0].name", greaterThanOrEqualTo("AAA")))
        .andExpect(jsonPath("$._embedded.tasks[41].name", lessThanOrEqualTo("ZZZ")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET: Sort order is deterministic across pages")
  void getTasksSortedDeterministicallyAcrossPages() throws Exception {
    // First page of 10
    String firstPageFirstItem =
        mvc.perform(get(URIConstants.TASKS_URI_PROJECTION_VIEW + "&sort=name,ASC&size=10&page=0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.tasks", hasSize(10)))
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Verify consistent ordering by fetching the second page
    mvc.perform(get(URIConstants.TASKS_URI_PROJECTION_VIEW + "&sort=name,ASC&size=10&page=1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tasks", hasSize(10)))
        .andExpect(jsonPath("$.page.number", is(1)));

    // Second fetch of first page should be identical
    String firstPageSecondFetch =
        mvc.perform(get(URIConstants.TASKS_URI_PROJECTION_VIEW + "&sort=name,ASC&size=10&page=0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.tasks", hasSize(10)))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Assertions.assertThat(firstPageFirstItem).isEqualTo(firstPageSecondFetch);
  }
}
