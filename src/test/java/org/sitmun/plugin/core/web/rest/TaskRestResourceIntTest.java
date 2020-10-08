package org.sitmun.plugin.core.web.rest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.test.TestUtils.asJsonString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.util.ArrayList;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.sitmun.plugin.core.domain.Task;
import org.sitmun.plugin.core.domain.TaskAvailability;
import org.sitmun.plugin.core.domain.TaskParameter;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.repository.TaskAvailabilityRepository;
import org.sitmun.plugin.core.repository.TaskParameterRepository;
import org.sitmun.plugin.core.repository.TaskRepository;
import org.sitmun.plugin.core.repository.TerritoryRepository;
import org.sitmun.plugin.core.security.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TaskRestResourceIntTest {

  @TestConfiguration
  static class ContextConfiguration {
    @Bean
    RepositoryRestConfigurer repositoryRestConfigurer() {
      return new RepositoryRestConfig();
    }
  }

  private static final String ADMIN_USERNAME = "admin";
  private static final String TASK_NAME = "Task Name";

  private static final String TASK_URI = "http://localhost/api/tasks";
  private static final String PUBLIC_USERNAME = "public";

  @Autowired
  TaskRepository taskRepository;
  @Autowired
  TaskAvailabilityRepository taskAvailabilityRepository;
  @Autowired
  TaskParameterRepository taskParameterRepository;
  @Autowired
  TokenProvider tokenProvider;
  @Autowired
  TerritoryRepository territoryRepository;
  @Autowired
  private MockMvc mvc;

  @Value("${default.territory.name}")
  private String defaultTerritoryName;

  private Task task;

  @Before
  public void init() {
    Territory territory = territoryRepository.findOneByName(defaultTerritoryName).get();
    ArrayList<Task> cartosToCreate = new ArrayList<>();
    task = new Task();
    task.setName(TASK_NAME);
    cartosToCreate.add(task);
    Task taskWithAvailabilities = new Task();
    taskWithAvailabilities.setName("Task with availabilities");
    cartosToCreate.add(taskWithAvailabilities);
    taskRepository.saveAll(cartosToCreate);

    ArrayList<TaskAvailability> availabilitesToCreate = new ArrayList<>();
    TaskAvailability taskAvailability1 = new TaskAvailability();
    taskAvailability1.setTask(taskWithAvailabilities);
    taskAvailability1.setTerritory(territory);
    taskAvailability1.setCreatedDate(new Date());
    availabilitesToCreate.add(taskAvailability1);
    taskAvailabilityRepository.saveAll(availabilitesToCreate);

    ArrayList<TaskParameter> paramsToCreate = new ArrayList<>();
    TaskParameter taskParam1 = new TaskParameter();
    taskParam1.setTask(task);
    taskParam1.setName("Task Param 1");
    paramsToCreate.add(taskParam1);
    TaskParameter taskParam2 = new TaskParameter();
    taskParam2.setTask(taskWithAvailabilities);
    taskParam2.setName("Task Param 2");
    paramsToCreate.add(taskParam2);
    taskParameterRepository.saveAll(paramsToCreate);
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME)
  public void postTask() throws Exception {
    String uri = mvc.perform(post(TASK_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(task))
    ).andExpect(status().isCreated())
        .andReturn().getResponse().getHeader("Location");

    mvc.perform(get(uri))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.name", equalTo(TASK_NAME)));
  }

  @Test
  public void getTasksAsPublic() throws Exception {
    mvc.perform(get(TASK_URI))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tasks", hasSize(0)));
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME)
  public void getTasksAsSitmunAdmin() throws Exception {
    mvc.perform(get(TASK_URI))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tasks", hasSize(2)));
  }

  @Test
  @WithMockUser(username = PUBLIC_USERNAME)
  public void getTaskParamsAsPublic() throws Exception {
    mvc.perform(get(TASK_URI + "/"+ task.getId() + "/parameters"))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void postTaskAsPublicUserFails() throws Exception {

    mvc.perform(post(TASK_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(task))
    ).andDo(print())
        .andExpect(status().is4xxClientError()).andReturn();
  }

}
