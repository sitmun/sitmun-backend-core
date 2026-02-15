package org.sitmun.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.test.context.ActiveProfilesResolver;

/**
 * Non-mutating active profiles resolver that merges externally provided profiles with
 * test-specific additive profiles.
 *
 * <p>This resolver reads base profiles from system property {@code spring.profiles.active} (set by
 * Gradle tasks for DB selection: test,h2 or postgres or oracle), falling back to environment
 * variable {@code SPRING_PROFILES_ACTIVE} if the property is empty. It then appends any additional
 * profiles declared via {@code @AdditiveActiveProfiles} on the test class.
 *
 * <p>This approach avoids JVM-global mutation (no {@code System.setProperty} calls) and is
 * parallel-safe.
 */
public class AdditiveProfilesResolver implements ActiveProfilesResolver {

  @Override
  public String[] resolve(Class<?> testClass) {
    // Start with base profiles from external source (Gradle/env)
    List<String> baseProfiles = getBaseProfiles();

    // Add test-specific profiles from @AdditiveActiveProfiles
    AdditiveActiveProfiles annotation = testClass.getAnnotation(AdditiveActiveProfiles.class);
    if (annotation != null && annotation.value() != null) {
      baseProfiles.addAll(Arrays.asList(annotation.value()));
    }

    // Deduplicate while preserving order (LinkedHashSet)
    Set<String> uniqueProfiles = new LinkedHashSet<>(baseProfiles);

    return uniqueProfiles.toArray(new String[0]);
  }

  /**
   * Get base profiles from system property or environment variable.
   *
   * @return list of base profiles (mutable)
   */
  private List<String> getBaseProfiles() {
    String profilesStr = System.getProperty("spring.profiles.active");

    // Fallback to environment variable only if system property is empty
    if (profilesStr == null || profilesStr.trim().isEmpty()) {
      profilesStr = System.getenv("SPRING_PROFILES_ACTIVE");
    }

    List<String> profiles = new ArrayList<>();
    if (profilesStr != null && !profilesStr.trim().isEmpty()) {
      // Split by comma and trim each profile
      String[] parts = profilesStr.split(",");
      for (String part : parts) {
        String trimmed = part.trim();
        if (!trimmed.isEmpty()) {
          profiles.add(trimmed);
        }
      }
    }

    return profiles;
  }
}
