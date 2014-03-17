package org.openimaj.squall.orchestrate.greedy;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 * @param <T> 
 *
 */
public interface TimestampedSteM <T> extends SteM <T> {

	/**
	 * @param typed
	 * @param timestamp 
	 * @param delay 
	 * @param unit 
	 * @return
	 * 		True if the object 'typed' was successfully built into the SteM. False otherwise.
	 */
	public boolean build(T typed, long timestamp, long delay, TimeUnit unit);
	
	/**
	 * @param typed
	 * @param timestamp 
	 * @param delay 
	 * @return
	 * 		True if the object 'typed' was successfully built into the SteM. False otherwise.
	 */
	public boolean build(T typed, long timestamp, long delay);
	
	/**
	 * @param typed
	 * @param timestamp 
	 * @return
	 * 		True if the object 'typed' was successfully built into the SteM. False otherwise.
	 */
	public boolean build(T typed, long timestamp);
	
	/**
	 * @param typed 
	 * @param timestamp 
	 * @param delay 
	 * @param delayUnit 
	 * @return
	 * 		The list of objects in the SteM that match the object 'typed', wrapped with their individual timestamps and drop times.
	 */
	public List<TimeAnnotated<T>> probe(T typed, long timestamp, long delay, TimeUnit delayUnit);
	
	/**
	 * @author David Monks <dm11g08@ecs.soton.ac.uk>
	 *
	 * @param <T>
	 */
	public class TimeAnnotated<T> {
		
		private final T thing;
		private final long timestamp;
		private final long droptime;
		
		protected TimeAnnotated(T t, long ts, long dt){
			this.thing = t;
			this.timestamp = ts;
			this.droptime = dt;
		}
		
		/**
		 * @return
		 */
		public T getWrapped(){
			return this.thing;
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
