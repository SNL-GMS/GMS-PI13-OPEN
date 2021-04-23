package gms.shared.frameworks.injector;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import reactor.core.publisher.Flux;

public class FluxFactory {

  private FluxFactory() {
  }

  // In the meantime, re-purpose or augment DataInjectorArguments as necessary.
  // We will probably use InjectableType instead of GenerationType.
  public static <T> Flux<T> create(Optional<Integer> batchCount,
      long initialDelay,
      Duration interval,
      int batchSize,
      Supplier<T> supplier,
      Modifier modifier,
      Consumer<Iterable<T>> consumer,
      Consumer<? super Throwable> onError) {

    Flux<Long> flux;

    int totalCount;

    if (batchCount.isPresent()) {
      totalCount = batchCount.get() * batchSize;
      // Flux.range emits a range of integers from 1 to totalCount
      // provided interval. We use the provided interval divided by the batch size so that we can
      // later collect them up, which takes numToEmit * interval/numToEmit time, thus producing the
      // desired interval. The sequence completes immediately after the last value
      // {@code (start + count - 1)} has been reached.
      flux = Flux.range(1, totalCount)
          // We then add an initial delay so nothing is emitted until after than time has
          // finished, on the
          .delaySequence(Duration.ofMillis(initialDelay))
          // Then delay each element by the total interval / batch size (so that when they get
          // reassembled into a batch later
          // Convert the integers int longs so that it can match up with the other flux
          .map(Integer::longValue);
    } else {
      // Flux.interval emits sequential, increasing longs on the provided interval.  We use the
      // provided interval divided by the batch size so that we can later collect them up, which
      // takes
      // numToEmit * interval/numToEmit time, thus producing the desired interval.
      flux = Flux
          .interval(Duration.ofMillis(interval.toMillis() / batchSize));
    }

    // The supplier: convert the interval value into an object of the type being emitted,
    // using the json file to provide the initial object
    //flux.map(val -> readValue(mapper, arguments.getBase(), arguments.getType().getBaseClass()))
    return flux.map(l -> supplier.get())
        // Collect into a batch of numToEmit items (possibly 1) that will be put on the topic
        // Since this batches
        // by number of emits, it will force the data to be batched on the desired interval
        // The modifier: take the basic object and tweaks values so not all objects being
        // submitted to the topic are identical
        .map(t -> {
          if (t instanceof Iterable) {
            return (Iterable<T>) modifier.apply((Iterable<T>) t);
          } else {
            return (Iterable<T>) modifier.apply(List.of(t));
          }
        })
        // the buffer has effectively removed the delay between the individual items in the
        // list, so we can flatmap without changing the interval, so that we can then submit
        // them to Kafka individually
        .flatMap(Flux::fromIterable)
        // re-batch
        .buffer(batchSize)
        .delayElements(interval.dividedBy(batchSize))
        // Move the work being done to the current thread, causing it to block and prevent
        // program termination.  Flux.interval is on the compute scheduler, so it won't
        // block and the program will finish before anything happens if we don't move the
        // subscription.
        // Put each item on the kafka topic, or log an error if one has occurred somewhere in
        // the flux.
        .doOnNext(consumer)
        // Handle any errors.
        // Note that the likely errors are either from 1) the supplier being a
        // format other than json, or 2) the modifier expecting a data type other than that
        // produced by the supplier
        .doOnError(onError)
        .flatMap(Flux::fromIterable);
  }
}
