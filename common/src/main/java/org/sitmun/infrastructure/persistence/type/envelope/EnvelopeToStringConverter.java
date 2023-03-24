package org.sitmun.infrastructure.persistence.type.envelope;

import lombok.extern.slf4j.Slf4j;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * Map a {@link Envelope} to a String.
 */
@Converter
@Slf4j
public class EnvelopeToStringConverter implements AttributeConverter<Envelope, String> {

  public static final String RECISION = "######";

  public static final String FORMAT = "{0,number,#." + RECISION + "} {1,number,#." + RECISION + "} {2,number,#." + RECISION + "} {3,number,#." + RECISION + "}";

  public static final Locale defaultLocale = Locale.US;

  public MessageFormat formatInstance() {
    return new MessageFormat(FORMAT, defaultLocale);
  }

  public Double extractDouble(Object obj) throws IllegalArgumentException {
    if (obj instanceof Number) {
      return ((Number) obj).doubleValue();
    } else {
      throw new IllegalArgumentException("Value " + obj + " is not a Number");
    }
  }

  @Override
  public String convertToDatabaseColumn(Envelope envelope) {
    if (envelope == null) {
      return null;
    } else {
      return formatInstance().format(new Object[]{
        envelope.getMinX(),
        envelope.getMinY(),
        envelope.getMaxX(),
        envelope.getMaxY()
      });
    }
  }

  @Override
  public Envelope convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    } else {
      try {
        Object[] values = formatInstance().parse(dbData);
        return Envelope.builder()
          .minX(extractDouble(values[0]))
          .minY(extractDouble(values[1]))
          .maxX(extractDouble(values[2]))
          .maxY(extractDouble(values[3]))
          .build();
      } catch (Exception e) {
        log.error("[" + dbData + "] cannot be parsed to Envelope", e);
        return null;
      }
    }
  }
}