package org.sitmun.plugin.core.web.rest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.util.ArrayList;
import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TaskRestResourceIntTest {

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
  private Task task;
  private Territory territory;
  @Value("${default.territory.name}")
  private String defaultTerritoryName;
  private Task taskWithAvailabilities;

  @Before
  public void init() {
    territory = territoryRepository.findOneByName(defaultTerritoryName).get();
    ArrayList<Task> cartosToCreate = new ArrayList<Task>();
    task = new Task();
    task.setName(TASK_NAME);
    cartosToCreate.add(task);
    taskWithAvailabilities = new Task();
    taskWithAvailabilities.setName("Task with availabilities");
    cartosToCreate.add(taskWithAvailabilities);
    taskRepository.save(cartosToCreate);

    ArrayList<TaskAvailability> availabilitesToCreate = new ArrayList<TaskAvailability>();
    TaskAvailability taskAvailability1 = new TaskAvailability();
    taskAvailability1.setTask(taskWithAvailabilities);
    taskAvailability1.setTerritory(territory);
    taskAvailability1.setCreatedDate(new Date());
    availabilitesToCreate.add(taskAvailability1);
    taskAvailabilityRepository.save(availabilitesToCreate);

    ArrayList<TaskParameter> paramsToCreate = new ArrayList<TaskParameter>();
    TaskParameter taskParam1 = new TaskParameter();
    taskParam1.setTask(task);
    taskParam1.setName("Task Param 1");
    paramsToCreate.add(taskParam1);
    TaskParameter taskParam2 = new TaskParameter();
    taskParam2.setTask(taskWithAvailabilities);
    taskParam2.setName("Task Param 2");
    paramsToCreate.add(taskParam2);
    taskParameterRepository.save(paramsToCreate);
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME)
  public void postTask() throws Exception {

    String uri = mvc.perform(post(TASK_URI)
                                 .contentType(MediaType.APPLICATION_JSON_UTF8).content(Util.convertObjectToJsonBytes(task)))
                     .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    mvc.perform(get(uri)
    ).andExpect(status().isOk()).andExpect(content().contentType(Util.APPLICATION_HAL_JSON_UTF8))
        .andExpect(jsonPath("$.name", equalTo(TASK_NAME)));
    taskRepository.deleteAll();
  }

  @Test
  public void getTasksAsPublic() throws Exception {
    mvc.perform(get(TASK_URI)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$._embedded.tasks", hasSize(1)));
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME)
  public void getTasksAsSitmunAdmin() throws Exception {
    mvc.perform(get(TASK_URI)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$._embedded.tasks", hasSize(2)));
  }

  /*
  @Test
  @WithMockUser(username = PUBLIC_USERNAME)
  public void getTaskParamsAsPublic() throws Exception {
      mvc.perform(get(TASK_URI+"/2/parameters")).andDo(print()).andExpect(status().isOk());
  }
  */
  @Test
  public void postTaskAsPublicUserFails() throws Exception {

    mvc.perform(post(TASK_URI)
                    // .header(HEADER_STRING, TOKEN_PREFIX + token)
                    .contentType(MediaType.APPLICATION_JSON_UTF8).content(Util.convertObjectToJsonBytes(task)))
        .andDo(print()).andExpect(status().is4xxClientError()).andReturn();
  }

  @After
  public void cleanup() {
    taskAvailabilityRepository.deleteAll();
    taskRepository.deleteAll();
    //taskParameterRepository.deleteAll();

  }

}
