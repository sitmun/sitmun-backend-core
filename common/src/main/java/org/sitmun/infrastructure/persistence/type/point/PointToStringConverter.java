package org.sitmun.infrastructure.persistence.type.point;

import lombok.extern.slf4j.Slf4j;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
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
    if (obj instanceof Number) {
      return ((Number) obj).doubleValue();
    }
      throw new IllegalArgumentException("Value " + obj + " is not a Number");
  }

  @Override
  public String convertToDatabaseColumn(Point envelope) {
    if (envelope == null) {
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
        log.error('[' + dbData + "] cannot be parsed to Point", e);
        return null;
      }
  }
}