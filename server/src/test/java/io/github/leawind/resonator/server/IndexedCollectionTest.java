package io.github.leawind.resonator.server;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.unique.UniqueIndex;
import com.googlecode.cqengine.query.QueryFactory;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

public class IndexedCollectionTest {

  record Cat(int id, long birthDay, @Nullable String name) {
    static final Attribute<Cat, Integer> ID =
        QueryFactory.attribute(Cat.class, Integer.class, "id", Cat::id);
    static final Attribute<Cat, Long> BIRTH_DAY =
        QueryFactory.attribute(Cat.class, Long.class, "birthDay", Cat::birthDay);
    static final Attribute<Cat, String> NAME =
        QueryFactory.attribute(Cat.class, String.class, "name", Cat::name);
    static final Attribute<Cat, String> NULLABLE_NAME =
        QueryFactory.nullableAttribute(Cat.class, String.class, "name", Cat::name);
  }

  @Test
  void testIndexedCollection() {
    IndexedCollection<Cat> collection = new ConcurrentIndexedCollection<>();

    collection.addIndex(UniqueIndex.onAttribute(Cat.ID));
    collection.addIndex(HashIndex.onAttribute(Cat.BIRTH_DAY));
    collection.addIndex(HashIndex.onAttribute(Cat.NAME));

    collection.add(new Cat(1, 1000L, "Mimi"));
    collection.add(new Cat(2, 1000L, "Mimi"));

    try {
      collection.add(new Cat(1, 2000L, "Dajv"));
      throw new RuntimeException("Should not be able to add duplicate ID");
    } catch (UniqueIndex.UniqueConstraintViolatedException e) {
      System.out.println("Error: " + e.getMessage());
    }

    try (var re = collection.retrieve(QueryFactory.equal(Cat.NAME, "Mimi"))) {
      re.forEach(System.out::println);
    }
  }

  @Test
  void testNull() {
    IndexedCollection<Cat> collection = new ConcurrentIndexedCollection<>();

    collection.addIndex(UniqueIndex.onAttribute(Cat.ID));
    collection.addIndex(HashIndex.onAttribute(Cat.BIRTH_DAY));
    collection.addIndex(HashIndex.onAttribute(Cat.NULLABLE_NAME));

    collection.add(new Cat(0, 2020L, "Dajv"));
    collection.add(new Cat(1, 2020L, "Mimi"));
    collection.add(new Cat(2, 2021L, "Mimi"));

    collection.add(new Cat(3, 2020L, null));

    try (var re =
        collection.retrieve(
            QueryFactory.and(
                QueryFactory.equal(Cat.ID, 3),
                QueryFactory.equal(Cat.BIRTH_DAY, 2020L),
                QueryFactory.not(QueryFactory.has(Cat.NULLABLE_NAME))))) {
      re.forEach(System.out::println);
    }
  }
}
