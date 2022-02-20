package org.sitmun.plugin.core.test;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.time.format.DateTimeFormatter;

public class DateTimeMatchers {
  public static Matcher<String> isIso8601DateAndTime() {
    return new ISO8601DateAndTime();
  }

  private static class ISO8601DateAndTime extends TypeSafeMatcher<String> {

    @Override
    public void describeTo(Description description) {
      description.appendText("must be ISO-8601 date-time");
    }

    @Override
    protected boolean matchesSafely(String item) {
      try {
        DateTimeFormatter.ISO_DATE_TIME.parse(item);
        return true;
      } catch (Exception e) {
        return false;
      }
    }
  }
}
