package org.openimaj.rdf.storm.utils;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.openimaj.rdf.storm.utils.OverflowHandler.DurationOverflowHandler;
import org.openimaj.rdf.storm.utils.OverflowHandler.CapacityOverflowHandler;
import org.openimaj.rdf.storm.utils.TimeLimitedCollection.TimeWrapped;
import org.openimaj.squall.orchestrate.WindowInformation;

/**
 * @author David Monks &lt;dm11g08@ecs.soton.ac.uk&gt;
 *
 */
public class TestHashedCircularPriorityQueue implements DurationOverflowHandler<Integer>, CapacityOverflowHandler<Integer> {
	
	private static final int duration = 1;
	private static final TimeUnit unit = TimeUnit.MINUTES;
	
	private HashedCircularPriorityWindow<Integer, Integer> window;
	private List<Integer> capOverflow;
	private List<Integer> durOverflow;
	
	/**
	 * Executed before every test.
	 */
	@Before
	public void prepare(){
		WindowInformation wi = new WindowInformation(false, 2, duration, unit);
		window = new HashedCircularPriorityWindow<Integer, Integer>(this, this, wi);
		
		capOverflow = new ArrayList<Integer>();
		durOverflow = new ArrayList<Integer>();
		
		TimeWrapped.resetNow();
	}
	
	/**
	 * 
	 */
	@Test
	public void testAddOneItem(){
		window.put(1, 1, 0);
		assertTrue(window.size() == 1);
	}
	
	/**
	 * 
	 */
	@Test
	public void testRemoveOneItem(){
		window.put(1, 1, 0);
		int size = window.size();
		assertTrue(window.remove(1, 1, TimeUnit.MILLISECONDS.convert(duration, unit)));
		assertTrue(window.size() == size - 1);
	}
	
	/**
	 * 
	 */
	@Test
	public void testGetItems(){
		window.put(1, 1, 0);
		Collection<Integer> c = window.getAll(1, 30000);
		assertTrue(c.contains(1));
		
		Collection<TimeWrapped<Integer>> tc = window.getTimed(1, 30001);
		assertTrue(tc.contains(new TimeWrapped<Integer>(1, 0, TimeUnit.MILLISECONDS.convert(duration, unit))));
	}
	
	/**
	 * 
	 */
	@Test
	public void testDurationLimit(){
		window.put(1, 1, 0);
		
		// Confirm that previous items that expire are pruned before probe.
		window.put(2, 2, 50000);
		Collection<Integer> c = window.getAll(2, 70000);
		assertTrue(durOverflow.contains(1) && durOverflow.size() == 1);
		durOverflow.clear();
		assertTrue(c.contains(2) && c.size() == 1);
		
		// Confirm that results that arrive out of timestamp order are pruned by timestamp, not arrival.
		// Also confirm that "now" is a monotonically increasing local concept
		window.put(3, 3, 1);
		c = window.getAll(3, 2);
		assertTrue(durOverflow.contains(3) && durOverflow.size() == 1);
		durOverflow.clear();
		assertTrue(c == null);
	}
	
	/**
	 * 
	 */
	@Test
	public void testCapacityLimit() {
		window.put(1,1,0); // 1st item
		window.put(2,2,1); // 2nd item
		window.put(1,3,2); // 3rd item
		assertTrue(capOverflow.isEmpty());
		// Window does not overflow immediately after reaching the semantic limit.
		
		Collection<Integer> c = window.getAll(1,3);
		assertTrue(capOverflow.contains(1) && capOverflow.size() == 1);
		// Window overflows semantic limit when probed.  1st item overflowed
		capOverflow.clear();
		assertTrue(c.contains(3) && c.size() == 1);
		// Confirm window contains 3rd item only.
		c = window.getAll(2,4);
		assertTrue(capOverflow.isEmpty());
		// Confirm no new overlow on second consecutive probe. 
		assertTrue(c.contains(2) && c.size() == 1);
		// Confirm window contains 2nd item only.
		
		window.put(2,2,5);
		// add item that exists in window with more recent timestamp. 2nd item replaced by 4th item
		c = window.getAll(1,6);
		assertTrue(capOverflow.isEmpty());
		// Confirm no new overflow after probe, as duplicate item should replace.
		assertTrue(c.contains(3) && c.size() == 1);
		// Confirm window contains 3rd item only.
		c = window.getAll(2,7);
		assertTrue(c.contains(2) && c.size() == 1);
		// Confirm window contains 2nd item only.
		
		window.put(2,4,8); // 5th item
		c = window.getAll(1, 9);
		assertTrue(capOverflow.contains(3) && capOverflow.size() == 1);
		// Confirm duplicate item replaced 2nd item with 4th, meaning 3rd item overflows
		capOverflow.clear();
		assertTrue(c == null);
		// Confirm window nothing with key 1.
		c = window.getAll(2, 10);
		assertTrue(c.contains(2) && c.contains(4) && c.size() == 2);
		// Confirm duplicate item replaced 2nd item with 4th, meaning 2nd item and 4th are in window
		
		window.put(1,5,11); // 6th item
		window.put(3,6,12); // 7th item
		assertTrue(capOverflow.isEmpty());
		// confirm window has not been pruned yet
		window.put(2,7,14); // 8th item
		assertTrue(capOverflow.contains(2) && capOverflow.contains(4) && capOverflow.contains(5) && capOverflow.size() == 3);
		// confirm that the window is pruned when 2*capacity is exceeded, pruning oldest items (4th, 5th and 6th) 
		capOverflow.clear();
		
		// Check All windows are in the expected state: [2, 7], [3, 6]
		c = window.getAll(1,15);
		assertTrue(capOverflow.isEmpty());
		assertTrue(c == null);
		capOverflow.clear();
		
		c = window.getAll(2,16);
		assertTrue(capOverflow.isEmpty());
		assertTrue(c.contains(7) && c.size() == 1);
		
		c = window.getAll(3,17);
		assertTrue(capOverflow.isEmpty());
		assertTrue(c.contains(6) && c.size() == 1);
		
		window.put(1,5,11); // 9th item, identical to 6th
		window.put(2,8,13); // 10th item, between 7th item and 8th item
		c = window.getAll(2, 18);
		assertTrue(capOverflow.contains(5) && capOverflow.contains(6) && capOverflow.size() == 2);
		// confirm that 9th item is pruned ahead of pre-existing items with more recent timestamps,
		// and that 10th item displaces 7th item
		capOverflow.clear();
		assertTrue(c.contains(7) && c.contains(8) && c.size() == 2);
		
		window.put(2, 9, 19); // 11th item
		c = window.getAll(1, 20);
		assertTrue(capOverflow.contains(8) && capOverflow.size() == 1);
		// confirm that 10th item is pruned ahead of pre-existing 8th item with more recent timestamp
		capOverflow.clear();
	}

	@Override
	public void handleDurationOverflow(Integer overflow) {
		durOverflow.add(overflow);
	}

	@Override
	public void handleCapacityOverflow(Integer overflow) {
		capOverflow.add(overflow);
	}
	
}
