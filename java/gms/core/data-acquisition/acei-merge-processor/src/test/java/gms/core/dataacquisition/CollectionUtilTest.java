package gms.core.dataacquisition;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.*;
import org.junit.jupiter.api.Test;

class CollectionUtilTest {

  @Test
  void testPartitionSet() {
    // Test partitioning of an empty set.
    Set<String> emptySet = Collections.emptySet();
    List<Set<String>> emptySetPartitions = CollectionUtil.partitionSet(emptySet, 5);
    assertEquals(0, emptySetPartitions.size());

    // Test partitioning of a singleton set.
    Set<String> singletonSet = Collections.singleton("hello");
    List<Set<String>> singletonPartitions = CollectionUtil.partitionSet(singletonSet, 5);
    assertEquals(1, singletonPartitions.size());
    assertEquals(singletonSet, singletonPartitions.iterator().next());

    // Test partitioning of a set of less than the max size for each partition.
    Set<String> smallSet = new HashSet<>(Arrays.asList("one two three four".split(" ")));
    assertEquals(4, smallSet.size());
    List<Set<String>> smallSetPartitions = CollectionUtil.partitionSet(smallSet, 5);
    assertEquals(1, smallSetPartitions.size());
    assertEquals(smallSet, smallSetPartitions.iterator().next());

    // Test partitioning of a larger set
    Set<String> largeSet = new HashSet<>(Arrays.asList(("one two three four five six seven eight " +
        "nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eightteen").split(" ")));
    assertEquals(18, largeSet.size());
    List<Set<String>> largeSetPartitions = CollectionUtil.partitionSet(largeSet, 5);
    assertEquals(4, largeSetPartitions.size());
    Set<String> largeSet2 = new HashSet<>();
    for (Set<String> set: largeSetPartitions) {
      assertTrue(Sets.intersection(set, largeSet2).isEmpty());
      assertTrue(set.size() <= 5);
      largeSet2.addAll(set);
    }
    assertEquals(largeSet, largeSet2);

  }
}