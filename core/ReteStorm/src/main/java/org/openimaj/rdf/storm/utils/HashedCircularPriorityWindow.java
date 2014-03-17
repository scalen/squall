package org.openimaj.rdf.storm.utils;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.openimaj.rdf.storm.utils.OverflowHandler.CapacityOverflowHandler;
import org.openimaj.rdf.storm.utils.OverflowHandler.DurationOverflowHandler;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 * @param <K>
 * @param <V>
 */
public class HashedCircularPriorityWindow<K, V> implements TimedMap<K,V>, SpaceLimitedCollection {
	
	private static final Logger logger = Logger.getLogger(HashedCircularPriorityWindow.class);

	private final Map<K,Set<V>> map;
	private final Map<V,TimedMapEntry<K,V>> timeMap;
	private final PriorityQueue<TimedMapEntry<K, V>> queue;
	
	protected final int semanticCapacity;
	protected final int maxCapacity;
	protected final long delay;
	protected final TimeUnit unit;
	protected final OverflowHandler<V> continuation;
	
	/**
	 * @param handler 
	 * @param queryCap 
	 * @param delay 
	 * @param unit 
	 * 
	 */
	public HashedCircularPriorityWindow(OverflowHandler<V> handler, int queryCap, long delay, TimeUnit unit){
		this(handler, queryCap, queryCap*2, delay, unit);
	}
	
	/**
	 * @param handler 
	 * @param queryCap 
	 * @param maxCap 
	 * @param delay 
	 * @param unit 
	 * 
	 */
	public HashedCircularPriorityWindow(OverflowHandler<V> handler, int queryCap, int maxCap, long delay, TimeUnit unit){
		this.map = new HashMap<K, Set<V>>();
		this.timeMap = new HashMap<V, TimedMapEntry<K,V>>();
		this.queue = new PriorityQueue<TimedMapEntry<K,V>>(maxCap + 1);
		
		this.semanticCapacity = queryCap;
		this.maxCapacity = maxCap;
		this.unit = unit;
		this.delay = delay;
		this.continuation = handler;
	}
	
	private void overflowCapacity(V value){
		try {
			((CapacityOverflowHandler<V>)continuation).handleCapacityOverflow(value);
		} catch (NullPointerException e) {
		}catch (ClassCastException e) {
		}
	}
	
	private void overflowDuration(V value){
		try {
			((DurationOverflowHandler<V>)continuation).handleDurationOverflow(value);
		} catch (NullPointerException e) {
		}catch (ClassCastException e) {
		}
	}
	
	private V removeOldest() {
		TimedMapEntry<K,V> last = this.queue.remove();
		V lastValue = last.getValue();
		K lastKey = last.getKey();
		
		logger.debug("Removing from key: " + lastKey + " by prune capacity");
		if (this.map.get(lastKey) == null){
			System.out.println("This should never ever happen. Ever.");
		}
		if (this.timeMap.get(lastValue).getDropTime() == last.getDropTime()){
			this.map.get(lastKey).remove(lastValue);
			if (this.map.get(lastKey).isEmpty()){
				logger.debug("Removing the window of key: " + lastKey);
				this.map.remove(lastKey);
			}
		}
		
		return lastValue; 
	}
	
	@Override
	public void pruneToCapacity() {
		while (this.queue.size() > this.maxCapacity){
			this.overflowCapacity(this.removeOldest());
		}
	}
	
	private boolean containsOldItems() {
		if (this.queue.isEmpty()) return false;
		if(this.queue.peek().getDelay(TimeUnit.MILLISECONDS) > 0) return false;
		return true;
	}
	
	@Override
	public void pruneToDuration() {
		while (containsOldItems()){
			this.overflowDuration(this.removeOldest());
		}
	}

	@Override
	public int size() {
		this.pruneToDuration();
		return this.queue.size();
	}

	@Override
	public boolean isEmpty() {
		this.pruneToDuration();
		return this.map.isEmpty();
	}

	@Override
	public void clear() {
		this.map.clear();
		this.timeMap.clear();
		this.queue.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		this.pruneToDuration();
		return this.map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		this.pruneToDuration();
		for (K key : this.map.keySet()){
			if (this.map.get(key).contains(value)){
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<K> keySet() {
		return this.map.keySet();
	}

	@Override
	public V put(K key, V value) {
		return this.put(key, value, new Date().getTime(), this.delay, this.unit);
	}
	
	@Override
	public V put(K key, V value, long timestamp) {
		return this.put(key, value, timestamp, this.delay, this.unit);
	}

	@Override
	public V put(K key, V value, long timestamp, long delay) {
		return this.put(key, value, timestamp, delay, this.unit);
	}

	@Override
	public V put(K key, V value, long timestamp, long delay, TimeUnit unit) {
		Set<V> window = this.map.get(key);
		if (window == null){
			logger.debug("Adding the window for key: " + key);
			window = new HashSet<V>();
			this.map.put(key, window);
		}
		
		TimedMapEntry<K,V> tme;
		if (window.add(value)){
			tme = new TimedMapEntry<K,V>(key, value, timestamp, delay, unit);
			this.queue.add(tme);
			this.pruneToCapacity();
		} else {
			TimedMapEntry<K,V> existingEntry = this.timeMap.get(value);
			value = existingEntry.getValue();
			long newDroptime = timestamp + TimeUnit.MILLISECONDS.convert(delay, unit);
			if (newDroptime <= existingEntry.getDropTime()){
				return null;
			}
			tme = new TimedMapEntry<K,V>(existingEntry, timestamp, newDroptime);
			this.queue.remove(existingEntry);
			this.queue.add(tme);
		}
		this.timeMap.put(value, tme);
		
		return value;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (K key : m.keySet()){
			this.put(key, m.get(key));
		}
	}
	
	/**
	 * Gets the sub queue of items added to this queue that matches the given key.
	 * @param key
	 * 		The key by which to retrieve items.
	 * @return
	 * 		The set of items that were added with the same key.  Returns null if there are no items with the given key.
	 */
	public Set<V> getWindow(K key){
		this.pruneToDuration();
		return this.map.get(key);
	}
	
	/**
	 * Gets the sub queue of items added to this queue that matches the given key.  Items are returned with their associated timestamps.
	 * @param key
	 * 		The key by which to retrieve items.
	 * @return
	 * 		The set of items that were added with the same key, along with their most recent timestamps.  Returns null if there are no items with the given key.
	 */
	public Set<TimeWrapped<V>> getTimedWindow(K key){
		Set<TimeWrapped<V>> window = new HashSet<TimeWrapped<V>>();
		
		for (V w : this.map.get(key)){
			window.add(this.timeMap.get(w));
		}
		
		return window;
	}

	@Override
	public Collection<V> values() {
		this.pruneToDuration();
		
		Collection<V> vals = new HashSet<V>();
		for (K key : this.map.keySet()){
			for (V value : this.map.get(key)){
				vals.add(value);
			}
		}
		return vals;
	}
	
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		this.pruneToDuration();
		
		Set<java.util.Map.Entry<K, V>> entrySet = new HashSet<java.util.Map.Entry<K, V>>();
		for (java.util.Map.Entry<K, V> entry : this.queue){
			entrySet.add(entry);
		}
		return entrySet;
	}
	
//	private class MapEntry implements java.util.Map.Entry<K,V> {
//
//		private final K key;
//		private V value;
//		
//		public MapEntry(K key, V value) {
//			this.key = key;
//			this.value = value;
//		}
//
//		@Override
//		public K getKey() {
//			return this.key;
//		}
//
//		@Override
//		public V getValue() {
//			return this.value;
//		}
//
//		@Override
//		public V setValue(V value) {
//			return this.value = value;
//		}
//		
//		@SuppressWarnings("unchecked")
//		@Override
//		public boolean equals(Object obj) {
//			try {
//				return this.value.equals(((MapEntry)obj).getValue());
//			} catch (ClassCastException e) {}
//			try {
//				return this.value.equals((V)obj);
//			} catch (ClassCastException ex) {}
//			return false;
//		}
//		
//	}
//
//	/**
//	 * Inner class used to represent a timestamped MapEntry for the generic type V contained by the queue and its key of type K.
//	 */
//	private class TimedMapEntry extends MapEntry implements Delayed {
//
//		private long droptime;
//
//		public TimedMapEntry (K key, V toWrap, long ts, long delay, TimeUnit delayUnit) {
//			super(key, toWrap);
//			droptime = ts + TimeUnit.MILLISECONDS.convert(delay, delayUnit);
//		}
//
//		@Override
//		public boolean equals(Object obj){
//			if (obj.getClass().equals(TimedMapEntry.class))
//				return getDelay(HashedCircularPriorityWindow.this.unit) == this.getClass().cast(obj).getDelay(HashedCircularPriorityWindow.this.unit)
//						&& getValue().equals(TimedMapEntry.class.cast(obj).getValue());
//			else
//				return super.equals(obj);
//		}
//
//		@Override
//		public int compareTo(Delayed arg0) {
//			return (int) (arg0.getDelay(TimeUnit.MILLISECONDS) - getDelay(TimeUnit.MILLISECONDS));
//		}
//
//		@Override
//		public long getDelay(TimeUnit arg0) {
//			return arg0.convert(droptime - (new Date()).getTime(),TimeUnit.MILLISECONDS);
//		}
//
//	}
	
	// METHODS THAT DON'T MAKE SENSE IN THIS OBJECT

	@Override
	public V get(Object key) {
		throw new UnsupportedOperationException("Cannot manually get individuals (rather than queues of matching individuals) from the map.");
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException("Cannot manually delete dedicated windows to map.");
	}

}
