/**
 * Copyright (c) ${year}, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.rdf.storm.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

import org.openimaj.rdf.storm.utils.OverflowHandler.CapacityOverflowHandler;
import org.openimaj.rdf.storm.utils.OverflowHandler.DurationOverflowHandler;

import scala.actors.threadpool.Arrays;

/**
 *
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 * @param <T>
 */
public class CircularPriorityWindow <T> implements TimedQueue <T>, SpaceLimitedCollection {

	static{
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
	}
	protected PriorityQueue<TimeWrapped<T>> queue;
	protected Map<T, Count> data;
	protected final int capacity;
	protected final long delay;
	protected final TimeUnit unit;
	protected final OverflowHandler<T> continuation;

	/**
	 * @param handler 
	 * @param size
	 * @param delay
	 * @param unit
	 */
	public CircularPriorityWindow(OverflowHandler<T> handler, int size, long delay, TimeUnit unit) {
		this.capacity = size;
		this.unit = unit;
		this.delay = delay;
		this.continuation = handler;
		this.clear();
	}
	
	/**
	 * @return int
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * @return long
	 */
	public long getDelay() {
		return delay;
	}
	
//	/**
//	 * @return oldest timestamp
//	 */
//	public long getOldestTimestamp(){
//		this.prune();
//		return data.peek().getTimestamp();
//	}

	@Override
	public void clear() {
		this.queue = new PriorityQueue<TimeWrapped<T>>(this.capacity + 1);
		this.data = new HashMap<T,Count>();
	}

	@Override
	public void pruneToDuration(long timestamp) {
		TimeWrapped.incrementNow(timestamp);
		Iterator<TimeWrapped<T>> pruner = new Iterator<TimeWrapped<T>>(){
			TimeWrapped<T> last;
			@Override
			public boolean hasNext() {
				last = queue.peek();
				if(last == null) return false;
				return last.getDelay(CircularPriorityWindow.this.unit) < 0;
			}
			@Override
			public TimeWrapped<T> next() {
				return last;
			}
			@Override
			public void remove() {
				queue.remove(last);
				T lastUnwrapped = last.getWrapped();
				decrement(lastUnwrapped);
				try {
					((DurationOverflowHandler<T>)continuation).handleDurationOverflow(lastUnwrapped);
				} catch (NullPointerException e) {
					
				}catch (ClassCastException e) {
					
				}
				last = null;
			}
		};

		while (pruner.hasNext()){
			pruner.remove();
		}
	}
	
	@Override
	public void pruneToCapacity() {
		Iterator<TimeWrapped<T>> pruner = new Iterator<TimeWrapped<T>>(){
			TimeWrapped<T> last;
			@Override
			public boolean hasNext() {
				last = queue.peek();
				if(last == null) return false;
				return queue.size() > capacity;
			}
			@Override
			public TimeWrapped<T> next() {
				return last;
			}
			@Override
			public void remove() {
				queue.remove(last);
				T lastUnwrapped = last.getWrapped();
				decrement(lastUnwrapped);
				try {
					((CapacityOverflowHandler<T>)continuation).handleCapacityOverflow(lastUnwrapped);
				} catch (NullPointerException e) {
					
				}catch (ClassCastException e) {
					
				}
				last = null;
			}
		};

		while (pruner.hasNext()){
			pruner.remove();
		}
	}

	@Override
	public boolean contains(Object arg0) {
		return data.containsKey(arg0) || queue.contains(arg0);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		boolean contains = true;
		for (Object item : arg0)
			contains &= contains(item);
		return contains;
	}

	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	public int size() {
		return queue.size();
	}

	@Override
	public T[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("hiding")
	@Override
	public <T> T[] toArray(T[] arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean decrement(T arg0) {
		Count count = data.get(arg0);
        if (count == null) {
            return false;
        } else {
            count.dec();
            if (count.getCount() == 0) {
                data.remove(arg0);
            }
        }
        return true;
	}

	private boolean increment(T arg0) {
		Count count = data.get(arg0);
        if (count == null) {
            data.put(arg0, new Count(1));
        } else {
            count.inc();
        }
        return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object arg0) {
		try {
			return queue.remove(arg0) && decrement(((TimeWrapped<T>)arg0).getWrapped());
		} catch (ClassCastException e) {}
		return false;

	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		boolean removed = true;
		for (Object item : arg0)
			removed &= remove(item);
		return removed;
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		boolean removed = true;
		List<TimeWrapped<T>> removals = new ArrayList<TimeWrapped<T>>();
		for (TimeWrapped<T> item : queue){
			boolean toRemove = true;
			for (Object keeper : arg0)
				toRemove &= !item.equals(keeper);
			if (toRemove)
				removals.add(item);
		}
		for (TimeWrapped<T> item : removals)
			removed &= remove(item);
		return removed;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(Collection<? extends T> arg0) {
		boolean success = true;
		List<Object> added = new ArrayList<Object>();
		for (Object item : arg0){
			try {
				success &= add((T)item);
			} catch (ClassCastException e) {
				try {
					success &= add((TimeWrapped<T>)item);
				} catch (ClassCastException ex) {
					success = false;
				}
			}
			if (success){
				added.add(item);
			} else {
				break;
			}
		}
		if (!success){
			for (Object item : added){
				remove(item);
			}
		}
		return success;
	}

	private boolean add(TimeWrapped<T> arg0){
		if (arg0.getWrapped() == null) return false;
		try {
			return queue.add(arg0);
		} catch (IllegalStateException ise) {
			pruneToCapacity();
			try {
				((CapacityOverflowHandler<T>)continuation).handleCapacityOverflow(queue.remove().getWrapped());
			} catch (NullPointerException e) {
				queue.remove();
			} catch (ClassCastException e) {
				queue.remove();
			}
			return queue.add(arg0);
		} finally {
			increment(arg0.getWrapped());
		}
	}

	@Override
	public boolean add(T arg0) {
		return add(arg0,new Date().getTime(),this.delay,this.unit);
	}
	
	
	@Override
	public boolean add(T arg0, long timestamp) {
		return add(arg0,timestamp,this.delay,this.unit);
	}
	
	@Override
	public boolean add(T arg0, long timestamp, long delay) {
		return add(arg0,timestamp,delay,this.unit);
	}
	
	@Override
	public boolean add(T arg0, long timestamp, long delay, TimeUnit unit) {
		return add(new TimeWrapped<T>(arg0,timestamp,TimeUnit.MILLISECONDS.convert(delay,unit)));
	}

	@Override
	public boolean offer(T arg0) {
		try {
			return add(arg0);
		} catch (IllegalStateException e) {
			return false;
		}
	}
	
	@Override
	public boolean offer(T arg0, long timestamp) {
		try {
			return add(arg0,timestamp);
		} catch (IllegalStateException e) {
			return false;
		}
	}
	
	@Override
	public boolean offer(T arg0, long timestamp, long delay) {
		try {
			return add(arg0, timestamp, delay);
		} catch (IllegalStateException e) {
			return false;
		}
	}
	
	@Override
	public boolean offer(T arg0, long timestamp, long delay, TimeUnit unit) {
		try {
			return add(arg0,timestamp,delay,unit);
		} catch (IllegalStateException e) {
			return false;
		}
	}

	@Override
	public T element() {
		pruneToCapacity();
		return queue.element().getWrapped();
	}

	@Override
	public T peek() {
		try {
			return element();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public T poll() {
		try {
			return remove();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public T remove() {
		pruneToCapacity();
		T item = queue.remove().getWrapped();
		decrement(item);
		return item;
	}

	@Override
	public Iterator <T> iterator() {
		pruneToCapacity();
		try {
			@SuppressWarnings("unchecked")
			final List<TimeWrapped<T>> dc = Arrays.asList(queue.toArray());
			Collections.sort(dc);
			return new Iterator<T>(){
				List<TimeWrapped<T>> dataclone = dc;
				int index = 0;
				TimeWrapped<T> last;
				@Override
				public boolean hasNext() {
					return index < dataclone.size();
				}
				@Override
				public T next() {
					return (last = dataclone.get(index++)).getWrapped();
				}
				@Override
				public void remove() {
					queue.remove(last);
					decrement(last.getWrapped());
				}
			};
		} catch (ClassCastException e) {
			System.out.print(queue.toString());
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	/**
	 * Gets the time-to-expiry of the oldest item in the queue.  If the queue is empty, returns null.
	 * @param unit
	 * 		Granularity of time-to-expiry
	 * @return
	 * 		The time-to-expiry of the oldest item in the queue.
	 */
	public Long getNextExpiry(TimeUnit unit){
		return this.queue.peek() == null ? null : this.queue.peek().getDelay(unit);
	}
	
	/**
	 * Removes the item that would expire next from the queue.
	 * @return
	 * 		The removed item.
	 */
	public T removeNextToExpire(){
		return this.queue.remove().getWrapped();
	}

}