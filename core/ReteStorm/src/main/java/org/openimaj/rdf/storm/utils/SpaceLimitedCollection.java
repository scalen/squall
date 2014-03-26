package org.openimaj.rdf.storm.utils;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 */
public interface SpaceLimitedCollection {

	/**
	 * Prunes the oldest items from the queue if it has exceeded the semantic capacity of the queue.
	 */
	public void pruneToCapacity();
	
}
