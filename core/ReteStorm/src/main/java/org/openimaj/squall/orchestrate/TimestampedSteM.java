package org.openimaj.squall.orchestrate;

import java.util.List;

import org.openimaj.rdf.storm.utils.TimeLimitedCollection.TimeWrapped;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 * @param <T> 
 *
 */
public interface TimestampedSteM <T> extends SteM <T> {
	
	/**
	 * @param typed
	 * @param timestamp 
	 * @param droptime
	 * @return
	 * 		True if the object 'typed' was successfully built into the SteM. False otherwise.
	 */
	public boolean build(T typed, long timestamp, long droptime);
	
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
	 * @param droptime
	 * @return
	 * 		The list of objects in the SteM that match the object 'typed', wrapped with their individual timestamps and drop times.
	 */
	public List<TimeWrapped<T>> probe(T typed, long timestamp, long droptime);
	
}
