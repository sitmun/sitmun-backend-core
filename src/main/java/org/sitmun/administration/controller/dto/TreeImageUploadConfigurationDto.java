package org.sitmun.administration.controller.dto;

import java.util.List;
import java.util.Map;

/**
 * Image upload constraints for tree and tree-node images as enforced by the backend. The backend
 * always resizes uploaded images to {@link #defaultSize}, or to the matching entry in {@link
 * #sizesByType} when the node has a known parent type.
 */
public record TreeImageUploadConfigurationDto(
    List<String> supportedFormats,
    long maxBytes,
    ImageSizeDto defaultSize,
    Map<String, ImageSizeDto> sizesByType) {}
