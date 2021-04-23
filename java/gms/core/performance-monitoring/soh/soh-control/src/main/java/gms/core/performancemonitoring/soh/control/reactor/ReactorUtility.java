package gms.core.performancemonitoring.soh.control.reactor;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

/**
 * General useful reactor utility methods
 */
public class ReactorUtility {

  private ReactorUtility() {

  }

  /**
   * Takes a Mono, mapper, and definition and creates UnicastProcessor, maps the mapper
   * across the Mono, and subscribes the UnicastProcesssor to the transformed Mono.
   *
   * @param inMono Mono that has what we want mapped
   * @param mapper mapper that maps what is in the Mono (as well as a definition) to a new thing
   * @param parameterObject object that tunes th behavior of the mapper. Can be null.
   * @param <I> Type that is wrapped by the Mono
   * @param <O> Type that I gets mapped to
   * @param <P> Type of "parameter" object that tunes the behavior of the mapper
   * @return the subscribed UnicastProcessor
   */
  // TODO: Check if something besides a UnicastProcessor is appropriate, as
  //   only one thing (from the Mono) is ever emitted. Maybe even look into a custom
  //   processor.
  public static <I, P, O> UnicastProcessor<O> getMonoSubscribedProcessor(
      Mono<I> inMono,
      BiFunction<I, P, O> mapper,
      P parameterObject
  ) {

    //This check cannot be inline, because it will not happen until the mono
    // starts emitting!
    Objects.requireNonNull(mapper);

    var processor = UnicastProcessor.<O>create();

    Objects.requireNonNull(inMono).cache()
        .map(item -> mapper.apply(item, parameterObject))
        .subscribe(processor);

    return processor;
  }

  /**
   * Takes a Mono, mapper, and definition and creates UnicastProcessor, maps the mapper
   * across the Mono, and subscribes the UnicastProcesssor to the transformed Mono.
   *
   * @param inMono Mono that has what we want mapped
   * @param mapper mapper that maps what is in the Mono (as well as a definition) to a new thing
   * @param parameterObject object that tunes th behavior of the mapper
   * @param <I> Type that is wrapped by the Mono
   * @param <O> Type that I gets mapped to
   * @return the subscribed UnicastProcessor
   */
  public static <I, O> UnicastProcessor<O> getMonoSubscribedProcessor(
      Mono<I> inMono,
      Function<I, O> mapper
  ) {

    var processor = UnicastProcessor.<O>create();

    Objects.requireNonNull(inMono).cache()
        .map(Objects.requireNonNull(mapper))
        .subscribe(processor);

    return processor;
  }

}
