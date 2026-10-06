package org.sitmun.administration.service.access;

import java.util.List;

public record ServiceAccessHour(String status, int statusRank, List<String> observers) {}
