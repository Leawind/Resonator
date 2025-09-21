package io.github.leawind.resonator.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/// Test [ClassInspector]
@SuppressWarnings("unused")
public class ClassInspectorTest {

  @Test
  void testFieldsInspector() {
    ClassInspector<TestClass> inspector = ClassInspector.of(TestClass.class);

    // Test basic field counting
    assertEquals(5, inspector.fields().count());

    // Test filtering by annotation
    assertEquals(1, inspector.fields().withAnnotation(FieldAnno.class).count());

    // Test filtering by exact type
    assertEquals(3, inspector.fields().withType(String.class).count());
    assertEquals(1, inspector.fields().withType(Long.class).count());
    assertEquals(0, inspector.fields().withType(long.class).count());

    // Test filtering by assignable type
    assertEquals(4, inspector.fields().withTypeAssignableTo(Object.class).count());

    // Test filtering by modifiers
    assertEquals(1, inspector.fields().withModifiers(Modifier.STATIC).count());
    assertEquals(3, inspector.fields().withModifiers(Modifier.PUBLIC).count());
    assertEquals(
        2,
        inspector.fields().withModifiers(Modifier.PROTECTED).count()
            + inspector.fields().withModifiers(Modifier.PRIVATE).count());

    // Test finding any field
    Field anyField = inspector.fields().findAny();
    assertNotNull(anyField);

    // Test combining multiple filters
    assertEquals(
        2, inspector.fields().withType(String.class).withModifiers(Modifier.PUBLIC).count());
    assertEquals(
        1, inspector.fields().withAnnotation(FieldAnno.class).withTypeAssignableTo(String.class).count());

    // Test accessing field values without instance
    assertThrows(
        ClassInspector.InvalidClassException.class,
        () -> inspector.fields().withAnnotation(FieldAnno.class).getAny());

    // Test accessing field values with instance
    var instanceInspector = ClassInspector.of(new TestClass());
    assertNotNull(instanceInspector.fields().withAnnotation(FieldAnno.class).getAny());
  }

  @Test
  void testMethodsInspector() {
    ClassInspector<TestClass> inspector = ClassInspector.of(TestClass.class);

    assertEquals(7, inspector.methods().count());

    assertEquals(1, inspector.methods().withAnnotation(MethodAnno.class).count());

    assertEquals(4, inspector.methods().withReturnType(String.class).count());
    assertEquals(2, inspector.methods().withReturnType(void.class).count());
    assertEquals(5, inspector.methods().withReturnTypeLike(Object.class).count());

    assertEquals(2, inspector.methods().withModifiers(Modifier.STATIC).count());
    assertEquals(5, inspector.methods().withModifiers(Modifier.PUBLIC).count());

    assertEquals(1, inspector.methods().withName("annotatedMethod").count());

    Method anyMethod = inspector.methods().findAny();
    assertNotNull(anyMethod);

    assertEquals(
        1,
        inspector.methods().withAnnotation(MethodAnno.class).withReturnType(String.class).count());

    assertThrows(
        ClassInspector.InvalidClassException.class,
        () -> inspector.methods().withAnnotation(MethodAnno.class).getAny());

    ClassInspector<TestClass> instanceInspector = ClassInspector.of(new TestClass());
    ClassInspector.MethodInspector.MethodValue<Object> method =
        instanceInspector.methods().withAnnotation(MethodAnno.class).getAny();
    assertNotNull(method);

    Object result = method.apply(new Object[] {});
    assertEquals("test", result);
  }

  @Test
  void testMethodsInspectorGetValueOf() {
    // Test getAny method with instance
    var instanceInspector = ClassInspector.of(new TestClass());
    ClassInspector.MethodInspector.MethodValue<Object> annotatedMethod =
        instanceInspector.methods().withAnnotation(MethodAnno.class).getAny();
    assertNotNull(annotatedMethod);

    // Test invoking the method through MethodValue
    Object result = annotatedMethod.apply(new Object[] {});
    assertEquals("test", result);

    // Test getSingleOrThrow method
    ClassInspector.MethodInspector.MethodValue<Object> singleMethod =
        instanceInspector
            .methods()
            .withName("annotatedMethod")
            .getSingleOrThrow(i -> new RuntimeException());
    assertNotNull(singleMethod);
    assertEquals("test", singleMethod.apply(new Object[] {}));

    // Test getAny method without instance for static method
    var classInspector = ClassInspector.of(TestClass.class);
    ClassInspector.MethodInspector.MethodValue<Object> staticMethod =
        classInspector.methods().withName("staticMethod").getAny();
    assertNotNull(staticMethod);

    // Should be able to call static method without exception
    assertDoesNotThrow(() -> staticMethod.apply(new Object[] {}));

    // Test exception when accessing non-static method without instance
    assertThrows(
        ClassInspector.InvalidClassException.class,
        () -> classInspector.methods().withName("publicMethod").getAny());
  }

  @Test
  void testMethodsWithParameters() {
    var instanceInspector = ClassInspector.of(new TestClass());

    // Test non-static method with parameters
    ClassInspector.MethodInspector.MethodValue<Object> methodWithParams =
        instanceInspector.methods().withName("methodWithParameters").getAny();
    assertNotNull(methodWithParams);

    Object result = methodWithParams.apply(new Object[] {"Hello", 123});
    assertEquals("Hello123", result);

    // Test static method with parameters
    var classInspector = ClassInspector.of(TestClass.class);
    ClassInspector.MethodInspector.MethodValue<Object> staticMethodWithParams =
        classInspector.methods().withName("staticMethodWithParameters").getAny();
    assertNotNull(staticMethodWithParams);

    Object staticResult = staticMethodWithParams.apply(new Object[] {"Hello", "World"});
    assertEquals("Hello World", staticResult);

    // Test filtering by parameter types
    ClassInspector.MethodInspector.MethodValue<Object> specificMethod =
        instanceInspector.methods().withParametersType(String.class, int.class).getAny();
    assertNotNull(specificMethod);

    Object specificResult = specificMethod.apply(new Object[] {"Test", 42});
    assertEquals("Test42", specificResult);
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface FieldAnno {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface MethodAnno {}

  /// Test class with various field types for inspection
  ///
  /// Contains fields with different annotations, types and modifiers
  /// to thoroughly test the FieldsInspector functionality.
  static class TestClass {
    @FieldAnno public String annotatedField = "Hello world";

    public static final int STATIC_FIELD = 100;

    private final String privateField = "private";

    public String publicField = "public";

    protected Long protectedField = 1000L;

    // Methods for MethodInspector testing
    @MethodAnno
    public String annotatedMethod() {
      return "test";
    }

    public static void staticMethod() {
      // Do nothing
    }

    public void publicMethod() {
      // Do nothing
    }

    private String privateMethod() {
      return "private";
    }

    protected Object protectedMethod() {
      return new Object();
    }

    // Method with parameters for testing
    public String methodWithParameters(String input, int number) {
      return input + number;
    }

    public static String staticMethodWithParameters(String prefix, String suffix) {
      return prefix + " " + suffix;
    }
  }
}
