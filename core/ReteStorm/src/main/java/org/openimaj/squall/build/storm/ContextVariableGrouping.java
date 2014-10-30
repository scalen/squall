package org.openimaj.squall.build.storm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openimaj.util.data.Context;
import org.openimaj.util.data.ContextKey;

import com.hp.hpl.jena.graph.Node;

import backtype.storm.generated.GlobalStreamId;
import backtype.storm.grouping.CustomStreamGrouping;
import backtype.storm.task.WorkerTopologyContext;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class ContextVariableGrouping implements CustomStreamGrouping {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7032811266489361619L;
	private String[] hashingVars;
	private int bindingsIndex;
	private List<Integer> tasks;

	/**
	 * @param variables
	 * @param index 
	 */
	public ContextVariableGrouping(String[] variables, int index) {
		hashingVars = variables;
		this.bindingsIndex = index;
	}

	@Override
	public void prepare(WorkerTopologyContext context, GlobalStreamId stream, List<Integer> targetTasks) {
		this.tasks = targetTasks;
	}

	@Override
	public List<Integer> chooseTasks(int taskId, List<Object> values) {
		List<Node> nodes = new ArrayList<Node>();
		@SuppressWarnings("unchecked")
		Map<String,Node> bindings = (Map<String,Node>) values.get(this.bindingsIndex);
		for (String bind : this.hashingVars) {
			nodes.add(bindings.get(bind));
		}
		int index = nodes.hashCode() % this.tasks.size();
		return Arrays.asList(this.tasks.get(index));
	}

}
