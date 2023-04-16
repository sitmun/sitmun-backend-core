package org.sitmun.domain.task;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.Application;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc

class TaskControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private String saved31507;

  @BeforeEach
  void saveValue() throws Exception {
    saved31507 = mockMvc.perform(get(URIConstants.TASK_URI, 31507)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
  }

  @AfterEach
  void restoreValue() throws Exception {
    if (saved31507 != null) {
      mockMvc.perform(put(URIConstants.TASK_URI, 31507).content(saved31507)
          .with(user(Fixtures.admin())))
        .andExpect(status().isOk());
    }
  }

  @Test
  @DisplayName("Ensure legacy query task is properly handled")
  void ensureLegacyQueryTaskIsHandled() throws Exception {

    // From
    String mustPassUpdate = "{\"properties\":{\"command\":\"Valor\",\"scope\":\"SQL\",\"parameters\":[{\"key\":\"${VALUE}\",\"label\":\"Etiqueta\",\"type\":\"A\",\"value\":\"Valor\",\"order\":231}]},\"type\":\"http://localhost:8080/api/task-types/5\",\"name\":\"02. Municipis connectats\",\"group\":\"http://localhost:8080/api/task-groups/50\",\"cartography\":\"http://localhost:8080/api/cartographies/86\",\"command\":\"Valor\",\"proxyUrl\":\"http://localhost:8080/api/\",\"rootUrl\":\"http://localhost:8080/api/\",\"typeName\":\"consulta\",\"id\":31507,\"order\":null,\"typeId\":5,\"cartographyId\":86,\"serviceName\":null,\"createdDate\":\"2011-04-29T11:43:45.000+00:00\",\"cartographyName\":\"CRE5M - Límits\",\"groupName\":\"INFORMACIÓ DE CONTROL (indicadors-llistats)\",\"serviceId\":null,\"uiId\":2,\"_links\":{\"self\":{\"href\":\"http://localhost:8080/api/tasks/31507\"},\"task\":{\"href\":\"http://localhost:8080/api/tasks/31507\",\"templated\":true},\"cartography\":{\"href\":\"http://localhost:8080/api/tasks/31507/cartography\",\"templated\":true},\"service\":{\"href\":\"http://localhost:8080/api/tasks/31507/service\"},\"relatedBy\":{\"href\":\"http://localhost:8080/api/tasks/31507/relatedBy\"},\"roles\":{\"href\":\"http://localhost:8080/api/tasks/31507/roles\"},\"type\":{\"href\":\"http://localhost:8080/api/tasks/31507/type\",\"templated\":true},\"connection\":{\"href\":\"http://localhost:8080/api/tasks/31507/connection\"},\"group\":{\"href\":\"http://localhost:8080/api/tasks/31507/group\"},\"relations\":{\"href\":\"http://localhost:8080/api/tasks/31507/relations\"},\"ui\":{\"href\":\"http://localhost:8080/api/tasks/31507/ui\"},\"availabilities\":{\"href\":\"http://localhost:8080/api/tasks/31507/availabilities\",\"templated\":true}}}";

    mockMvc.perform(put(URIConstants.TASK_URI, 31507).content(mustPassUpdate)
        .with(user(Fixtures.admin())))
      .andDo(print())
      .andExpect(status().isOk());

  }
}