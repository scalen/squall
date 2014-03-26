package org.openimaj.rdf.storm.utils;

import java.util.Collection;
import java.util.Map;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 * @param <K>
 * @param <V>
 */
public interface TimedMultiMap<K, V> extends TimeLimitedCollection, Map<K, V> {
	
	/**
	 * Adds a new object of type T to the time-based priority queue.  A timestamp should be generated for the object by the Collection.
	 * @param key 
	 * 		the key by which to insert the datum
	 * @param value
	 * 		the datum
	 * @return
	 * 		whether the add was successful
	 */
	@Override
	public V put(K key, V value);
	
	/**
	 * Adds a new object of type T to the time-based priority queue, along with the timestamp related to the object.
	 * @param key 
	 * 		the key by which to insert the datum
	 * @param value
	 * 		the datum
	 * @param timestamp
	 * 		an externally defined timestamp to be applied in the system.
	 * @return
	 * 		whether the add was successful
	 */
	public V put(K key, V value, long timestamp);
	
	/**
	 * Adds a new object of type T to the time-based priority queue, along with the timestamp related to the object and its intended expiry time.
	 * @param key 
	 * 		the key by which to insert the datum
	 * @param value
	 * 		the datum
	 * @param timestamp
	 * 		an externally defined timestamp to be applied in the system.
	 * @param droptime
	 * 		an externally defined timestamp to be applied in the system.
	 * @return
	 * 		whether the add was successful
	 */
	public V put(K key, V value, long timestamp, long droptime);
	
	/**
	 * Gets the Collection of items added to this map that matches the given key.
	 * @param key
	 * 		The key by which to retrieve items.
	 * @param timestamp 
	 * 		The time of the request
	 * @return
	 * 		The set of items that were added with the same key.  Returns null if there are no items with the given key.
	 */
	public Collection<V> getAll(K key, long timestamp);
	
	/**
	 * Gets the Collection of items added to this map that matches the given key.  Items are returned with their associated timestamps.
	 * @param key
	 * 		The key by which to retrieve items.
	 * @param timestamp 
	 * 		The time of the request
	 * @return
	 * 		The set of items that were added with the same key, along with their most recent timestamps.  Returns null if there are no items with the given key.
	 */
	public Collection<TimeWrapped<V>> getTimed(K key, long timestamp);
	
	/**
	 * @author davidlmonks
	 *
	 * @param <K>
	 * @param <V>
	 */
	public class TimedMapEntry<K,V> extends TimeWrapped<V> implements java.util.Map.Entry<K,V> {

		private final K key;
		
		protected TimedMapEntry(K key, V value, long ts, long droptime) {
			super(value, ts, droptime);
			this.key = key;
		}
		
		protected TimedMapEntry(TimedMapEntry<K,V> toUpdate, long ts, long droptime) {
			super(toUpdate, ts, droptime);
			this.key = toUpdate.key;
		}

		@Override
		public K getKey() {
			return this.key;
		}

		@Override
		public V getValue() {
			return this.wrapped;
		}

		@Override
		public V setValue(V value) {
			return this.wrapped = value;
		}
		
		@Override
		public boolean equals(Object obj) {
			return (obj instanceof java.util.Map.Entry)
					&& java.util.Map.Entry.class.cast(obj).getKey().equals(this.key)
					&& super.equals(obj);
		}
		
	}
	
}
