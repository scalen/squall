package org.openimaj.rdf.storm.utils;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 */
public interface TimeLimitedCollection {

	/**
	 * Prunes items from the queue if they have expired at the time of the call (items are expired if their timestamp + duration &lt; the most recent timestamp received).
	 * @param timestamp 
	 */
	public void pruneToDuration(long timestamp);

	/**
	 * Inner class used to represent a timestamped MapEntry for the generic type V contained by the queue and its key of type K.
	 * @param <T> 
	 */
	public class TimeWrapped<T> implements Delayed {

		private static long now = 0;
		
		/**
		 * @param timestamp
		 * @return
		 */
		public static long incrementNow(long timestamp){
			return TimeWrapped.now = Math.max(TimeWrapped.now, timestamp);
		}
		
		/**
		 * @param timestamp
		 * @return
		 */
		public static long resetNow(){
			return TimeWrapped.now = 0;
		}
		
		/**
		 * @return
		 */
		public static long getNow() {
			return TimeWrapped.now;
		}
		
		private long droptime;
		private long timestamp;
		protected T wrapped;

		/**
		 * @param toWrap
		 * @param ts
		 * @param droptime
		 */
		public TimeWrapped (T toWrap, long ts, long droptime) {
			this.wrapped = toWrap;
			this.timestamp = ts;
			this.droptime = droptime;
		}
		
		protected TimeWrapped (TimeWrapped<T> toUpdate, long ts, long droptime) {
			this.wrapped = toUpdate.wrapped;
			this.timestamp = ts;
			this.droptime = droptime;
		}

		@Override
		public boolean equals(Object obj){
			if (obj instanceof TimeWrapped)
				return getDelay(TimeUnit.MILLISECONDS) == TimeWrapped.class.cast(obj).getDelay(TimeUnit.MILLISECONDS)
						&& this.wrapped.equals(TimeWrapped.class.cast(obj).wrapped);
			else
				return this.wrapped.equals(obj);
		}
		
		@Override
		public int hashCode() {
			return this.wrapped.hashCode();
		}

		@Override
		public int compareTo(Delayed arg0) {
			return (int) (getDelay(TimeUnit.MILLISECONDS) - arg0.getDelay(TimeUnit.MILLISECONDS));
		}

		@Override
		public long getDelay(TimeUnit arg0) {
			return arg0.convert(droptime - TimeWrapped.now,TimeUnit.MILLISECONDS);
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
		
		@Override
		public String toString() {
			return new StringBuilder("[ ")
					.append(this.wrapped.toString())
					.append(", ")
					.append(this.timestamp)
					.append(", ")
					.append(this.droptime)
					.append(" ]")
					.toString();
		}

	}
	
}
