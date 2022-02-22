package org.sitmun.converters;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.Point;

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

  public static final String precision = "######";

  public static final String format = "{0,number,#." + precision + "} {1,number,#." + precision + "}";

  public static final Locale defaultLocale = Locale.US;

  public MessageFormat formatInstance() {
    MessageFormat mf = new MessageFormat(format);
    mf.setLocale(defaultLocale);
    return mf;
  }

  public Double extractDouble(Object obj) throws Exception {
    if (obj instanceof Number) {
      return ((Number) obj).doubleValue();
    } else {
      throw new Exception("Value " + obj + " is not a Number");
    }
  }

  @Override
  public String convertToDatabaseColumn(Point envelope) {
    if (envelope == null) {
      return null;
    } else {
      return formatInstance().format(new Object[]{
        envelope.getX(),
        envelope.getY(),
      });
    }
  }

  @Override
  public Point convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    } else {
      try {
        Object[] values = formatInstance().parse(dbData);
        return Point.builder()
          .x(extractDouble(values[0]))
          .y(extractDouble(values[1]))
          .build();
      } catch (Exception e) {
        log.error("[" + dbData + "] cannot be parsed to Point", e);
        return null;
      }
    }
  }
}