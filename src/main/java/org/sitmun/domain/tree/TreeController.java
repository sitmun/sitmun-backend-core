package org.sitmun.domain.tree;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.tree.dto.TreeTypeValidationRequest;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;
import org.sitmun.infrastructure.web.dto.ProblemDetail;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for tree-related operations that are not handled by Spring Data REST.
 *
 * <p>This controller provides custom endpoints for operations that cannot be easily expressed
 * through the standard REST repository pattern, such as validating tree type changes against
 * candidate applications.
 */
@RestController
@RequestMapping("/api/trees")
public class TreeController {

  private final TreeRepository treeRepository;
  private final ApplicationRepository applicationRepository;

  public TreeController(
      TreeRepository treeRepository, ApplicationRepository applicationRepository) {
    this.treeRepository = treeRepository;
    this.applicationRepository = applicationRepository;
  }

  /**
   * Validates a tree type change against candidate applications.
   *
   * <p>This endpoint is necessary because Spring Data REST requires two separate PUT operations:
   *
   * <ol>
   *   <li>PUT /api/trees/{id} - updates tree properties (including type)
   *   <li>PUT /api/trees/{id}/availableApplications - updates linked applications
   * </ol>
   *
   * <p>Without proactive validation, the first PUT could succeed (type change) and the second PUT
   * could fail (incompatible applications), requiring a rollback that violates REST principles.
   *
   * @param treeId the ID of the tree to validate
   * @param request the validation request containing the new type and candidate application IDs
   * @return 204 No Content if validation passes, 422 Unprocessable Entity if validation fails
   */
  @PostMapping("/{treeId}/validate-type-change")
  public ResponseEntity<Void> validateTreeTypeChange(
      @PathVariable Integer treeId,
      @Valid @RequestBody TreeTypeValidationRequest request,
      HttpServletRequest httpRequest) {

    // Verify tree exists
    Tree tree =
        treeRepository
            .findById(treeId)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        ProblemTypes.NOT_FOUND, "Tree with ID " + treeId + " not found"));

    // Fetch candidate applications
    Set<Integer> appIds =
        request.getApplicationIds() != null ? request.getApplicationIds() : Set.of();
    List<Application> candidateApps = fetchApplications(appIds);

    // Validate the type change
    validateTypeAgainstApplications(request.getType(), candidateApps, tree);

    return ResponseEntity.noContent().build();
  }

  /**
   * Fetches applications by their IDs.
   *
   * @param appIds the application IDs to fetch
   * @return list of applications
   * @throws BusinessRuleException if any application is not found
   */
  private List<Application> fetchApplications(Set<Integer> appIds) {
    if (appIds.isEmpty()) {
      return List.of();
    }

    List<Application> apps = applicationRepository.findAllById(appIds);

    // Verify all requested applications were found
    if (apps.size() != appIds.size()) {
      Set<Integer> foundIds = apps.stream().map(Application::getId).collect(Collectors.toSet());
      Set<Integer> missingIds =
          appIds.stream().filter(id -> !foundIds.contains(id)).collect(Collectors.toSet());
      throw new BusinessRuleException(
          ProblemTypes.NOT_FOUND, "Application(s) with ID(s) " + missingIds + " not found");
    }

    return apps;
  }

  /**
   * Validates the tree type against candidate applications using the same business rules as
   * TreeEventHandler.
   *
   * @param type the intended tree type
   * @param apps the candidate applications to link
   * @param tree the tree being validated (used to exclude from touristic app counting)
   * @throws BusinessRuleException if validation fails
   */
  private void validateTypeAgainstApplications(String type, List<Application> apps, Tree tree) {
    if (DomainConstants.Trees.TYPE_TOURISTIC.equals(type)) {
      validateTouristicTreeType(apps);
    } else {
      validateNonTouristicTreeType(apps, tree);
    }
  }

  /**
   * Validates that a touristic tree can be linked to the candidate applications.
   *
   * <p>Business rules:
   *
   * <ul>
   *   <li>Touristic tree can be linked to 0 applications (valid)
   *   <li>Touristic tree can be linked to 1 touristic application (valid)
   *   <li>Touristic tree cannot be linked to multiple applications (invalid)
   *   <li>Touristic tree cannot be linked to non-touristic applications (invalid)
   * </ul>
   *
   * @param apps the candidate applications
   * @throws BusinessRuleException if validation fails
   */
  private void validateTouristicTreeType(List<Application> apps) {
    if (apps.isEmpty()) {
      return; // No applications linked, valid case
    }
    if (apps.size() == 1 && DomainConstants.Applications.isTouristicApplication(apps.get(0))) {
      return; // Valid case with one touristic application
    }
    throw new BusinessRuleException(
        ProblemTypes.TREE_TYPE_CHANGE_CONSTRAINT,
        "Touristic tree can only be linked with 0 or 1 tourist application");
  }

  /**
   * Validates that a non-touristic tree can be linked to the candidate applications.
   *
   * <p>Business rules:
   *
   * <ul>
   *   <li>Non-touristic tree can be linked to any number of non-touristic applications (valid)
   *   <li>Non-touristic tree can be linked to a touristic application only if that application has
   *       exactly one touristic tree linked (excluding the current tree being validated)
   * </ul>
   *
   * @param apps the candidate applications
   * @param currentTree the tree being validated (to exclude from counting)
   * @throws BusinessRuleException if validation fails
   */
  private void validateNonTouristicTreeType(List<Application> apps, Tree currentTree) {
    boolean valid = apps.stream().allMatch(app -> validAppTrees(app, currentTree));

    if (!valid) {
      throw new BusinessRuleException(
          ProblemTypes.TREE_TYPE_CHANGE_CONSTRAINT,
          "A non-touristic tree can only be linked to a non-tourist application or touristic application with only one touristic tree");
    }
  }

  /**
   * Checks if an application can be linked to a non-touristic tree.
   *
   * @param app the application to check
   * @param currentTree the tree being validated (to exclude from counting)
   * @return true if the application can be linked, false otherwise
   */
  private boolean validAppTrees(Application app, Tree currentTree) {
    if (DomainConstants.Applications.isTouristicApplication(app)) {
      return validateTouristicApp(app, currentTree);
    }
    return true; // Non-touristic apps are always valid
  }

  /**
   * Validates that a touristic application has exactly one touristic tree.
   *
   * @param app the touristic application to validate
   * @param currentTree the tree being validated (to exclude from counting)
   * @return true if the application has exactly one touristic tree (excluding current), false
   *     otherwise
   */
  private boolean validateTouristicApp(Application app, Tree currentTree) {
    List<Tree> trees =
        app.getTrees().stream()
            .filter(t -> !t.getId().equals(currentTree.getId())) // Exclude current tree
            .toList();
    return trees.size() == 1 && DomainConstants.Trees.isTouristicTree(trees.get(0));
  }

  /**
   * Handles BusinessRuleException thrown during validation.
   *
   * @param exception the exception
   * @param request the HTTP request
   * @return RFC 9457 problem detail response
   */
  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ProblemDetail> handleBusinessRuleException(
      BusinessRuleException exception, HttpServletRequest request) {

    // Determine HTTP status based on problem type
    HttpStatus status;
    String title;
    if (ProblemTypes.NOT_FOUND.equals(exception.getProblemType())) {
      status = HttpStatus.NOT_FOUND;
      title = "Resource Not Found";
    } else {
      status = HttpStatus.UNPROCESSABLE_ENTITY;
      title = "Tree Type Change Validation Failed";
    }

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(exception.getProblemType())
            .status(status.value())
            .title(title)
            .detail(exception.getMessage())
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
