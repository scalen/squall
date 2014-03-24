package org.openimaj.rdf.storm.utils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 * @param <K>
 * @param <V>
 */
public interface TimedMap<K, V> extends TimeLimitedCollection, Map<K, V> {
	
	/**
	 * Adds a new object of type T to the time-based priority queue.  A timestamp should be generated for the object by the Collection.
	 * @param key 
	 * 		the key by which to insert the datum
	 * @param value
	 * 		the datum
	 * @return
	 * 		whether the add was successful
	 */
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
