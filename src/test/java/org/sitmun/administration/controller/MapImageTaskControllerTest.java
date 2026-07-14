package org.sitmun.administration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.administration.service.mapimage.MapImageTaskExecutionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("Map image task controller")
class MapImageTaskControllerTest {

  @Mock private MapImageTaskExecutionService mapImageTaskExecutionService;

  @Test
  @DisplayName("POST /api/tasks/map-image/render returns png bytes")
  void renderReturnsPngBytes() throws Exception {
    when(mapImageTaskExecutionService.renderMapImage(any())).thenReturn(new byte[] {1, 2, 3});

    MockMvc mvc = MockMvcBuilders.standaloneSetup(new MapImageTaskController(mapImageTaskExecutionService)).build();

    mvc.perform(post("/api/tasks/map-image/render")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"taskId":33,"bbox":[1,2,3,4],"width":256,"height":256,"format":"png","srs":"EPSG:4326"}
                """))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(new byte[] {1, 2, 3}));
  }

  @Test
  @DisplayName("POST /api/tasks/map-image/render rejects invalid body")
  void renderRejectsInvalidBody() throws Exception {
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new MapImageTaskController(mapImageTaskExecutionService)).build();

    mvc.perform(post("/api/tasks/map-image/render")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"bbox":[1,2,3]}
                """))
        .andExpect(status().isBadRequest());
  }
}
