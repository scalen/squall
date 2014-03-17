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
	 * Adds a new object of type T to the time-based priority queue, along with the timestamp related to the object and its intended life span in the queue (in milliseconds).
	 * @param key 
	 * 		the key by which to insert the datum
	 * @param value
	 * 		the datum
	 * @param timestamp
	 * 		an externally defined timestamp to be applied in the system.
	 * @param delay
	 * 		a datum-specific life span, defined in the unit of the Window
	 * @return
	 * 		whether the add was successful
	 */
	public V put(K key, V value, long timestamp, long delay);
	
	/**
	 * Adds a new object of type T to the time-based priority queue, along with the timestamp related to the object and its intended life span in the queue, given in the specified time unit.
	 * @param key 
	 * 		the key by which to insert the datum
	 * @param value
	 * 		the datum
	 * @param timestamp
	 * 		an externally defined timestamp to be applied in the system.
	 * @param delay
	 * 		a datum-specific life span
	 * @param unit
	 * 		the time unit that the datum-specific life span is defined in
	 * @return
	 * 		whether the add was successful
	 */
	public V put(K key, V value, long timestamp, long delay, TimeUnit unit);
	
	/**
	 * @author davidlmonks
	 *
	 * @param <K>
	 * @param <V>
	 */
	public class TimedMapEntry<K,V> extends TimeWrapped<V> implements java.util.Map.Entry<K,V> {

		private final K key;
		
		protected TimedMapEntry(K key, V value, long ts, long delay,
				TimeUnit delayUnit) {
			super(value, ts, delay, delayUnit);
			this.key = key;
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
