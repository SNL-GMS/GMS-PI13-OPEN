package gms.core.dataacquisition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import reactor.core.publisher.Flux;

/**
 * Contains static utility methods pertaining to collections that are not found in
 * {@link java.util.Collections} or guava. This should eventually be
 * moved into a shared frameworks class.
 */
public final class CollectionUtil {

  /**
   * Attempts to partition a set into subsets of the specified maximum size.
   * @param set
   * @param maxSize
   * @param <T>
   * @return a list of subsets containing the elements of the passed in set with each
   *   subset containing at most maxSize number of elements.
   */
  public static <T> List<Set<T>> partitionSet(Set<T> set, int maxSize) {
    Objects.requireNonNull(set, "set must not be null");
    if (maxSize <= 0) {
      throw new IllegalArgumentException("maxSize must be greater than 0: " + maxSize);
    }
    List<Set<T>> partitions = new ArrayList<>();
    Flux.fromIterable(set)
        .buffer(maxSize)
        .subscribe(lst -> partitions.add(new HashSet<>(lst)));
    return partitions;
  }
}
