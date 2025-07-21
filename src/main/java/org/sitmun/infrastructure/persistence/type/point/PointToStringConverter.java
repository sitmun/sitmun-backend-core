package org.sitmun.infrastructure.persistence.type.point;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * Map a {@link Point} to a String.
 */
@Converter
@Slf4j
public class PointToStringConverter implements AttributeConverter<Point, String> {

  public static final String PRECISION = "######";

  public static final String FORMAT = "{0,number,#." + PRECISION + "} {1,number,#." + PRECISION + '}';

  public static final Locale defaultLocale = Locale.US;

  public MessageFormat formatInstance() {
    return new MessageFormat(FORMAT, defaultLocale);
  }

  public Double extractDouble(Object obj) throws IllegalArgumentException {
    if (obj instanceof Number number) {
      return number.doubleValue();
    }
    throw new IllegalArgumentException("Value " + obj + " is not a Number");
  }

  @Override
  public String convertToDatabaseColumn(Point envelope) {
    if (envelope == null) {
      return null;
    }
    if (envelope.getX() == null || envelope.getY() == null) {
      return null;
    }
    return formatInstance().format(new Object[]{
      envelope.getX(),
      envelope.getY(),
    });
  }

  @Override
  public Point convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    try {
      Object[] values = formatInstance().parse(dbData);
      return Point.builder()
        .x(extractDouble(values[0]))
        .y(extractDouble(values[1]))
        .build();
    } catch (Exception e) {
      log.error("[{}] cannot be parsed to Point", dbData);
      return null;
    }
  }
}