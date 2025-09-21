package io.github.leawind.resonator.server.utils;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.persistence.Persistence;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.QueryFactory;
import com.googlecode.cqengine.resultset.ResultSet;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/// An extended version of [ConcurrentIndexedCollection] with additional utility methods
public class ConcurrentIndexedCollectionExtended<O> extends ConcurrentIndexedCollection<O> {
  /// Default constructor
  public ConcurrentIndexedCollectionExtended() {
    super();
  }

  /// Constructor with persistence
  ///
  /// ### Parameters
  ///
  /// - `persistence`: The persistence configuration
  public ConcurrentIndexedCollectionExtended(Persistence<O, ? extends Comparable> persistence) {
    super(persistence);
  }

  /// Execute a query and close the result set automatically
  ///
  /// ### Parameters
  ///
  /// - `query`: The query to execute
  ///
  /// ### Returns
  ///
  /// A list of objects matching the query
  public List<O> queryClose(Query<O> query) {
    try (ResultSet<O> results = retrieve(query)) {
      return results.stream().toList();
    }
  }

  /// Execute a query by attribute and close the result set automatically
  ///
  /// ### Parameters
  ///
  /// - `attribute`: The attribute to query
  /// - `attributeValue`: The value of the attribute to match
  ///
  /// ### Returns
  ///
  /// A list of objects matching the query
  public <A> List<O> queryClose(@Nonnull Attribute<O, A> attribute, @Nonnull A attributeValue) {
    return queryClose(QueryFactory.equal(attribute, attributeValue));
  }

  /// Execute a query and return the first result, closing the result set automatically
  ///
  /// ### Parameters
  ///
  /// - `query`: The query to execute
  ///
  /// ### Returns
  ///
  /// The first object matching the query, or null if no matches found
  public @Nullable O queryFirstClose(Query<O> query) {
    try (ResultSet<O> results = retrieve(query)) {
      for (O obj : results) {
        return obj;
      }
      return null;
    }
  }

  /// Execute a query by attribute and return the first result, closing the result set automatically
  ///
  /// ### Parameters
  ///
  /// - `attribute`: The attribute to query
  /// - `attributeValue`: The value of the attribute to match
  ///
  /// ### Returns
  ///
  /// The first object matching the query, or null if no matches found
  public @Nullable <A> O queryFirstClose(
      @Nonnull Attribute<O, A> attribute, @Nonnull A attributeValue) {
    return queryFirstClose(QueryFactory.equal(attribute, attributeValue));
  }

  /// Check if no objects match the given query
  ///
  /// ### Parameters
  ///
  /// - `query`: The query to check
  ///
  /// ### Returns
  ///
  /// True if no objects match the query, false otherwise
  public boolean unexists(Query<O> query) {
    return queryFirstClose(query) == null;
  }

  /// Check if no objects match the given attribute query
  ///
  /// ### Parameters
  ///
  /// - `attribute`: The attribute to query
  /// - `attributeValue`: The value of the attribute to match
  ///
  /// ### Returns
  ///
  /// True if no objects match the query, false otherwise
  public <A> boolean unexists(@Nonnull Attribute<O, A> attribute, @Nonnull A attributeValue) {
    return unexists(QueryFactory.equal(attribute, attributeValue));
  }
}
