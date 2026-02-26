package org.sitmun.infrastructure.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.DomainConstants;

/** Tests for TaskParameterUtil ensuring backward-compatible parameter access. */
class TaskParameterUtilTest {

  @Test
  void getParameterVariable_withVariableKey_returnsVariable() {
    Map<String, Object> param = new HashMap<>();
    param.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "userId");

    String result = TaskParameterUtil.getParameterVariable(param);

    assertThat(result).isEqualTo("userId");
  }

  @Test
  void getParameterVariable_withNameKey_returnsName() {
    Map<String, Object> param = new HashMap<>();
    param.put(DomainConstants.Tasks.PARAMETERS_NAME, "userId");

    String result = TaskParameterUtil.getParameterVariable(param);

    assertThat(result).isEqualTo("userId");
  }

  @Test
  void getParameterVariable_withBothKeys_prioritizesVariable() {
    Map<String, Object> param = new HashMap<>();
    param.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "newName");
    param.put(DomainConstants.Tasks.PARAMETERS_NAME, "oldName");

    String result = TaskParameterUtil.getParameterVariable(param);

    assertThat(result).isEqualTo("newName");
  }

  @Test
  void getParameterVariable_withNeitherKey_returnsNull() {
    Map<String, Object> param = new HashMap<>();
    param.put("someOtherKey", "value");

    String result = TaskParameterUtil.getParameterVariable(param);

    assertThat(result).isNull();
  }

  @Test
  void getParameterVariable_withNullMap_returnsNull() {
    String result = TaskParameterUtil.getParameterVariable(null);

    assertThat(result).isNull();
  }

  @Test
  void getParameterVariable_withEmptyMap_returnsNull() {
    Map<String, Object> param = new HashMap<>();

    String result = TaskParameterUtil.getParameterVariable(param);

    assertThat(result).isNull();
  }

  @Test
  void getParameterVariable_withNullValue_returnsNullString() {
    Map<String, Object> param = new HashMap<>();
    param.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, null);

    String result = TaskParameterUtil.getParameterVariable(param);

    // Falls back to name key, which doesn't exist
    assertThat(result).isNull();
  }

  @Test
  void getParameterVariable_withNumericValue_convertsToString() {
    Map<String, Object> param = new HashMap<>();
    param.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, 123);

    String result = TaskParameterUtil.getParameterVariable(param);

    assertThat(result).isEqualTo("123");
  }

  @Test
  void hasParameterVariable_withVariableKey_returnsTrue() {
    Map<String, Object> param = new HashMap<>();
    param.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "userId");

    boolean result = TaskParameterUtil.hasParameterVariable(param);

    assertThat(result).isTrue();
  }

  @Test
  void hasParameterVariable_withNameKey_returnsTrue() {
    Map<String, Object> param = new HashMap<>();
    param.put(DomainConstants.Tasks.PARAMETERS_NAME, "userId");

    boolean result = TaskParameterUtil.hasParameterVariable(param);

    assertThat(result).isTrue();
  }

  @Test
  void hasParameterVariable_withBothKeys_returnsTrue() {
    Map<String, Object> param = new HashMap<>();
    param.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "newName");
    param.put(DomainConstants.Tasks.PARAMETERS_NAME, "oldName");

    boolean result = TaskParameterUtil.hasParameterVariable(param);

    assertThat(result).isTrue();
  }

  @Test
  void hasParameterVariable_withNeitherKey_returnsFalse() {
    Map<String, Object> param = new HashMap<>();
    param.put("someOtherKey", "value");

    boolean result = TaskParameterUtil.hasParameterVariable(param);

    assertThat(result).isFalse();
  }

  @Test
  void hasParameterVariable_withNullMap_returnsFalse() {
    boolean result = TaskParameterUtil.hasParameterVariable(null);

    assertThat(result).isFalse();
  }

  @Test
  void hasParameterVariable_withEmptyMap_returnsFalse() {
    Map<String, Object> param = new HashMap<>();

    boolean result = TaskParameterUtil.hasParameterVariable(param);

    assertThat(result).isFalse();
  }
}
