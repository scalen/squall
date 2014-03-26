package org.openimaj.squall.orchestrate.greedy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.openimaj.rdf.storm.utils.DeepHashArray;
import org.openimaj.rdf.storm.utils.HashedCircularPriorityWindow;
import org.openimaj.rdf.storm.utils.OverflowHandler;
import org.openimaj.rdf.storm.utils.OverflowHandler.CapacityOverflowHandler;
import org.openimaj.rdf.storm.utils.OverflowHandler.DurationOverflowHandler;
import org.openimaj.rdf.storm.utils.TimeLimitedCollection.TimeWrapped;
import org.openimaj.rdf.storm.utils.TimedMultiMap;
import org.openimaj.squall.orchestrate.TimestampedSteM;
import org.openimaj.squall.orchestrate.WindowInformation;

import com.hp.hpl.jena.graph.Node;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 */
public class FixedHashSteM implements TimestampedSteM<Map<String, Node>>{
	
	TimedMultiMap<DeepHashArray<Node>,Map<String,Node>> window;
	List<String> sharedVariables; // must match the sibling stream
	
	/**
	 * @param sharedVariables
	 * @param wi 
	 */
	public FixedHashSteM(List<String> sharedVariables, WindowInformation wi) {
		this(null, sharedVariables, wi);
	}
	
	/**
	 * @param handler 
	 * @param sharedVariables
	 * @param wi 
	 */
	public FixedHashSteM(OverflowHandler<Map<String, Node>> handler, List<String> sharedVariables, WindowInformation wi) {
		this.sharedVariables = sharedVariables;
		
		CapacityOverflowHandler<Map<String, Node>> capHandler =
				handler instanceof CapacityOverflowHandler ? (CapacityOverflowHandler<Map<String, Node>>)handler : null;
		DurationOverflowHandler<Map<String, Node>> durHandler =
				handler instanceof DurationOverflowHandler ? (DurationOverflowHandler<Map<String, Node>>)handler : null;
		
		window = new HashedCircularPriorityWindow<DeepHashArray<Node>,Map<String,Node>>(capHandler,
																						durHandler,
																						wi);
	}
	
	@Override
	public boolean build(Map<String, Node> typed, long timestamp, long droptime) {
		return window.put(extractSharedBindings(typed), typed, timestamp, droptime) != null;
	}
	
	@Override
	public boolean build(Map<String, Node> typed, long timestamp) {
		return window.put(extractSharedBindings(typed), typed, timestamp) != null;
	}
	
	@Override
	public boolean build(Map<String, Node> typed) {
		return window.put(extractSharedBindings(typed), typed) != null;
	}
	
	private DeepHashArray<Node> extractSharedBindings(Map<String, Node> binds) {
		DeepHashArray<Node> vals = new DeepHashArray<Node>(new Node[this.sharedVariables.size()]);
		int i = 0;
		for (String key : this.sharedVariables){
			Node node = binds.get(key);
			if(node.isConcrete()){
				vals.set(i++, node);
				continue;
			} else {
				throw new UnsupportedOperationException("Incorrect node type for comparison: " + node);
			}
		}
		return vals;
	}

	@Override
	public List<Map<String,Node>> probe(Map<String, Node> typed) {
		List<Map<String, Node>> ret = new ArrayList<Map<String,Node>>();
		DeepHashArray<Node> sharedBindings = extractSharedBindings(typed);
		Collection<Map<String, Node>> matchedQueue = this.window.getAll(sharedBindings, new Date().getTime());
		if (matchedQueue != null){
			for (Map<String, Node> sibitem : matchedQueue) {
				Map<String,Node> newbind = new HashMap<String, Node>();
				for (Entry<String, Node> map : typed.entrySet()) {
					newbind.put(map.getKey(), map.getValue());
				}
				for (Entry<String, Node> map : sibitem.entrySet()) {
					newbind.put(map.getKey(), map.getValue());
				}
				ret.add(newbind);
			}
		}
		return ret ;
	}
	
	@Override
	public List<TimeWrapped<Map<String, Node>>> probe(Map<String, Node> typed, long timestamp, long droptime) {
		List<TimeWrapped<Map<String, Node>>> ret = new ArrayList<TimeWrapped<Map<String,Node>>>();
		DeepHashArray<Node> sharedBindings = extractSharedBindings(typed);
		Collection<TimeWrapped<Map<String, Node>>> matchedQueue = this.window.getTimed(sharedBindings, timestamp);
		if (matchedQueue != null){
			for (TimeWrapped<Map<String, Node>> sibitem : matchedQueue) {
				Map<String,Node> newbind = new HashMap<String, Node>();
				for (Entry<String, Node> map : typed.entrySet()) {
					newbind.put(map.getKey(), map.getValue());
				}
				for (Entry<String, Node> map : sibitem.getWrapped().entrySet()) {
					newbind.put(map.getKey(), map.getValue());
				}
				ret.add(new TimeWrapped<Map<String, Node>>(newbind,
																Math.max(timestamp, sibitem.getTimestamp()),
																Math.min(droptime, sibitem.getDropTime())
															));
			}
		}
		return ret ;
	}
	
	

}