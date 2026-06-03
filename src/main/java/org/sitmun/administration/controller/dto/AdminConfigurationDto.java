package org.sitmun.administration.controller.dto;

/**
 * Runtime configuration contract for the SITMUN admin application. Designed to be extensible:
 * future sections of {@code front/admin/sitmun-admin-app/src/config.ts} may be added here without
 * changing the endpoint URL.
 */
public record AdminConfigurationDto(AdminImageUploadConfigurationDto imageUpload) {}
