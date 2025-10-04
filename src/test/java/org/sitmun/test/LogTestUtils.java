package org.sitmun.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 * Utility class for testing log content in Spring Boot tests. Provides methods to capture and
 * verify log messages.
 */
public class LogTestUtils {

  private final ListAppender<ILoggingEvent> listAppender;
  private final Logger logger;

  /**
   * Creates a LogTestUtils instance for the specified logger name.
   *
   * @param loggerName the name of the logger to capture
   */
  public LogTestUtils(String loggerName) {
    this.logger = (Logger) LoggerFactory.getLogger(loggerName);
    this.listAppender = new ListAppender<>();
    this.listAppender.start();
  }

  /** Starts capturing log events for the logger. */
  public void startCapturing() {
    logger.addAppender(listAppender);
    logger.setLevel(Level.DEBUG);
  }

  /** Stops capturing log events and removes the appender. */
  public void stopCapturing() {
    logger.detachAppender(listAppender);
  }

  /**
   * Gets log messages containing a specific text.
   *
   * @param text the text to search for
   * @return list of log messages containing the text
   */
  public List<String> getLogMessagesContaining(String text) {
    Pattern pattern = Pattern.compile(text, Pattern.CASE_INSENSITIVE);
    return listAppender.list.stream()
        .map(ILoggingEvent::getFormattedMessage)
        .filter(message -> pattern.matcher(message).find())
        .collect(Collectors.toList());
  }
}
