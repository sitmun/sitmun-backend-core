package org.sitmun.test;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Custom TestExecutionListener that makes @ActiveProfiles additive.
 *
 * <p>This listener appends the profiles specified in @ActiveProfiles to the existing
 * spring.profiles.active system property instead of replacing it.
 */
public class AdditiveProfilesTestExecutionListener extends AbstractTestExecutionListener {

  @Override
  public void beforeTestClass(TestContext testContext) {
    // Get the existing profiles from system property
    String existingProfiles = System.getProperty("spring.profiles.active", "");

    // Get the additional profiles from @ActiveProfiles annotation
    AdditiveActiveProfiles ann =
        testContext.getTestClass().getAnnotation(AdditiveActiveProfiles.class);
    String[] additionalProfiles;

    if (ann != null && ann.value() != null) {
      additionalProfiles = ann.value();
    } else {
      // If no @AdditiveActiveProfiles annotation, return early
      return;
    }

    if (additionalProfiles.length > 0) {
      testContext.setAttribute("org.sitmun.test.spring.profiles.active", existingProfiles);

      // Combine existing and additional profiles
      StringBuilder combinedProfiles = new StringBuilder();

      if (!existingProfiles.isEmpty()) {
        combinedProfiles.append(existingProfiles);
      }

      for (String profile : additionalProfiles) {
        if (!combinedProfiles.isEmpty()) {
          combinedProfiles.append(",");
        }
        combinedProfiles.append(profile);
      }

      // Set the combined profiles
      System.setProperty("spring.profiles.active", combinedProfiles.toString());
    }
  }

  @Override
  public void afterTestClass(TestContext testContext) {
    if (testContext.hasAttribute("org.sitmun.test.spring.profiles.active")) {
      String existingProfiles =
          (String) testContext.getAttribute("org.sitmun.test.spring.profiles.active");
      if (existingProfiles != null) {
        System.setProperty("spring.profiles.active", existingProfiles);
      }
      testContext.removeAttribute("org.sitmun.test.spring.profiles.active");
    }
  }
}
