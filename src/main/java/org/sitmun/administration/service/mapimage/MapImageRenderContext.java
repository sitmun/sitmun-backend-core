package org.sitmun.administration.service.mapimage;

import java.util.List;

record MapImageRenderContext(List<Double> bbox, int width, int height, String srs) {}
