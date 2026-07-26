package org.sitmun.domain.application;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.sitmun.domain.application.tree.ApplicationTree;
import org.sitmun.domain.tree.Tree;
import org.sitmun.domain.tree.TreeApplicationLinkPolicy;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RepositoryEventHandler
public class ApplicationEventHandler {

  // linked is Object: SDR calls this for every association (e.g. situationMap), not only trees
  @HandleBeforeLinkSave
  @Transactional
  public void handleApplicationTreeLink(
      @NotNull Application application, @SuppressWarnings("unused") Object linked) {
    List<Tree> trees = application.getTrees().stream().map(ApplicationTree::getTree).toList();
    TreeApplicationLinkPolicy.validateApplicationTrees(application, trees);
  }
}
