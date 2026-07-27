package org.sitmun.domain.tree;

/** Tree selected for an application with its application-specific order. */
public record OrderedTree(Integer order, Tree tree) {}
