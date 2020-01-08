package org.sitmun.plugin.core.security;

import org.sitmun.plugin.core.domain.User;

public interface PermissionResolver<E> {

  boolean resolvePermission(User authUser, E entity, String permission);

  //public boolean resolvePermission(User authUser, Serializable id, String permission);


}
