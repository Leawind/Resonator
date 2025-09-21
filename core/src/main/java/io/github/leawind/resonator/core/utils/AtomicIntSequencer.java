package io.github.leawind.resonator.core.utils;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;

public final class AtomicIntSequencer {
  private final long limit;
  private final AtomicInteger lastValue;
  private final Function<Integer, Integer> nextValueFunction;
  @Nullable private final Predicate<Integer> filter;

  public AtomicIntSequencer(int initialValue, Function<Integer, Integer> nextValueFunction) {
    this(initialValue, nextValueFunction, null, -1);
  }

  public AtomicIntSequencer(
      int initialValue,
      Function<Integer, Integer> nextValueFunction,
      @Nullable Predicate<Integer> filter) {
    this(initialValue, nextValueFunction, filter, -1);
  }

  /// ### Params
  ///
  /// - `initialValue` The initial value to start sequencing from
  /// - `nextValueFunction` Function that computes the next candidate value from the current one
  /// - `filter` Optional predicate to validate generated values; if null, all values are
  /// accepted
  /// - `limit` Optional limit to the number of generated values; `0` or negative for no limit
  public AtomicIntSequencer(
      int initialValue,
      Function<Integer, Integer> nextValueFunction,
      @Nullable Predicate<Integer> filter,
      long limit) {
    this.lastValue = new AtomicInteger(initialValue);
    this.nextValueFunction = nextValueFunction;
    this.filter = filter;
    this.limit = limit;
  }

  /// Atomically generate and return the next valid value in the sequence.
  ///
  /// Continuously applies `nextValueFunction()` to the current value until either:
  ///
  /// - No filter is set (`filter == null`)
  /// - A value is produced that satisfies the `filter` predicate
  ///
  /// ### Return
  ///
  /// The next valid value in the sequence
  ///
  /// ### Throws
  ///
  /// - [NoSuchElementException] if no valid value can be generated within the limit (if set)
  public int next() throws NoSuchElementException {
    return lastValue.updateAndGet(
        (i) -> {
          long count = 0;
          do {
            i = nextValueFunction.apply(i);
            count++;
            if (limit > 0 && count > limit) {
              throw new NoSuchElementException("Limit exceeded");
            }
          } while (filter != null && !filter.test(i));
          return i;
        });
  }

  /// Creates a cyclic sequencer over the half-open range `[low, high)`
  ///
  /// Limit is set to `high - low + 1`.
  ///
  /// ### Params
  ///
  /// - `low` inclusive lower bound of the range
  /// - `high` exclusive upper bound of the range
  ///
  /// ### Return
  ///
  /// a sequencer that cycles between `low` and `high - 1`
  public static AtomicIntSequencer createRanged(int low, int high) {
    return createRanged(low, high, null);
  }

  /// Creates a cyclic sequencer over the half-open range `[low, high)`
  ///
  /// Internally uses `lastValue = low` as the starting point.
  ///
  /// Limit is set to `high - low + 1`.
  ///
  /// ### Params
  ///
  /// - `low` inclusive lower bound of the range
  /// - `high` exclusive upper bound of the range
  /// - `filter` optional predicate to filter accepted values; may be null
  ///
  /// ### Return
  ///
  /// a sequencer that cycles between `low` and `high - 1`
  public static AtomicIntSequencer createRanged(
      int low, int high, @Nullable Predicate<Integer> filter) {
    return createRanged(low, low, high, filter, high - low + 1);
  }

  /// Creates a cyclic sequencer with a custom starting point within the range` [low, high)`.
  ///
  /// ### Params
  ///
  /// - `initialValue` the initial value to start from
  /// - `low` inclusive lower bound of the cyclic range
  /// - `high` exclusive upper bound of the cyclic range
  /// - `filter` optional predicate to filter accepted values; may be null
  /// - `limit` optional limit to the number of generated values; `0` or negative for no limit
  ///
  /// ### Return
  ///
  /// a sequencer that cycles through the specified range
  public static AtomicIntSequencer createRanged(
      int initialValue, int low, int high, @Nullable Predicate<Integer> filter, long limit) {
    return new AtomicIntSequencer(
        initialValue, (i) -> ((i - low + 1) % (high - low)) + low, filter, limit);
  }
}
