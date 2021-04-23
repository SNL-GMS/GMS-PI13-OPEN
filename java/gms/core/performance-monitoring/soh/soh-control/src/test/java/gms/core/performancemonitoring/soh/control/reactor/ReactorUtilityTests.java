package gms.core.performancemonitoring.soh.control.reactor;

import com.google.common.util.concurrent.AtomicDouble;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ReactorUtilityTests {

  @Disabled("Thread yield() calls cause intermittent failures in the pipeline")
  @Test
  void testParameterizedGetMonoSubscribedProcessor() {

    BiFunction<Integer, Double, Double> mapper = (i, p) -> i * p;

    var processor = ReactorUtility.getMonoSubscribedProcessor(
        Mono.just(1),
        mapper,
        2.0
    );

    AtomicDouble resultRef = new AtomicDouble(Double.MIN_VALUE);

    processor.subscribe(resultRef::set);

    while (!(resultRef.get() > Double.MIN_VALUE)) {
      Thread.yield();
    }

    Assertions.assertEquals(
        2.0, resultRef.get()
    );
  }

  @Disabled("Thread yield() calls cause intermittent failures in the pipeline")
  @Test
  void testGetMonoSubscribedProcessor() {

    Function<Integer, Double> mapper = i -> i * 2.0;

    var processor = ReactorUtility.getMonoSubscribedProcessor(
        Mono.just(1),
        mapper
    );

    AtomicDouble resultRef = new AtomicDouble(Double.MIN_VALUE);

    processor.subscribe(resultRef::set);

    while (!(resultRef.get() > Double.MIN_VALUE)) {
      Thread.yield();
    }

    Assertions.assertEquals(
        2.0, resultRef.get()
    );
  }

  @Test
  void testParameterizedGetMonoSubscribedProcessorValidation() {

    // Dont want multiple method calls when testing that a single method
    // throws an exception.
    var theMono = Mono.just(1);

    Assertions.assertThrows(
        NullPointerException.class,
        () -> ReactorUtility.getMonoSubscribedProcessor(
            theMono,
            null,
            2.0
        )
    );

    Assertions.assertThrows(
        NullPointerException.class,
        () -> ReactorUtility.getMonoSubscribedProcessor(
            null,
            (i, p) -> 0,
            2.0
        )
    );

    Assertions.assertThrows(
        NullPointerException.class,
        () -> ReactorUtility.getMonoSubscribedProcessor(
            null,
            null,
            2.0
        )
    );
  }

  @Test
  void testGetMonoSubscribedProcessorValidation() {

    // Dont want multiple method calls when testing that a single method
    // throws an exception.
    var theMono = Mono.just(1);

    Assertions.assertThrows(
        NullPointerException.class,
        () -> ReactorUtility.getMonoSubscribedProcessor(
            theMono,
            null
        )
    );

    Assertions.assertThrows(
        NullPointerException.class,
        () -> ReactorUtility.getMonoSubscribedProcessor(
            null,
            i -> 0
        )
    );
  }
}
