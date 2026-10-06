package org.sitmun.administration.controller;

import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceHealthQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RepositoryRestController
public class ServiceHealthController {

  private final ServiceHealthQuery health;
  private final PagedResourcesAssembler<Service> pages;

  public ServiceHealthController(
      ServiceHealthQuery health, PagedResourcesAssembler<Service> pages) {
    this.health = health;
    this.pages = pages;
  }

  @GetMapping(value = "/services", params = "health")
  public ResponseEntity<?> byHealth(
      @RequestParam("health") String health,
      @RequestParam(value = "q", required = false) String text,
      Pageable pageable,
      PersistentEntityResourceAssembler assembler) {
    return ResponseEntity.ok(
        pages.toModel(this.health.page(health, text, pageable), assembler::toModel));
  }

  @GetMapping(value = "/services/search/content", params = "health")
  public ResponseEntity<?> byHealthAndText(
      @RequestParam("health") String health,
      @RequestParam(value = "q", required = false) String text,
      Pageable pageable,
      PersistentEntityResourceAssembler assembler) {
    return byHealth(health, text, pageable, assembler);
  }

  @GetMapping(
      value = "/services",
      params = {"sort=accessRank,desc", "!health"})
  public ResponseEntity<?> byRankDesc(
      Pageable pageable, PersistentEntityResourceAssembler assembler) {
    return byHealth(null, null, pageable, assembler);
  }

  @GetMapping(
      value = "/services",
      params = {"sort=accessRank,asc", "!health"})
  public ResponseEntity<?> byRankAsc(
      Pageable pageable, PersistentEntityResourceAssembler assembler) {
    return byHealth(null, null, pageable, assembler);
  }
}
