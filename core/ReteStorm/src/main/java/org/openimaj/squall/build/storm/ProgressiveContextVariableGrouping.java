package org.openimaj.squall.build.storm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.graph.Node;

import backtype.storm.generated.GlobalStreamId;
import backtype.storm.grouping.CustomStreamGrouping;
import backtype.storm.task.WorkerTopologyContext;

/**
 * @author David Monks (dm11g08@ecs.soton.ac.uk)
 *
 */
public class ProgressiveContextVariableGrouping implements CustomStreamGrouping {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7032811266489361619L;
	private static final int[] primes = {2,3,5,7,11,13,17,19,23,29,31,37,41,43,47};
	private int bindingsIndex;
	private String[] vars;
	private Integer[] tasks;
	private Map<String,Integer> tasksCountByVar;
	private Map<String,Integer> groupsCountByVar;
	private Map<String,Long> bitMapByVar;

	/**
	 * @param variables
	 * @param index 
	 */
	public ProgressiveContextVariableGrouping(String[] variables, int index) {
		this.vars = variables;
		this.bindingsIndex = index;
	}
	
	/**
	 * @return number of variables being partitioned over
	 */
	public int getVariableCount() {
		return this.vars.length;
	}
	
	/**
	 * @param index
	 * @return the variable at the specified index in the set of variables.
	 */
	public String getVariable(int index){
		return this.vars[index];
	}
	
	/**
	 * @return an object that may be iterated over to produce the set of variables by which this grouping groups its tuples
	 */
	public Iterable<String> variables(){
		return new Iterable<String>(){
			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>(){
					private int index = 0;
					private String[] vars = ProgressiveContextVariableGrouping.this.vars;
					@Override public boolean hasNext() { return index < vars.length; }
					@Override public String next() { return vars[index++]; }
					@Override public void remove() { throw new UnsupportedOperationException("Grouping's variable set is immutable."); }
				};
			}
		};
	}

	@Override
	public void prepare(WorkerTopologyContext context, GlobalStreamId stream, List<Integer> targetTasks) {
		// initialise map
		this.tasksCountByVar = new HashMap<String,Integer>();
		this.groupsCountByVar = new HashMap<String,Integer>();
		this.bitMapByVar = new HashMap<String,Long>();
		
		// initialise variables for task partitioning:
		// the number of tasks per group in the previous/current partition
		int taskCount = targetTasks.size();
		// the previous number of distinct partitions
		int multiple = 1;
		// the variable currently being assigned a partition of the tasks
		int varIndex = 0;
		// loop until either:
		//   one variable remaining to which to assign a partition;
		//   run out of primes by which to progressively partition tasks;
		//   run out of primes by which the remaining tasks may be further partitioned.
		for (int i = 0; varIndex < this.vars.length - 1 && i < primes.length && primes[i] <= taskCount;){
			// if the progressively partitioned task partition size may be divided precisely by the current prime...
			if (taskCount % primes[i] == 0){
				//... divide it by that prime
				taskCount /= primes[i];
				// update the tasks per group and groups of tasks of a progressive partition for the current var
				this.tasksCountByVar.put(this.vars[varIndex],taskCount);
				this.groupsCountByVar.put(this.vars[varIndex],primes[i]);
				// initialise the bitmap for the current progressive partition
				long ts = 1;
				// set the bitmap to the same number of least significant bits as there are tasks per group for the current var.
				for (int x = 1; x < taskCount; x++){
					ts = (ts << 1) | 1;
				}
				// duplicate the bitmap into each of the previous partitions, producing the bitmap of the partition irrespective of prior partitioning.
				for (int x = 1; x < multiple; x++){
					ts = (ts << (taskCount*primes[i])) | ts;
				}
				// update the independent bitmap of the partitioning for the current var
				this.bitMapByVar.put(this.vars[varIndex],ts);
				// update the number of distinct partitions
				multiple *= primes[i];
				// move to the next variable to which to assign partitions
				varIndex++;
			} else {
				//... move to the next prime.
				i++;
			}
		}
		// if we ran out of variables before we reached a point where the number of tasks per maximally partitioned group 1...
		if (taskCount > 1) {
			//... update the tasks per group and groups of tasks of a progressive partition for the current var
			// to a group size of 1 and number of groups equal to the number of tasks still available for partitioning.
			this.tasksCountByVar.put(this.vars[varIndex],1);
			this.groupsCountByVar.put(this.vars[varIndex],taskCount);
			// initialise the bitmap for the current progressive partition.
			// this is already the map of the progressive partition.
			long ts = 1;
			// duplicate the bitmap into each of the previous partitionings spaces, producing the bitmap of the partition irrespective of prior partitioning.
			for (int x = 1; x < multiple; x++){
				ts = (ts << (taskCount)) | ts;
			}
			// update the independent bitmap of the partitioning for the current var
			this.bitMapByVar.put(this.vars[varIndex],ts);
			// update the number of distinct partitions (should now be equal to targetTasks.size())
			multiple *= taskCount;
			// update the task count to 1, being the number of tasks per group in the just completed partition
			taskCount = 1;
			// move to the next variable to which to assign partitions
			varIndex++;
		}
		// while there are still variables not assigned partitions...
		while (varIndex < this.vars.length){
			//... specify that the data should not be partitioned on that data by saying the group count for that variable is 1 and all tasks are in the group, with the bitmap being all 1s
			this.tasksCountByVar.put(this.vars[varIndex],targetTasks.size());
			this.groupsCountByVar.put(this.vars[varIndex],1);
			this.bitMapByVar.put(this.vars[varIndex],Long.MIN_VALUE);
			// move to the next variable to which to assign partitions
			varIndex++;
		}
		
		// store the specific tasks as an array of Integers.
		this.tasks = targetTasks.toArray(new Integer[targetTasks.size()]);
	}
	
	/**
	 * @return number of variables being partitioned over
	 */
	public int getTaskCount() {
		try {
			return this.tasks.length;
		} catch (NullPointerException e){
			throw new UnsupportedOperationException("Grouping has not been prepared!",e);
		}
	}
	
	/**
	 * @param index
	 * @return the variable at the specified index in the set of variables.
	 */
	public int getTask(int index){
		try {
			return this.tasks[index];
		} catch (NullPointerException e){
			throw new UnsupportedOperationException("Grouping has not been prepared!",e);
		}
	}
	
	/**
	 * @return an object that may be iterated over to produce the set of variables by which this grouping groups its tuples
	 */
	public Iterable<Integer> tasks(){
		if (this.tasks == null) throw new UnsupportedOperationException("Grouping has not been prepared!");
		return new Iterable<Integer>(){
			@Override
			public Iterator<Integer> iterator() {
				return new Iterator<Integer>(){
					private int index = 0;
					private Integer[] tasks = ProgressiveContextVariableGrouping.this.tasks;
					@Override public boolean hasNext() { return index < tasks.length; }
					@Override public Integer next() { return tasks[index++]; }
					@Override public void remove() { throw new UnsupportedOperationException("Grouping's task set is immutable."); }
				};
			}
		};
	}
	
	/**
	 * @param variable
	 * @return number of ways the stream is partitioned by the given variable
	 */
	public int getPartitionsByVariable(String variable){
		try {
			return this.groupsCountByVar.get(variable);
		} catch (NullPointerException e){
			throw new UnsupportedOperationException("Grouping has not been prepared!",e);
		}
	}
	
	/**
	 * @param variable
	 * @return number of tasks per partition in a given repetition of the partitioning plan
	 */
	public int getProgressivePartitionSizeByVariable(String variable){
		try {
			return this.tasksCountByVar.get(variable);
		} catch (NullPointerException e){
			throw new UnsupportedOperationException("Grouping has not been prepared!",e);
		}
	}
	
	/**
	 * @param variable
	 * @return the total bitmap for the partitioning of the streams by the given variable
	 */
	public long getPartitionBitMapByVariable(String variable){
		try {
			return this.bitMapByVar.get(variable);
		} catch (NullPointerException e){
			throw new UnsupportedOperationException("Grouping has not been prepared!",e);
		}
	}

	@Override
	public List<Integer> chooseTasks(int taskId, List<Object> values) {
		long tasks = Long.MIN_VALUE;
		@SuppressWarnings("unchecked")
		Map<String,Node> bindings = (Map<String,Node>)values.get(this.bindingsIndex);
		for (String bind : this.vars) {
			Object val = bindings.get(bind);
			if (val != null){
				tasks = tasks
						& (this.bitMapByVar.get(bind)
							<< (this.tasksCountByVar.get(bind)
								*(val.hashCode() % this.groupsCountByVar.get(bind))
							)
						);
			}
		}
		
		List<Integer> partitions = new ArrayList<Integer>();
		if (tasks > 0){
			long mask = 1;
			for (Integer task : this.tasks){
				if ((mask & tasks) == mask){
					partitions.add(task);
				}
				mask = mask << 1;
			}
		}
		return partitions;
	}

}
