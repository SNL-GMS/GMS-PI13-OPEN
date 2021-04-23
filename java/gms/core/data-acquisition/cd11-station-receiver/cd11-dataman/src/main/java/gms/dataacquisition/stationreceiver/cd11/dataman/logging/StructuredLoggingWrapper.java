package gms.dataacquisition.stationreceiver.cd11.dataman.logging;


import com.google.common.base.Throwables;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.argument.StructuredArguments;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

/**
 * Wrapper for logback-based loggers to transparently insert structured arguments into logging
 * statements. All throwables provided to this wrapper will have their stack traces aggregated into
 * an array field within the structured log in addition to any format insertion.
 *
 * NOTE: If providing a non-logback-based logger, application of structured arguments will fail, but
 * formatted logs should still log successfully.
 */
public class StructuredLoggingWrapper {

  private final Logger logger;
  private final Map<String, StructuredArgument> structuredArgsByName;

  private StructuredLoggingWrapper(Logger logger) {
    this.logger = logger;
    this.structuredArgsByName = new ConcurrentHashMap<>();
  }

  public static StructuredLoggingWrapper create(Logger logger) {
    return new StructuredLoggingWrapper(logger);
  }

  public Logger getWrappedLogger() {
    return logger;
  }

  /**
   * Adds a {@link StructuredArgument} to the wrapper's arguments that, when logged in a format
   * string, will log the entire key=value pair
   *
   * @param key Key to be used for the JSON field
   * @param value Value to be used for the JSON field
   */
  public void addKeyValueArgument(String key, Object value) {
    structuredArgsByName.put(key, StructuredArguments.keyValue(key, value));
  }

  /**
   * Adds a {@link StructuredArgument} to the wrapper's arguments that, when logged in a format
   * string, will only log the value
   *
   * @param key Key to be used for the JSON field
   * @param value Value to be used for the JSON field
   */
  public void addValueArgument(String key, Object value) {
    structuredArgsByName.put(key, StructuredArguments.value(key, value));
  }

  /**
   * Removes the argument with matching key from this wrapper's arguments
   *
   * @param key Key of the structured argument to be removed
   */
  public void removeArgument(String key) {
    structuredArgsByName.remove(key);
  }

  public void info(String message) {
    logger.info(message, joinArgs());
  }

  public void info(String formatMessage, Object... arguments) {
    logger.info(formatMessage, joinArgs(aggregateThrowableStackTraces(arguments)));
  }

  public void warn(String message) {
    logger.warn(message, joinArgs());
  }

  public void warn(String formatMessage, Object... arguments) {
    logger.warn(formatMessage, joinArgs(aggregateThrowableStackTraces(arguments)));
  }

  public void error(String message) {
    logger.error(message, joinArgs());
  }

  public void error(String formatMessage, Object... arguments) {
    logger.error(formatMessage, joinArgs(aggregateThrowableStackTraces(arguments)));
  }

  public void debug(String message) {
    logger.debug(message, joinArgs());
  }

  public void debug(String formatMessage, Object... arguments) {
    logger.debug(formatMessage, joinArgs(aggregateThrowableStackTraces(arguments)));
  }

  private Object[] joinArgs(Object... arguments) {
    return ArrayUtils.addAll(arguments, structuredArgsByName.values().toArray());
  }

  private Object[] aggregateThrowableStackTraces(Object[] arguments) {
    List<String> traces = Arrays.stream(arguments)
        .filter(obj -> obj instanceof Throwable)
        .map(t -> (Throwable) t)
        .map(Throwables::getStackTraceAsString)
        .collect(Collectors.toList());
    Object stackTraces = StructuredArguments.value("stackTraces", traces);
    if (traces.isEmpty()) {
      return arguments;
    }
    return ArrayUtils.add(arguments, stackTraces);
  }
}
