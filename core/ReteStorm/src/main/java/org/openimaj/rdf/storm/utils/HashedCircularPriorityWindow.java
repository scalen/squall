package org.openimaj.rdf.storm.utils;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.openimaj.rdf.storm.utils.OverflowHandler.CapacityOverflowHandler;
import org.openimaj.rdf.storm.utils.OverflowHandler.DurationOverflowHandler;
import org.openimaj.squall.orchestrate.WindowInformation;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 * @param <K>
 * @param <V>
 */
public class HashedCircularPriorityWindow<K, V> implements TimedMultiMap<K,V>, SpaceLimitedCollection {
	
	private static final Logger logger = Logger.getLogger(HashedCircularPriorityWindow.class);

	private final Map<K,Set<V>> map;
	private final Map<V,TimedMapEntry<K,V>> timeMap;
	private final PruneableQueue queue;
	
	protected final long delay;
	protected final boolean overrideDelay;
	
	/**
	 * @param capHandler 
	 * @param durHandler 
	 * @param wi 
	 * 
	 */
	public HashedCircularPriorityWindow(CapacityOverflowHandler<V> capHandler, DurationOverflowHandler<V> durHandler, WindowInformation wi){
		this.delay = TimeUnit.MILLISECONDS.convert(wi.getDuration(), wi.getGranularity());
		this.overrideDelay = wi.isOverriding();
		
		this.queue = new OrderedPQ(wi.getCapacity(), capHandler, durHandler);
		this.map = new HashMap<K, Set<V>>(this.queue.maxCapacity());
		this.timeMap = new HashMap<V, TimedMapEntry<K,V>>(this.queue.maxCapacity());
	}
	
	@Override
	public void pruneToDuration(long timestamp) {
		this.queue.pruneToDuration(timestamp);
	}

	@Override
	public void pruneToCapacity() {
		this.queue.pruneToCapacity();
	}
	
	/**
	 * performs pruneToDuraction() using the given timestamp followed by pruneToCapacity().
	 * @param timestamp
	 */
	public void prune(long timestamp){
		this.queue.prune(timestamp);
	}

	@Override
	public int size() {
		return this.queue.size();
	}

	@Override
	public boolean isEmpty() {
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
		this.pruneToCapacity();
		return this.map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		this.pruneToCapacity();
		for (K key : this.map.keySet()){
			if (this.map.get(key).contains(value)){
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<K> keySet() {
		this.pruneToCapacity();
		return this.map.keySet();
	}

	@Override
	public V put(K key, V value) {
		long timestamp = new Date().getTime();
		return this.put(key, value, timestamp);
	}
	
	@Override
	public V put(K key, V value, long timestamp) {
		long droptime = timestamp + this.delay;
		return this.put(key, value, timestamp, droptime);
	}

	@Override
	public V put(K key, V value, long timestamp, long droptime) {
		if (this.overrideDelay) droptime = timestamp + this.delay;
		// If the droptime preceeds the timestamp, then the tansaction time of the value is an instant,
		// not intended to be stored.
		else if (droptime <= timestamp) return value;
		Set<V> window = this.map.get(key);
		if (window == null){
			logger.debug("Adding the window for key: " + key);
			window = new HashSet<V>();
			this.map.put(key, window);
		}
		
		if (!window.add(value)){
			TimedMapEntry<K,V> existingEntry = this.timeMap.get(value);
			value = existingEntry.getValue();
			if (droptime <= existingEntry.getDropTime()){
				return null;
			}
			this.queue.remove(existingEntry);
		}
		TimedMapEntry<K,V> tme = new TimedMapEntry<K,V>(key, value, timestamp, droptime);
		this.queue.add(tme);
		this.timeMap.put(value, tme);
		
		return value;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (K key : m.keySet()){
			this.put(key, m.get(key));
		}
	}
	
	@Override
	public Collection<V> getAll(K key, long timestamp){
		this.prune(timestamp);
		return this.map.get(key);
	}
	
	@Override
	public Collection<TimeWrapped<V>> getTimed(K key, long timestamp){
		this.prune(timestamp);
		
		Set<TimeWrapped<V>> window = new HashSet<TimeWrapped<V>>();
		
		for (V w : this.map.get(key)){
			window.add(this.timeMap.get(w));
		}
		
		return window;
	}
	
	/**
	 * Removes the specified value from the collection associated with the specified key, as long as that value is associated with the given droptime.
	 * @param key
	 * @param value
	 * @param droptime
	 * @return
	 * 		TimedMapEntry<K,V> - if the key is in the map and the value is associated with the droptime.
	 * 		null - otherwise.  
	 */
	public boolean remove (K key, V value, long droptime){
		TimedMapEntry<K, V> a;
		return (a = this.removeFromMap(key, value, droptime)) != null && this.queue.remove(a);
	}
	
	private TimedMapEntry<K,V> removeFromMap (K key, V value, long droptime){
		logger.debug("Removing from key: " + key);
		try {
			TimedMapEntry<K,V> tme = this.timeMap.get(value);
			if (tme.getDropTime() == droptime){
				try {
					if (this.map.get(key).remove(value)){
						this.timeMap.remove(value);
						if (this.map.get(key).isEmpty()){
							logger.debug("Removing the window of key: " + key);
							this.map.remove(key);
						}
						return tme;
					}
				} catch (NullPointerException e) {
					logger.debug("Window did not contain any values with key: " + key);
				}
			} else {
				logger.error("Value's expiration time, "+tme.getDropTime()+", does not match that to remove: " + droptime);
			}
		} catch (NullPointerException e) {
			logger.error("Value not time annotated: " + value);
		}
		return null;
	}

	@Override
	public Collection<V> values() {
		this.pruneToCapacity();
		
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
		this.pruneToCapacity();
		
		Set<java.util.Map.Entry<K, V>> entrySet = new HashSet<java.util.Map.Entry<K, V>>();
		for (java.util.Map.Entry<K, V> entry : this.queue){
			entrySet.add(entry);
		}
		return entrySet;
	}
	
	private abstract class PruneableQueue implements TimeLimitedCollection, SpaceLimitedCollection, Queue<TimedMapEntry<K, V>> {
		
		private final Queue<TimedMapEntry<K,V>> queue;
		private final CapacityOverflowHandler<V> capContinuation;
		private final DurationOverflowHandler<V> durContinuation;
		private final int capacity;
		private final int maxCap;
		
		protected abstract Queue<TimedMapEntry<K,V>> initQueue(int cap);
		
		protected PruneableQueue(int cap, CapacityOverflowHandler<V> cc, DurationOverflowHandler<V> dc){
			this.capacity = cap;
			this.maxCap = this.capacity > Integer.MAX_VALUE / 2 ? Integer.MAX_VALUE : this.capacity * 2;
			this.queue = initQueue(maxCap);
			this.capContinuation = cc;
			this.durContinuation = dc;
		}
		
		protected PruneableQueue(PruneableQueue pq){
			this.capacity = pq.capacity;
			this.maxCap = pq.maxCap;
			this.queue = initQueue(this.maxCap);
			this.capContinuation = pq.capContinuation;
			this.durContinuation = pq.durContinuation;
			this.queue.addAll(pq);
		}
		
		public void prune(long timestamp){
			this.pruneToDuration(timestamp);
			this.pruneToCapacity();
		}
		
		protected void overflowCapacity(V value){
			try {
				capContinuation.handleCapacityOverflow(value);
			} catch (NullPointerException e) {}
		}
		
		protected void overflowDuration(V value){
			try {
				durContinuation.handleDurationOverflow(value);
			} catch (NullPointerException e) {}
		}
		
		//clears the queue, then adds the elements from the specified part of the array to the queue in order
		//from first to last (leaving the head of the queue as the element at index "start", barring re-ordering).
		protected void repopulate(TimedMapEntry<K,V>[] arr, int start, int length){
			this.queue.clear();
			int stop = start + length;
			for (int i = start; i < stop; i ++){
				this.add(arr[i]);
			}
		}
		
		//orders elements smallest at index 0 to largest at index n.
		protected TimedMapEntry<K,V>[] prioritise(){
			@SuppressWarnings("unchecked")
			TimedMapEntry<K,V>[] tmec = new TimedMapEntry[this.queue.size()];
			Arrays.sort(this.queue.toArray(tmec));
			return tmec;
		}
		
		//checks if the head of the queue has expired.
		protected boolean nextItemIsOld() {
			if (this.queue.isEmpty()) return false;
			if(this.queue.peek().getDelay(TimeUnit.MILLISECONDS) > 0) return false;
			return true;
		}
		
		@Override
		public TimedMapEntry<K,V> remove() {
			TimedMapEntry<K,V> last = this.poll(); 
			if (last == null) throw new NoSuchElementException();
			return last;
		}
		
		@Override
		public TimedMapEntry<K, V> poll() {
			try {
				TimedMapEntry<K,V> last = this.queue.remove();
				if (HashedCircularPriorityWindow.this.removeFromMap(last.getKey(), last.getValue(), last.getDropTime()) != null){
					return last;
				}
			} catch (NoSuchElementException e){}
			return null;
		}
		
//		@SuppressWarnings("unchecked")
		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException("Will never be needed.");
//			TimedMapEntry<K,V>[] tmec = new TimedMapEntry[c.size()];
//			Iterator<?> iter = c.iterator();
//			try {
//				for (int i = 0; iter.hasNext(); i++){
//					tmec[i] = (TimedMapEntry<K,V>) iter.next();
//				}
//				if (this.queue.removeAll(c)){
//					for (TimedMapEntry<K,V> tme : tmec){
//						HashedCircularPriorityWindow.this.removeFromMap(tme.getKey(), tme.getValue(), tme.getDropTime());
//					}
//					return true;
//				}
//			} catch (ClassCastException e){}
//			return false;
		}
		
//		@SuppressWarnings("unchecked")
		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException("Will never be needed.");
//			TimedMapEntry<K,V>[] tmec = new TimedMapEntry[c.size()];
//			Iterator<?> iter = this.queue.iterator();
//			try {
//				for (int i = 0; iter.hasNext(); i++){
//					Object item = iter.next();
//					if (!c.contains(item)){
//						tmec[i] = (TimedMapEntry<K,V>) item;
//					}
//				}
//				if (this.queue.retainAll(c)){
//					for (TimedMapEntry<K,V> tme : tmec){
//						HashedCircularPriorityWindow.this.removeFromMap(tme.getKey(), tme.getValue(), tme.getDropTime());
//					}
//					return true;
//				}
//			} catch (ClassCastException e){}
//			return false;
		}
		
		@Override
		public boolean add(TimedMapEntry<K, V> e) {
			boolean ret = this.queue.add(e);
			if (this.queue.size() >= this.maxCap)
				this.pruneToCapacity();
			return ret;
		}
		
		@Override
		public boolean offer(TimedMapEntry<K, V> e) {
			try {
				return this.add(e);
			} catch (IllegalStateException ex) {
				return false;
			}
		}
		
		@Override
		public boolean addAll(Collection<? extends TimedMapEntry<K, V>> c) {
			boolean changed = false;
			for (TimedMapEntry<K, V> tme : c){
				changed |= this.offer(tme);
			}
			return changed;
		}
		
		@Override public int size() {this.pruneToCapacity();return this.queue.size();}
		protected int currentSize() {return this.queue.size();}
		@Override public TimedMapEntry<K, V> element() {this.pruneToCapacity();return this.queue.element();}
		@Override public TimedMapEntry<K, V> peek() {this.pruneToCapacity();return this.queue.peek();}
		@Override public Iterator<TimedMapEntry<K, V>> iterator() {this.pruneToCapacity();return this.queue.iterator();}
		@Override public boolean contains(Object o) {this.pruneToCapacity();return this.queue.contains(o);}
		@Override public Object[] toArray() {this.pruneToCapacity();return this.queue.toArray();}
		@Override public <T> T[] toArray(T[] a) {this.pruneToCapacity();return this.queue.toArray(a);}
		@Override public boolean containsAll(Collection<?> c) {this.pruneToCapacity();return this.queue.containsAll(c);}
		
		public int capacity() {return this.capacity;}
		protected int maxCapacity() {return this.maxCap;}
		@Override public boolean isEmpty() {return this.queue.isEmpty();}
		@Override public boolean remove(Object o) {return this.queue.remove(o);}
		@Override public void clear() {this.queue.clear();}
		
	}
	
	private class OrderedPQ extends PruneableQueue {

		protected Queue<TimedMapEntry<K,V>> initQueue(int cap){
			return new PriorityQueue<TimedMapEntry<K,V>>(cap * 2);
		}
		
		public OrderedPQ(int size, CapacityOverflowHandler<V> cc, DurationOverflowHandler<V> dc){
			super(size, cc, dc);
		}
		
		protected OrderedPQ(PruneableQueue pq){
			super(pq);
		}
		
		//add: O(logf) (ammortised O((D+1)logf)) where n = logical queue capacity,
		//											   d = number of items affected,
		//											   f = min(2n, MAX_INT),
		//											   D = f - n .
		//addAll: O(dlogf + floor(d/D)(Dlogf))
		
		//O(dlog(n+d))
		@Override
		public void pruneToCapacity() {
			while (this.currentSize() > this.capacity()){
				this.overflowCapacity(this.remove().getValue());
			}
		}
		
		//O(dlog(n+d))
		@Override
		public void pruneToDuration(long timestamp) {
			TimeWrapped.incrementNow(timestamp);
			while (this.nextItemIsOld()){
				try{
					this.overflowDuration(this.remove().getValue());
				} catch (NoSuchElementException e){}
			}
		}
		
	}
	
	@SuppressWarnings("unused")
	private class JITPQ extends PruneableQueue {

		protected Queue<TimedMapEntry<K,V>> initQueue(int cap){
			return new ArrayDeque<TimedMapEntry<K,V>>(cap * 2);
		}
		
		public JITPQ(int size, CapacityOverflowHandler<V> cc, DurationOverflowHandler<V> dc){
			super(size, cc, dc);
		}
		
		protected JITPQ(PruneableQueue pq){
			super(pq);
		}
		
		//add: O(1) (ammortised O(flogf + f + 2D)) where n = logical queue capacity,
		//												 f = min(2n, MAX_INT),
		//												 D = f - n.
		//addAll: O(d + floor(d/D)(flogf + f + 2D)) where d = number of items affected.
		
		//O((n+d)log(n+d) + n + 3d)
		@Override
		public void pruneToCapacity() {
			TimedMapEntry<K,V>[] tmec = this.prioritise();
			int i;
			for (i = 0; i + this.capacity() < tmec.length; i++){
				HashedCircularPriorityWindow.this.removeFromMap(tmec[i].getKey(), tmec[i].getValue(), tmec[i].getDropTime());
				this.overflowCapacity(tmec[i].getValue());
			}
			this.repopulate(tmec, i, tmec.length - i);
		}
		
		//O((n+d)log(n+d) + n + 3d)
		@Override
		public void pruneToDuration(long timestamp) {
			TimeWrapped.incrementNow(timestamp);
			TimedMapEntry<K,V>[] tmec = this.prioritise();
			int i;
			for (i = 0; i < tmec.length ? tmec[i].getDelay(TimeUnit.MILLISECONDS) < 0 : false; i++){
				HashedCircularPriorityWindow.this.removeFromMap(tmec[i].getKey(), tmec[i].getValue(), tmec[i].getDropTime());
				this.overflowDuration(tmec[i].getValue());
			}
			this.repopulate(tmec, i - 1, tmec.length - i);
		}
		
		//O((n+d)log(n+d) + n + 3d) where m = n+d
		@Override
		public void prune(long timestamp){
			TimeWrapped.incrementNow(timestamp);
			TimedMapEntry<K,V>[] tmec = this.prioritise();
			int i;
			for (i = 0; i < tmec.length ? tmec[i].getDelay(TimeUnit.MILLISECONDS) < 0 : false; i++){
				HashedCircularPriorityWindow.this.removeFromMap(tmec[i].getKey(), tmec[i].getValue(), tmec[i].getDropTime());
				this.overflowDuration(tmec[i].getValue());
			}
			for (; i + this.capacity() < tmec.length; i++){
				HashedCircularPriorityWindow.this.removeFromMap(tmec[i].getKey(), tmec[i].getValue(), tmec[i].getDropTime());
				this.overflowCapacity(tmec[i].getValue());
			}
			this.repopulate(tmec, i, tmec.length - i);
		}
		
	}
	
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
