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
import org.openimaj.squall.orchestrate.WindowInformation;

/**
 * @author David Monks &lt;dm11g08@ecs.soton.ac.uk&gt;
 *
 */
public class TestHashedCircularPriorityQueue implements DurationOverflowHandler<Integer>, CapacityOverflowHandler<Integer> {
	
	private HashedCircularPriorityWindow<Integer, Integer> window;
	private List<Integer> capOverflow;
	private List<Integer> durOverflow;
	
	/**
	 * Executed before every test.
	 */
	@Before
	public void prepare(){
		WindowInformation wi = new WindowInformation(false, 2, 1, TimeUnit.MINUTES);
		window = new HashedCircularPriorityWindow<Integer, Integer>(this, this, wi);
		
		capOverflow = new ArrayList<Integer>();
		durOverflow = new ArrayList<Integer>();
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
		window.remove(1, 1, 60000);
		assertTrue(window.size() == size -1);
	}
	
	/**
	 * 
	 */
	@Test
	public void testGetItems(){
		window.put(1, 1, 0);
		Collection<Integer> c = window.getAll(1, 30000);
		assertTrue(c.contains(1));
	}
	
	/**
	 * 
	 */
	@Test
	public void testDurationLimit(){
		window.put(1, 1, 0);
		window.put(1, 2, 50000);
		Collection<Integer> c = window.getAll(1, 70000);
		assertTrue(c.contains(2));
		assertTrue(durOverflow.contains(1));
	}
	
	/**
	 * 
	 */
	@Test
	public void testCapacityLimit() {
		window.put(1,1,0);
		window.put(2,2,1);
		window.put(1,3,2);
		assertTrue(capOverflow.isEmpty());
		
		Collection<Integer> c = window.getAll(1,3);
		assertTrue(capOverflow.contains(1));
		assertTrue(c.contains(3));
		capOverflow.clear();
		
		c = window.getAll(2,4);
		assertTrue(capOverflow.isEmpty());
		assertTrue(c.contains(2));
		
		window.put(2,2,5);
		c = window.getAll(1,6);
		assertTrue(capOverflow.isEmpty());
		assertTrue(c.contains(3));
		c = window.getAll(2,7);
		assertTrue(c.contains(2));
		
		window.put(2,4,8);
		window.put(1,5,9);
		assertTrue(capOverflow.isEmpty());
		window.put(3,6,10);
		assertTrue(capOverflow.contains(2) && capOverflow.contains(3));
		capOverflow.clear();
		
		c = window.getAll(1,11);
		assertTrue(capOverflow.contains(4));// because getAll calls prunes to capacity
		assertTrue(c.contains(5));
		capOverflow.clear();
		
		c = window.getAll(2,12);
		assertTrue(capOverflow.isEmpty());
		assertTrue(c == null);
		
		c = window.getAll(3,13);
		assertTrue(capOverflow.isEmpty());
		assertTrue(c.contains(6));
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
