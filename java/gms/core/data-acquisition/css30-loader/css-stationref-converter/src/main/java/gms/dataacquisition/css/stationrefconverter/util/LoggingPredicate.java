package gms.dataacquisition.css.stationrefconverter.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Function;
import java.util.function.Predicate;

public class LoggingPredicate<T> implements Predicate<T> {

  private final Logger logger;
  private final LoggerLevel level;
  private final Function<T, String> messageBuilder;
  private final Predicate<T> wrapped;

  public LoggingPredicate(LoggerLevel level, Predicate<T> wrapped, Function<T, String> messageBuilder, Class<?> forClass) {
    logger = LoggerFactory.getLogger(forClass);
    this.level = level;
    this.messageBuilder = messageBuilder;
    this.wrapped = wrapped;
  }

  /**
   * Evaluates this predicate on the given argument.
   *
   * @param t the input argument
   * @return {@code true} if the input argument matches the predicate,
   * otherwise {@code false}
   */
  @Override
  public boolean test(T t) {
    boolean result = wrapped.test(t);

    if (!result) {
      String message = messageBuilder.apply(t);
      if (level == LoggerLevel.WARN && logger.isWarnEnabled()) {
        logger.warn(message);
      } else if (level == LoggerLevel.INFO && logger.isInfoEnabled()) {
        logger.info(message);
      } else if (level == LoggerLevel.ERROR && logger.isErrorEnabled()) {
        logger.error(message);
      }
    }

    return result;
  }
}
