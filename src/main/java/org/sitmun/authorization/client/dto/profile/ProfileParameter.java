package org.sitmun.authorization.client.dto.profile;

/**
 * Sealed interface for typed task parameter projections in client configuration profiles.
 *
 * <p>Each permitted implementation represents a specific parameter shape consumed by the viewer or
 * admin applications. The wire format (JSON key order, nullability) is locked by Phase 1 snapshot
 * tests and must remain byte-identical during migration.
 *
 * @see QueryParameter
 * @see ServiceParameter
 * @see FeatureInfoParameter
 */
public sealed interface ProfileParameter
    permits QueryParameter, ServiceParameter, FeatureInfoParameter {}
