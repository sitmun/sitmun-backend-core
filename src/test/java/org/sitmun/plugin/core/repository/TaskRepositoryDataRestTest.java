package org.sitmun.plugin.core.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.security.SecurityConstants.HEADER_STRING;
import static org.sitmun.plugin.core.security.SecurityConstants.TOKEN_PREFIX;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.sitmun.plugin.core.test.TestUtils.asJsonString;
import static org.sitmun.plugin.core.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.sitmun.plugin.core.domain.Task;
import org.sitmun.plugin.core.domain.TaskAvailability;
import org.sitmun.plugin.core.domain.TaskParameter;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.security.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TaskRepositoryDataRestTest {

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

  private String token;
  private Territory territory;
  private Task task;
  private ArrayList<Task> tasks;
  private ArrayList<TaskAvailability> availabilities;
  private ArrayList<TaskParameter> parameters;

  @Before
  public void init() {

    withMockSitmunAdmin(() -> {

      token = tokenProvider.createToken(SITMUN_ADMIN_USERNAME);
      territory = Territory.builder()
          .setName("Territorio 1")
          .setCode("")
          .setBlocked(false)
          .build();
      territoryRepository.save(territory);
      tasks = new ArrayList<>();
      task = new Task();
      task.setName(TASK_NAME);
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

      parameters = new ArrayList<>();
      TaskParameter taskParam1 = new TaskParameter();
      taskParam1.setTask(task);
      taskParam1.setName("Task Param 1");
      parameters.add(taskParam1);
      TaskParameter taskParam2 = new TaskParameter();
      taskParam2.setTask(taskWithAvailabilities);
      taskParam2.setName("Task Param 2");
      parameters.add(taskParam2);
      taskParameterRepository.saveAll(parameters);
    });
  }

  @After
  public void cleanup() {
    withMockSitmunAdmin(() -> {
      taskParameterRepository.deleteAll(parameters);
      taskAvailabilityRepository.deleteAll(availabilities);
      taskRepository.deleteAll(tasks);
      territoryRepository.delete(territory);
    });
  }

  @Test
  public void postTask() throws Exception {
    String location = mvc.perform(post(TASK_URI)
        .header(HEADER_STRING, TOKEN_PREFIX + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(task))
    ).andExpect(status().isCreated())
        .andReturn().getResponse().getHeader("Location");

    assertThat(location).isNotNull();

    mvc.perform(get(location)
        .header(HEADER_STRING, TOKEN_PREFIX + token))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.name", equalTo(TASK_NAME)));

    withMockSitmunAdmin(() -> {
      String[] paths = URI.create(location).getPath().split("/");
      Integer id = Integer.valueOf(Integer.parseInt(paths[paths.length - 1]));
      taskRepository.findById(id).ifPresent((it) -> tasks.add(it));
    });
  }

  @Ignore
  public void getTasksAsPublic() throws Exception {
    mvc.perform(get(TASK_URI))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tasks", hasSize(0)));
  }

  @Test
  public void getTasksAsSitmunAdmin() throws Exception {
    mvc.perform(get(TASK_URI)
        .header(HEADER_STRING, TOKEN_PREFIX + token))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tasks", hasSize(115)));
  }

  @Test
  @WithMockUser(username = PUBLIC_USERNAME)
  public void getTaskParamsAsPublic() throws Exception {
    mvc.perform(get(TASK_URI + "/" + task.getId() + "/parameters"))
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

  @TestConfiguration
  static class ContextConfiguration {
    @Bean
    public Validator validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    RepositoryRestConfigurer repositoryRestConfigurer() {
      return new RepositoryRestConfig(validator());
    }
  }

}
