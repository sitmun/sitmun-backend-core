package org.sitmun.decorators;

import org.sitmun.authorization.dto.PayloadDto;

public interface Decorator {

    boolean accept(Object target, PayloadDto payload);

    default void apply(Object target, PayloadDto payload) {
        if (accept(target, payload)) {
            addBehavior(target, payload);
        }
    }

    void addBehavior(Object target, PayloadDto payload);
}
