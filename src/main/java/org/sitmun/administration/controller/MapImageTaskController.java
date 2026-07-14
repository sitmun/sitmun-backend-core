package org.sitmun.administration.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sitmun.administration.controller.dto.MapImageRenderRequestDto;
import org.sitmun.administration.service.mapimage.MapImageTaskExecutionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/map-image")
@RequiredArgsConstructor
public class MapImageTaskController {

  private final MapImageTaskExecutionService mapImageTaskExecutionService;

  @PostMapping(value = "/render", produces = MediaType.IMAGE_PNG_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<byte[]> render(@RequestBody @Valid MapImageRenderRequestDto requestDto) {
    byte[] content = mapImageTaskExecutionService.renderMapImage(requestDto);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
        .body(content);
  }
}
