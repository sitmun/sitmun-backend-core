package org.sitmun.authorization.proxy.service;

import lombok.Data;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;

@Data
public class RequestCoordinates {
  User user;
  Territory territory;
  Application application;
}
