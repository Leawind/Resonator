package io.github.leawind.resonator.core.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/// Utility class for inspecting class fields and their values

public class ClassInspector<T> {
  private final Class<T> clazz;
  private final @Nullable T instance;

  private ClassInspector(Class<T> clazz, @Nullable T instance) {
    this.clazz = clazz;
    this.instance = instance;
  }

  /// Get a fields inspector for this class
  ///
  /// Including both static and instance fields
  ///
  /// ### Returns
  ///
  /// A new [FieldInspector] instance for inspecting fields of this class
  public FieldInspector<Object> fields() {
    return new FieldInspector<>();
  }

  /// Get a methods inspector for this class
  ///
  /// Including both static and instance methods
  ///
  /// ### Returns
  ///
  /// A new [MethodInspector] instance for inspecting methods of this class
  public MethodInspector<Object> methods() {
    return new MethodInspector<>();
  }

  /// Create a ClassInspector for a class
  ///
  /// ### Params
  ///
  /// - `clazz`: The class to inspect
  ///
  /// ### Returns
  ///
  /// A new [ClassInspector] instance for the given class
  public static <T> ClassInspector<T> of(Class<T> clazz) {
    return of(clazz, null);
  }

  /// Create a ClassInspector for an instance
  ///
  /// ### Params
  ///
  /// - `instance`: The instance to inspect
  ///
  /// ### Returns
  ///
  /// A new [ClassInspector] instance for the given instance
  public static <T> ClassInspector<T> of(T instance) {
    return of(TypeUtils.forceCast(Objects.requireNonNull(instance).getClass()), instance);
  }

  /// Create a ClassInspector
  ///
  /// ### Params
  ///
  /// - `clazz`: The class of the instance
  /// - `instance`: The instance to inspect
  ///
  /// ### Returns
  ///
  /// A new [ClassInspector] instance for the given instance
  public static <T> ClassInspector<T> of(Class<T> clazz, @Nullable T instance) {
    return new ClassInspector<>(clazz, instance);
  }

  public abstract static class Inspector<
          Item extends AccessibleObject & Member, Value, Self extends Inspector<Item, Value, Self>>
      implements Iterable<Item> {

    public interface Requisite<T> {
      void accept(T item) throws Throwable;
    }

    private final List<Predicate<Item>> conditions = new ArrayList<>();
    private final List<Requisite<Item>> requisites = new ArrayList<>();

    protected Inspector() {}

    protected List<Predicate<Item>> conditions() {
      return conditions;
    }

    protected List<Requisite<Item>> requisites() {
      return requisites;
    }

    protected abstract Item[] raw();

    public abstract Value getValueOf(Item item) throws InvalidClassException;

    protected Stream<Item> stream() {
      return Arrays.stream(raw())
          .filter(conditions.stream().reduce(x -> true, Predicate::and))
          .peek(
              item ->
                  requisites.forEach(
                      requisite -> {
                        try {
                          requisite.accept(item);
                        } catch (Throwable e) {
                          throw new RuntimeException(e);
                        }
                      }));
    }

    @Override
    public @Nonnull Iterator<Item> iterator() {
      return stream().iterator();
    }

    /// Add a condition to filter items
    ///
    /// ### Params
    ///
    /// - `condition`: A predicate to test items
    ///
    /// ### Returns
    ///
    /// This inspector instance for method chaining

    public Self with(Predicate<Item> condition) {
      conditions.add(condition);
      return TypeUtils.forceCast(this);
    }

    public Self withConditionsFrom(Self source) {
      source.conditions().forEach(this::with);
      return TypeUtils.forceCast(this);
    }

    /// Add a condition to filter items with specific annotation
    ///
    /// ### Params
    ///
    /// - `annotationClass`: The annotation class to filter by
    ///
    /// ### Returns
    ///
    /// This inspector instance for method chaining
    public Self withAnnotation(Class<? extends Annotation> annotationClass) {
      return with(item -> item.isAnnotationPresent(annotationClass));
    }

    /// Add a condition to filter items with specific annotation
    ///
    /// ### Params
    ///
    /// - `annotationClass`: The annotation class to filter by
    ///
    /// ### Returns
    ///
    /// This inspector instance for method chaining
    public <A extends Annotation> Self withAnnotation(
        Class<A> annotationClass, Predicate<A> predicate) {
      return with(
          item -> {
            A a = item.getDeclaredAnnotation(annotationClass);
            return a != null && predicate.test(a);
          });
    }

    /// Add a condition to filter items with specific modifiers
    ///
    /// ### Params
    ///
    /// - `modifiers`: The modifiers to filter by
    ///
    /// ### Returns
    ///
    /// This inspector instance for method chaining
    public Self withModifiers(int modifiers) {
      return with(item -> (item.getModifiers() & modifiers) != 0);
    }

    /// Add a condition to filter static or instance items
    ///
    /// ### Params
    ///
    /// - `isStatic`: True to filter static items, false to filter instance items
    ///
    /// ### Returns
    ///
    /// This inspector instance for method chaining
    public Self withStatic(boolean isStatic) {
      return with(item -> Modifier.isStatic(item.getModifiers()) == isStatic);
    }

    /// Add a condition to filter items by name
    ///
    /// ### Params
    ///
    /// - `name`: The name to filter by
    ///
    /// ### Returns
    ///
    /// This inspector instance for method chaining
    public Self withName(String name) {
      return with(item -> Objects.equals(item.getName(), name));
    }

    public Self expect(Requisite<Item> requisite) {
      requisites.add(requisite);
      return TypeUtils.forceCast(this);
    }

    public Self expect(Predicate<Item> predicate, Function<Item, Throwable> mapper) {
      return expect(
          item -> {
            if (!predicate.test(item)) {
              throw mapper.apply(item);
            }
          });
    }

    public Self expectRequisiteFrom(Self source) {
      source.requisites().forEach(this::expect);
      return TypeUtils.forceCast(this);
    }

    public Self expectAnnotation(
        Class<? extends Annotation> annotationClass, Function<Item, Throwable> mapper) {
      return expect(item -> item.isAnnotationPresent(annotationClass), mapper);
    }

    public <A extends Annotation> Self expectAnnotation(
        Class<A> annotationClass, Predicate<A> predicate, Function<Item, Throwable> mapper) {
      return expect(
          item -> {
            A a = item.getDeclaredAnnotation(annotationClass);
            return a != null && predicate.test(a);
          },
          mapper);
    }

    public Self expectModifiers(int modifiers, Function<Item, Throwable> mapper) {
      return expect(item -> (item.getModifiers() & modifiers) != 0, mapper);
    }

    public Self expectStatic(boolean isStatic, Function<Item, Throwable> mapper) {
      return expect(item -> Modifier.isStatic(item.getModifiers()) != isStatic, mapper);
    }

    public Self expectName(String name, Function<Item, Throwable> mapper) {
      return expect(item -> Objects.equals(item.getName(), name), mapper);
    }

    /// Get the count of items matching all conditions
    ///
    /// ### Returns
    ///
    /// The count of matching items
    public long count() {
      return stream().count();
    }

    /// Find any item matching all conditions
    ///
    /// ### Returns
    ///
    /// A matching item, or null if none found
    public @Nullable Item findAny() {
      return stream().findFirst().orElse(null);
    }

    public @Nullable <E extends Throwable> Item findSingleOrThrow(Function<List<Item>, E> mapper)
        throws E {
      var results = stream().toList();

      if (results.size() > 1) {
        throw mapper.apply(results);
      }

      if (results.isEmpty()) {
        return null;
      }

      return results.get(0);
    }

    /// Get values of all items matching all conditions
    ///
    /// ### Returns
    ///
    /// An iterator over values of all matching items
    ///
    /// ### Throws
    ///
    /// - `InvalidClassException`: If accessing item values fails
    public Stream<Value> getAll() throws InvalidClassException {
      return stream().map(this::getValueOf);
    }

    /// Get value of any item matching all conditions
    ///
    /// ### Returns
    ///
    /// The value of a matching item, or null if none found
    ///
    /// ### Throws
    ///
    /// - `InvalidClassException`: If accessing item values fails
    public @Nullable Value getAny() throws InvalidClassException {
      Item item = findAny();
      return item == null ? null : getValueOf(item);
    }

    /// Get value of a single item matching all conditions, throw if more than one found
    ///
    /// ### Returns
    ///
    /// The value of the single matching item
    ///
    /// ### Throws
    ///
    /// - `IllegalStateException`: If more than one item is found
    /// - `InvalidClassException`: If calculating item values fails (see [Inspector#getValueOf])
    public @Nullable <E extends Throwable> Value getSingleOrThrow(Function<List<Item>, E> mapper)
        throws E, InvalidClassException {
      Item item = findSingleOrThrow(mapper);
      return item == null ? null : getValueOf(item);
    }

    public abstract Self copy();
  }

  /// Utility class for inspecting fields of a class
  ///
  /// Provides methods to filter and access fields of the inspected class.
  /// Use [ClassInspector#fields()] to get an instance of this class.
  public class FieldInspector<Value> extends Inspector<Field, Value, FieldInspector<Value>> {
    private FieldInspector() {}

    @Override
    protected Field[] raw() {
      return clazz.getDeclaredFields();
    }

    @Override
    public @Nullable Value getValueOf(@Nullable Field field) throws InvalidClassException {
      if (field == null) {
        return null;
      }
      int modifiers = field.getModifiers();
      boolean isStatic = Modifier.isStatic(modifiers);

      if (instance == null && !isStatic) {
        throw new InvalidClassException(
            String.format("Accessing non-static field '%s' without instance", field.getName()));
      }

      field.setAccessible(true);

      try {
        return TypeUtils.forceCast(field.get(isStatic ? null : instance));
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public FieldInspector<Value> copy() {
      return new FieldInspector<Value>().withConditionsFrom(this).expectRequisiteFrom(this);
    }

    /// Filter fields by exact type
    ///
    /// ### Params
    ///
    /// - `type`: The type to filter by
    ///
    /// ### Returns
    ///
    /// This [FieldInspector] instance for method chaining

    public <V extends Value> FieldInspector<V> withType(Class<V> type) {
      return TypeUtils.forceCast(with(field -> type.equals(field.getType())));
    }

    /// Filter fields by assignable type
    ///
    /// ### Params
    ///
    /// - `type`: The type to filter by
    ///
    /// ### Returns
    ///
    /// This [FieldInspector] instance for method chaining

    public <V extends Value> FieldInspector<V> withTypeAssignableTo(Class<V> type) {
      return TypeUtils.forceCast(with(field -> type.isAssignableFrom(field.getType())));
    }

    public <V extends Value> FieldInspector<V> withTypeAssignableFrom(Class<V> type) {
      return TypeUtils.forceCast(with(field -> field.getType().isAssignableFrom(type)));
    }

    public <V extends Value> FieldInspector<V> expectType(
        Class<V> type, Function<Field, Throwable> mapper) {
      return TypeUtils.forceCast(
          expect(
              field -> {
                if (!type.equals(field.getType())) throw mapper.apply(field);
              }));
    }

    public <V extends Value> FieldInspector<V> expectTypeAssignableTo(
        Class<V> type, Function<Field, Throwable> mapper) {
      return TypeUtils.forceCast(
          expect(
              field -> {
                if (!type.isAssignableFrom(field.getType())) throw mapper.apply(field);
              }));
    }

    public <V extends Value> FieldInspector<V> expectTypeAssignableFrom(
        Class<V> type, Function<Field, Throwable> mapper) {
      return TypeUtils.forceCast(
          expect(
              field -> {
                if (!field.getType().isAssignableFrom(type)) throw mapper.apply(field);
              }));
    }
  }

  /// Utility class for inspecting methods of a class
  ///
  /// Provides methods to filter and access methods of the inspected class.
  /// Use [ClassInspector#methods()] to get an instance of this class.
  public class MethodInspector<Result>
      extends Inspector<Method, MethodInspector.MethodValue<Result>, MethodInspector<Result>> {

    public interface MethodValue<R> {
      R apply(Object... args);
    }

    private MethodInspector() {}

    @Override
    protected Method[] raw() {
      return clazz.getDeclaredMethods();
    }

    @Override
    public MethodInspector.MethodValue<Result> getValueOf(Method method)
        throws InvalidClassException {
      if (method == null) {
        return null;
      }

      int modifiers = method.getModifiers();
      boolean isStatic = Modifier.isStatic(modifiers);

      if (instance == null && !isStatic) {
        throw new InvalidClassException(
            String.format("Accessing non-static method '%s' without instance", method.getName()));
      }

      method.setAccessible(true);

      T runtimeInstance = isStatic ? null : instance;

      return (Object... args) -> {
        try {
          return TypeUtils.forceCast(method.invoke(runtimeInstance, args));
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      };
    }

    @Override
    public MethodInspector<Result> copy() {
      return new MethodInspector<Result>().withConditionsFrom(this).expectRequisiteFrom(this);
    }

    /// Add a condition to filter methods by return type
    ///
    /// ### Params
    ///
    /// - `type`: The return type to filter by
    ///
    /// ### Returns
    ///
    /// This [MethodInspector] instance for method chaining

    public <R extends Result> MethodInspector<R> withReturnType(Class<R> type) {
      return TypeUtils.forceCast(with(method -> type.equals(method.getReturnType())));
    }

    public <R extends Result> MethodInspector<R> withReturnType(Class<R>... types) {
      return TypeUtils.forceCast(
          with(
              method ->
                  Arrays.stream(types).anyMatch(type -> type.equals(method.getReturnType()))));
    }

    /// Add a condition to filter methods by assignable return type
    ///
    /// ### Params
    ///
    /// - `type`: The return type to filter by
    ///
    /// ### Returns
    ///
    /// This [MethodInspector] instance for method chaining

    public <R extends Result> MethodInspector<R> withReturnTypeLike(Class<R> type) {
      return TypeUtils.forceCast(with(method -> type.isAssignableFrom(method.getReturnType())));
    }

    public <R extends Result> MethodInspector<R> withReturnTypeLike(Class<R>... types) {
      return TypeUtils.forceCast(
          with(
              method ->
                  Arrays.stream(types)
                      .anyMatch(type -> type.isAssignableFrom(method.getReturnType()))));
    }

    public MethodInspector<Result> withParameterCount(int count) {
      return with(method -> method.getParameterCount() == count);
    }

    public MethodInspector<Result> withParameterType(int index, Class<?> parameterType) {
      return with(
          method ->
              index < method.getParameterCount()
                  && method.getParameterTypes()[index] == parameterType);
    }

    /// Add a condition to filter methods by parameter types
    ///
    /// ### Params
    ///
    /// - `parameterTypes`: The parameter types to filter by
    ///
    /// ### Returns
    ///
    /// This [MethodInspector] instance for method chaining
    public MethodInspector<Result> withParametersType(Class<?>... parameterTypes) {
      return with(method -> Arrays.equals(method.getParameterTypes(), parameterTypes));
    }

    public <R extends Result> MethodInspector<R> expectReturnType(
        Class<R> type, Function<Method, Throwable> mapper) {
      return TypeUtils.forceCast(expect(method -> type.equals(method.getReturnType()), mapper));
    }

    public <R extends Result> MethodInspector<R> expectReturnType(
        Function<Method, Throwable> mapper, Class<R>... types) {
      return TypeUtils.forceCast(
          expect(
              method -> Arrays.stream(types).anyMatch(type -> type.equals(method.getReturnType())),
              mapper));
    }

    public <R extends Result> MethodInspector<R> expectReturnTypeLike(
        Class<R> type, Function<Method, Throwable> mapper) {
      return TypeUtils.forceCast(
          expect(method -> type.isAssignableFrom(method.getReturnType()), mapper));
    }

    public MethodInspector<Result> expectReturnTypeLike(
        Function<Method, Throwable> mapper, Class<?>... types) {
      return expect(
          method ->
              Arrays.stream(types).anyMatch(type -> type.isAssignableFrom(method.getReturnType())),
          mapper);
    }

    public MethodInspector<Result> expectParameterCount(
        int count, Function<Method, Throwable> mapper) {
      return expect(method -> method.getParameterCount() == count, mapper);
    }

    public MethodInspector<Result> expectParameterType(
        int index, Class<?> parameterType, Function<Method, Throwable> mapper) {
      return expect(
          method ->
              index < method.getParameterCount()
                  && method.getParameterTypes()[index] == parameterType,
          mapper);
    }

    public MethodInspector<Result> expectParametersType(
        Function<Method, Throwable> mapper, Class<?>... types) {
      return expect(method -> Arrays.equals(method.getParameterTypes(), types), mapper);
    }
  }

  /// Exception thrown when class inspection fails
  public static class InvalidClassException extends RuntimeException {

    InvalidClassException(String message) {
      super(message);
    }
  }
}
