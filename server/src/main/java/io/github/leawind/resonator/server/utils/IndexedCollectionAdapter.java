package io.github.leawind.resonator.server.utils;

import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.QueryFactory;
import com.googlecode.cqengine.resultset.ResultSet;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/// Adapter class for [IndexedCollection] with additional utility methods
public class IndexedCollectionAdapter<O> {
  /// The underlying indexed collection
  public final IndexedCollection<O> collection;

  /// Constructor
  ///
  /// ### Parameters
  ///
  /// - `collection`: The indexed collection to wrap
  public IndexedCollectionAdapter(IndexedCollection<O> collection) {
    this.collection = collection;
  }

  /// Execute a query on the underlying collection
  ///
  /// ### Parameters
  ///
  /// - `query`: The query to execute
  ///
  /// ### Returns
  ///
  /// A list of objects matching the query
  public List<O> query(Query<O> query) {
    try (ResultSet<O> results = collection.retrieve(query)) {
      return results.stream().toList();
    }
  }

  /// Execute a query by attribute on the underlying collection
  ///
  /// ### Parameters
  ///
  /// - `attribute`: The attribute to query
  /// - `attributeValue`: The value of the attribute to match
  ///
  /// ### Returns
  ///
  /// A list of objects matching the query
  public <A> List<O> query(@Nonnull Attribute<O, A> attribute, @Nonnull A attributeValue) {
    return query(QueryFactory.equal(attribute, attributeValue));
  }

  /// Execute a query and return the first result from the underlying collection
  ///
  /// ### Parameters
  ///
  /// - `query`: The query to execute
  ///
  /// ### Returns
  ///
  /// The first object matching the query, or null if no matches found
  public @Nullable O queryFirst(Query<O> query) {
    try (ResultSet<O> results = collection.retrieve(query)) {
      for (O obj : results) {
        return obj;
      }
      return null;
    }
  }

  /// Execute a query by attribute and return the first result from the underlying collection
  ///
  /// ### Parameters
  ///
  /// - `attribute`: The attribute to query
  /// - `attributeValue`: The value of the attribute to match
  ///
  /// ### Returns
  ///
  /// The first object matching the query, or null if no matches found
  public @Nullable <A> O queryFirst(@Nonnull Attribute<O, A> attribute, @Nonnull A attributeValue) {
    return queryFirst(QueryFactory.equal(attribute, attributeValue));
  }

  /// Check if any objects match the given query
  ///
  /// ### Parameters
  ///
  /// - `query`: The query to check
  ///
  /// ### Returns
  ///
  /// True if any objects match the query, false otherwise
  public boolean exists(Query<O> query) {
    return queryFirst(query) != null;
  }

  /// Check if any objects match the given attribute query
  ///
  /// ### Parameters
  ///
  /// - `attribute`: The attribute to query
  /// - `attributeValue`: The value of the attribute to match
  ///
  /// ### Returns
  ///
  /// True if any objects match the query, false otherwise
  public <A> boolean exists(@Nonnull Attribute<O, A> attribute, @Nonnull A attributeValue) {
    return exists(QueryFactory.equal(attribute, attributeValue));
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
    return queryFirst(query) == null;
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
