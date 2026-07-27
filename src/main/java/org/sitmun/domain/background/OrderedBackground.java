package org.sitmun.domain.background;

/** Active background selected for an application with its application-specific order. */
public record OrderedBackground(Integer order, Background background) {}
