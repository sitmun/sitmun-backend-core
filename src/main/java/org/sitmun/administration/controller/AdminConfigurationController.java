package org.sitmun.administration.controller;

import java.util.stream.Collectors;
import org.sitmun.administration.controller.dto.AdminConfigurationDto;
import org.sitmun.administration.controller.dto.AdminImageUploadConfigurationDto;
import org.sitmun.administration.controller.dto.ImageSizeDto;
import org.sitmun.administration.controller.dto.TreeImageUploadConfigurationDto;
import org.sitmun.infrastructure.persistence.type.image.ImageScalingProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes runtime configuration for the SITMUN admin application. */
@RestController
@RequestMapping("/api/config/admin")
public class AdminConfigurationController {

  /** Maximum image upload size enforced by the admin app file picker (2 MB). */
  static final long TREE_IMAGE_MAX_BYTES = 2_097_152L;

  private final ImageScalingProperties imageScalingProperties;

  public AdminConfigurationController(ImageScalingProperties imageScalingProperties) {
    this.imageScalingProperties = imageScalingProperties;
  }

  @GetMapping
  public AdminConfigurationDto getAdminConfiguration() {
    var defaultSize =
        new ImageSizeDto(
            imageScalingProperties.getDefaultWidth(), imageScalingProperties.getDefaultHeight());

    var sizesByType =
        imageScalingProperties.getSizes().stream()
            .collect(
                Collectors.toMap(
                    p -> p.getType(), p -> new ImageSizeDto(p.getWidth(), p.getHeight())));

    var treeConfig =
        new TreeImageUploadConfigurationDto(
            imageScalingProperties.getSupportedFormats(),
            TREE_IMAGE_MAX_BYTES,
            defaultSize,
            sizesByType);

    return new AdminConfigurationDto(new AdminImageUploadConfigurationDto(treeConfig));
  }
}
