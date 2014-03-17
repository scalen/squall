package org.openimaj.rdf.storm.utils;

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 */
public interface TimeLimitedCollection {

	/**
	 * Prunes items from the queue if they have expired at the time of the call (items are expired if their timestamp + duration &lt; Now).
	 * @return
	 * 		The number of items of data pruned from the queue.
	 */
	public void pruneToDuration();

	/**
	 * Inner class used to represent a timestamped MapEntry for the generic type V contained by the queue and its key of type K.
	 * @param <T> 
	 */
	public class TimeWrapped<T> implements Delayed {

		private long droptime;
		private long timestamp;
		private TimeUnit delayUnit;
		protected T wrapped;

		protected TimeWrapped (T toWrap, long ts, long delay, TimeUnit delayUnit) {
			this.wrapped = toWrap;
			this.delayUnit = delayUnit;
			this.timestamp = ts;
			this.droptime = ts + TimeUnit.MILLISECONDS.convert(delay, delayUnit);
		}

		@Override
		public boolean equals(Object obj){
			if (obj instanceof TimeWrapped)
				return getDelay(this.delayUnit) == TimeWrapped.class.cast(obj).getDelay(this.delayUnit)
						&& this.wrapped.equals(TimeWrapped.class.cast(obj).wrapped);
			else
				return this.wrapped.equals(obj);
		}

		@Override
		public int compareTo(Delayed arg0) {
			return (int) (arg0.getDelay(TimeUnit.MILLISECONDS) - getDelay(TimeUnit.MILLISECONDS));
		}

		@Override
		public long getDelay(TimeUnit arg0) {
			return arg0.convert(droptime - new Date().getTime(),TimeUnit.MILLISECONDS);
		}
		
		/**
		 * @return
		 */
		public T getWrapped(){
			return this.wrapped;
		}
		
		/**
		 * @return
		 */
		public long getTimestamp(){
			return this.timestamp;
		}
		
		/**
		 * @return
		 */
		public long getDropTime(){
			return this.droptime;
		}

	}
	
}
