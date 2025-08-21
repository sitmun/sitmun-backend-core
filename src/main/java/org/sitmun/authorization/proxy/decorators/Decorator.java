package org.sitmun.authorization.proxy.decorators;

import org.sitmun.authorization.proxy.dto.PayloadDto;

public interface Decorator<T> {

  boolean accept(T target, PayloadDto payload);

  default void apply(T target, PayloadDto payload) {
    if (accept(target, payload)) {
      addBehavior(target, payload);
    }
  }

  void addBehavior(T target, PayloadDto payload);
}
