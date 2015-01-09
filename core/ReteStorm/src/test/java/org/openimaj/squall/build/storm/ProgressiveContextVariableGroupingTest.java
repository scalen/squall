package org.openimaj.squall.build.storm;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import backtype.storm.tuple.Values;

/**
 * @author david.monks
 *
 */
public class ProgressiveContextVariableGroupingTest {
	
	private void testVariableStorage(ProgressiveContextVariableGrouping pcvg){
		assertEquals("Different number of variables stored than passed!", 3, pcvg.getVariableCount());
		assertEquals("Variables stored out of order!", "a", pcvg.getVariable(0));
		assertEquals("Variables stored out of order!", "b", pcvg.getVariable(1));
		assertEquals("Variables stored out of order!", "c", pcvg.getVariable(2));
		try {
			Iterator<String> vars = pcvg.variables().iterator();
			assertTrue("Iterator contains no values!", vars.hasNext());
			assertEquals("Variables iterated out of order!", "a", vars.next());
			assertTrue("Iterator contains too few values!", vars.hasNext());
			assertEquals("Variables iterated out of order!", "b", vars.next());
			assertTrue("Iterator contains too few values!", vars.hasNext());
			assertEquals("Variables iterated out of order!", "c", vars.next());
			assertFalse("Iterator contains too many values: " + vars.next(), vars.hasNext());
		} catch (AssertionError e){
			throw e;
		} catch (Throwable t){
			assertEquals("Unexpected exception thrown!", ArrayIndexOutOfBoundsException.class, t.getClass());
		}	
	}
	
	private void testPrepared(ProgressiveContextVariableGrouping pcvg, int taskCount, int expectedACount, int expectedBCount, int expectedCCount, int expectedASize, int expectedBSize, int expectedCSize){
		assertEquals("Different number of variables stored than passed!", taskCount, pcvg.getTaskCount());
		
		Iterator<Integer> ts = pcvg.tasks().iterator();
		for (int i = 0; i < taskCount; i++){
			assertTrue("Iterator contains too few tasks!", ts.hasNext());
		}
		
		assertEquals("Different first group count expected.", expectedACount, pcvg.getPartitionsByVariable("a"));
		assertEquals("Different second group count expected.", expectedBCount, pcvg.getPartitionsByVariable("b"));
		assertEquals("Different third group count expected.", expectedCCount, pcvg.getPartitionsByVariable("c"));
		
		assertEquals("Different first group size expected.", expectedASize, pcvg.getProgressivePartitionSizeByVariable("a"));
		assertEquals("Different second group size expected.", expectedBSize, pcvg.getProgressivePartitionSizeByVariable("b"));
		assertEquals("Different third group size expected.", expectedCSize, pcvg.getProgressivePartitionSizeByVariable("c"));
	}
	
	private void testUnprepared(ProgressiveContextVariableGrouping pcvg){
		try {
			pcvg.getTaskCount();
			assertTrue("Nothing thrown, expected an Unsupported Operation Exception!",false);
		} catch (AssertionError e){
			throw e;
		} catch (Throwable t){
			assertEquals("Unexpected exception thrown!", UnsupportedOperationException.class, t.getClass());
		}
		try {
			pcvg.getTask(0);
			assertTrue("Nothing thrown, expected an Unsupported Operation Exception!",false);
		} catch (AssertionError e){
			throw e;
		} catch (Throwable t){
			assertEquals("Unexpected exception thrown!", UnsupportedOperationException.class, t.getClass());
		}
		try {
			pcvg.tasks();
			assertTrue("Nothing thrown, expected an Unsupported Operation Exception!",false);
		} catch (AssertionError e){
			throw e;
		} catch (Throwable t){
			assertEquals("Unexpected exception thrown!", UnsupportedOperationException.class, t.getClass());
		}
		try {
			pcvg.getPartitionBitMapByVariable("a");
			assertTrue("Nothing thrown, expected an Unsupported Operation Exception!",false);
		} catch (AssertionError e){
			throw e;
		} catch (Throwable t){
			assertEquals("Unexpected exception thrown!", UnsupportedOperationException.class, t.getClass());
		}
		try {
			pcvg.getPartitionsByVariable("a");
			assertTrue("Nothing thrown, expected an Unsupported Operation Exception!",false);
		} catch (AssertionError e){
			throw e;
		} catch (Throwable t){
			assertEquals("Unexpected exception thrown!", UnsupportedOperationException.class, t.getClass());
		}
		try {
			pcvg.getProgressivePartitionSizeByVariable("a");
			assertTrue("Nothing thrown, expected an Unsupported Operation Exception!",false);
		} catch (AssertionError e){
			throw e;
		} catch (Throwable t){
			assertEquals("Unexpected exception thrown!", UnsupportedOperationException.class, t.getClass());
		}
	}
	
	private void testAllocation(ProgressiveContextVariableGrouping pcvg, Values vals, List<Integer> predictedTasks){
		List<Integer> tasks = pcvg.chooseTasks(0, vals);
		assertEquals("Number of assigned tasks differs from that predicted.", predictedTasks.size(), tasks.size());
		for (int i = 0; i < tasks.size(); i++){
			assertEquals("Task assignment differs from prediction.", predictedTasks.get(i), tasks.get(i));
		}
	}
	
	/**
	 * 
	 */
	@Test
	public void construction() {
		ProgressiveContextVariableGrouping pcvg = new ProgressiveContextVariableGrouping(new String[]{"a","b","c"}, 0);
		
		testVariableStorage(pcvg);
		testUnprepared(pcvg);
	}
	
	/**
	 * 
	 */
	@Test
	public void prepare(){
		ProgressiveContextVariableGrouping pcvg = new ProgressiveContextVariableGrouping(new String[]{"a","b","c"}, 0);
		
		List<Integer> tasks = new ArrayList<Integer>();
		Random random = new Random();
		for (int i = 0; i < 8; i++){
			tasks.add(random.nextInt());
		}
		pcvg.prepare(null, null, tasks);
		
		testVariableStorage(pcvg);
		testPrepared(pcvg, 8, 2, 2, 2, 4, 2, 1);
		
		tasks.add(random.nextInt());
		pcvg.prepare(null, null, tasks);
		
		testVariableStorage(pcvg);
		testPrepared(pcvg, 9, 3, 3, 1, 3, 1, 9);
		
		for (int i = 9; i < 12; i++){
			tasks.add(random.nextInt());
		}
		pcvg.prepare(null, null, tasks);
		
		testVariableStorage(pcvg);
		testPrepared(pcvg, 12, 2, 2, 3, 6, 3, 1);
		
		for (int i = 12; i < 15; i++){
			tasks.add(random.nextInt());
		}
		pcvg.prepare(null, null, tasks);
		
		testVariableStorage(pcvg);
		testPrepared(pcvg, 15, 3, 5, 1, 5, 1, 15);
		
		tasks.add(random.nextInt());
		pcvg.prepare(null, null, tasks);
		
		testVariableStorage(pcvg);
		testPrepared(pcvg, 16, 2, 2, 4, 8, 4, 1);
		
		for (int i = 16; i < 18; i++){
			tasks.add(random.nextInt());
		}
		pcvg.prepare(null, null, tasks);
		
		testVariableStorage(pcvg);
		testPrepared(pcvg, 18, 2, 3, 3, 9, 3, 1);
		
		for (int i = 18; i < 20; i++){
			tasks.add(random.nextInt());
		}
		pcvg.prepare(null, null, tasks);
		
		testVariableStorage(pcvg);
		testPrepared(pcvg, 20, 2, 2, 5, 10, 5, 1);
		
		for (int i = 20; i < 27; i++){
			tasks.add(random.nextInt());
		}
		pcvg.prepare(null, null, tasks);
		
		testVariableStorage(pcvg);
		testPrepared(pcvg, 27, 3, 3, 3, 9, 3, 1);
		
		tasks.add(random.nextInt());
		pcvg.prepare(null, null, tasks);
		
		testVariableStorage(pcvg);
		testPrepared(pcvg, 28, 2, 2, 7, 14, 7, 1);
		
		for (int i = 28; i < 30; i++){
			tasks.add(random.nextInt());
		}
		pcvg.prepare(null, null, tasks);
		
		testVariableStorage(pcvg);
		testPrepared(pcvg, 30, 2, 3, 5, 15, 5, 1);
	}
	
	/**
	 * 
	 */
	@Test
	public void allocate(){
//		System.out.println(String.format("%s",
//				Integer.valueOf(1).hashCode() % 2
//			)); // = 1
//		System.out.println(String.format("%s",
//				Integer.valueOf(2).hashCode() % 2
//			)); // = 0
//		System.out.println(String.format("%s",
//				Integer.valueOf(3).hashCode() % 2
//			)); // = 1
		ProgressiveContextVariableGrouping pcvg = new ProgressiveContextVariableGrouping(new String[]{"a","b","c"}, 0);
		
		List<Integer> tasks = new ArrayList<Integer>();
		Random random = new Random();
		for (int i = 0; i < 8; i++){
			tasks.add(random.nextInt());
		}
		pcvg.prepare(null, null, tasks);
		
		Map<String,Object> bindings = new HashMap<String,Object>();
		bindings.put("a", Integer.valueOf(1));
		bindings.put("b", Integer.valueOf(2));
		bindings.put("c", Integer.valueOf(3));
		
		Values testAllBindings = new Values(bindings);
		
		List<Integer> predictedTasks = new ArrayList<Integer>();
		predictedTasks.add(tasks.get(5));
		
		testAllocation(pcvg, testAllBindings, predictedTasks);
		
		bindings = new HashMap<String,Object>();
		bindings.put("a", Integer.valueOf(1));
		bindings.put("c", Integer.valueOf(3));
		
		Values testMissingBindings = new Values(bindings);
		
		predictedTasks = new ArrayList<Integer>();
		predictedTasks.add(tasks.get(5));
		predictedTasks.add(tasks.get(7));
		
		testAllocation(pcvg, testMissingBindings, predictedTasks);
	}

}
