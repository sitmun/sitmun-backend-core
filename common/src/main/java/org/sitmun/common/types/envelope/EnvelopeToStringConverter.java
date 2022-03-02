package org.sitmun.common.types.envelope;

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

  public static final String precision = "######";

  public static final String format = "{0,number,#." + precision + "} {1,number,#." + precision + "} {2,number,#." + precision + "} {3,number,#." + precision + "}";

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