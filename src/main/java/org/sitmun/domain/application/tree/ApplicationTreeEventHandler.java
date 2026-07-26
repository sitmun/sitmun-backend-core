package org.sitmun.domain.application.tree;

import java.util.List;
import org.sitmun.domain.tree.TreeApplicationLinkPolicy;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler(ApplicationTree.class)
public class ApplicationTreeEventHandler {

  @HandleBeforeCreate
  @HandleBeforeSave
  void validate(ApplicationTree link) {
    TreeApplicationLinkPolicy.validateTreeApplications(
        link.getTree(), List.of(link.getApplication()));
  }
}
