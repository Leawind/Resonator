package io.github.leawind.resonator.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class AtomicIntSequencerTest {

  @Test
  void testNext_IncrementingSequence() {
    // Given: starting from -1, increment by 1 each time, no filter
    AtomicIntSequencer generator = new AtomicIntSequencer(-1, i -> i + 1);

    // When: calling next()
    // Then: should return 0, 1, 2, ...
    assertEquals(0, generator.next());
    assertEquals(1, generator.next());
    assertEquals(2, generator.next());
    assertEquals(3, generator.next());
  }

  @Test
  void testNext_WithFilter() {
    // Given: only allow odd numbers
    Predicate<Integer> onlyOdd = i -> i % 2 == 1;
    Function<Integer, Integer> nextGetter = i -> i + 1;
    AtomicIntSequencer generator = new AtomicIntSequencer(0, nextGetter, onlyOdd);

    // When: calling next()
    // Then: skip even numbers and return 1, 3, 5, ...
    assertEquals(1, generator.next());
    assertEquals(3, generator.next());
    assertEquals(5, generator.next());
  }

  @Test
  void testNext_FilterIsNull_AllValuesAllowed() {
    // Given: filter is null (meaning no restrictions)
    Function<Integer, Integer> nextGetter = i -> i + 1;
    AtomicIntSequencer generator = new AtomicIntSequencer(10, nextGetter, null);

    // When: calling next()
    // Then: should increment normally
    assertEquals(11, generator.next());
    assertEquals(12, generator.next());
  }

  @Test
  void testNext_ThrowsExceptionWhenLimitExceeded() {
    // Given: a sequencer with a very restrictive filter and a small limit
    Predicate<Integer> neverSatisfied = i -> false; // This filter will never be satisfied
    Function<Integer, Integer> nextGetter = i -> i + 1;
    AtomicIntSequencer generator = new AtomicIntSequencer(0, nextGetter, neverSatisfied, 5);

    // When & Then: calling next() should throw NoSuchElementException
    assertThrows(NoSuchElementException.class, generator::next);
  }

  @Test
  void testNext_FilterIsNotNull_RespectsFilter() {
    // Given: only allow values greater than 100
    Predicate<Integer> greaterThan100 = i -> i > 100;
    Function<Integer, Integer> nextGetter = i -> i + 1;
    AtomicIntSequencer generator = new AtomicIntSequencer(90, nextGetter, greaterThan100);

    // When: calling next()
    // Then: keep incrementing until reaching 101
    assertEquals(101, generator.next());
  }

  @Test
  void testCreateRanged_InRangeSequence() {
    // Given: range [10, 15) → 10,11,12,13,14
    AtomicIntSequencer generator = AtomicIntSequencer.createRanged(10, 15);

    // When: calling next()
    // Then: cycle through values
    assertEquals(11, generator.next());
    assertEquals(12, generator.next());
    assertEquals(13, generator.next());
    assertEquals(14, generator.next());
    assertEquals(10, generator.next()); // cycles back to 10
  }

  @Test
  void testCreateRanged_WithFilter() {
    // Given: range [1, 4), only allow odd numbers (1,3)
    Predicate<Integer> onlyOdd = i -> i % 2 == 1;
    AtomicIntSequencer generator = AtomicIntSequencer.createRanged(0, 1, 4, onlyOdd, 4);

    // When: calling next()
    // Then: return 1, 3, 1, 3, ...
    assertEquals(1, generator.next());
    assertEquals(3, generator.next());
    assertEquals(1, generator.next());
    assertEquals(3, generator.next());
  }

  @Test
  void testConcurrent_Next_NoFilter() throws InterruptedException {
    // Given: multiple threads concurrently acquiring IDs
    Function<Integer, Integer> nextGetter = i -> i + 1;
    AtomicIntSequencer generator = new AtomicIntSequencer(-1, nextGetter, null);

    Set<Integer> generatedIds = ConcurrentHashMap.newKeySet();
    int threadCount = 10;
    int iterationsPerThread = 100;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);

    // When: concurrent calls to next()
    for (int t = 0; t < threadCount; t++) {
      executor.submit(
          () -> {
            try {
              startLatch.await(); // start simultaneously
              for (int i = 0; i < iterationsPerThread; i++) {
                int id = generator.next();
                assertTrue(generatedIds.add(id), "Duplicate ID detected: " + id);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              endLatch.countDown();
            }
          });
    }

    startLatch.countDown(); // start
    assertTrue(endLatch.await(5, TimeUnit.SECONDS), "Threads did not finish in time");

    // Then: total count is correct, no duplicates
    assertEquals(threadCount * iterationsPerThread, generatedIds.size());
  }

  @Test
  void testConcurrent_Next_WithFilter() throws InterruptedException {
    // Given: concurrency with odd-number filtering
    Function<Integer, Integer> nextGetter = i -> i + 1;
    Predicate<Integer> onlyOdd = i -> i % 2 == 1;
    AtomicIntSequencer generator = new AtomicIntSequencer(0, nextGetter, onlyOdd);

    Set<Integer> generatedIds = ConcurrentHashMap.newKeySet();
    int threadCount = 5;
    int iterationsPerThread = 50;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);

    for (int t = 0; t < threadCount; t++) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              for (int i = 0; i < iterationsPerThread; i++) {
                int id = generator.next();
                assertEquals(1, id % 2, "Generated even ID: " + id);
                assertTrue(generatedIds.add(id), "Duplicate ID: " + id);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              endLatch.countDown();
            }
          });
    }

    startLatch.countDown();
    assertTrue(endLatch.await(3, TimeUnit.SECONDS));

    // All IDs are odd, no duplicates
    generatedIds.forEach(id -> assertEquals(1, id % 2));
    assertEquals(threadCount * iterationsPerThread, generatedIds.size());
  }

  @Test
  void testConcurrent_createRangedWithFilter() throws InterruptedException {
    // Given: cyclic range [0,3), only allow value 1
    Predicate<Integer> onlyOne = i -> i == 1;
    AtomicIntSequencer generator = AtomicIntSequencer.createRanged(0, 3, onlyOne);

    Set<Integer> seen = ConcurrentHashMap.newKeySet();
    int threadCount = 5;
    int iterations = 20;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);

    for (int t = 0; t < threadCount; t++) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              for (int i = 0; i < iterations; i++) {
                int id = generator.next();
                assertEquals(1, id);
                seen.add(id);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              endLatch.countDown();
            }
          });
    }

    startLatch.countDown();
    assertTrue(endLatch.await(3, TimeUnit.SECONDS));

    assertEquals(Set.of(1), seen);
  }

  @Test
  void testInitialState_Respected() {
    Function<Integer, Integer> nextGetter = i -> i + 1;
    AtomicIntSequencer generator = new AtomicIntSequencer(100, nextGetter, null);

    // The first next() call should be based on the initial value 100
    assertEquals(101, generator.next());
  }
}
