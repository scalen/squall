package org.openimaj.squall.compile;

import java.util.List;

import org.openimaj.squall.compile.data.IStream;
import org.openimaj.squall.compile.data.IVFunction;
import org.openimaj.util.data.Context;


/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk), David Monks <dm11g08@ecs.soton.ac.uk>
 *
 */
public class ContextCPS extends CompiledProductionSystem{

	@Override
	public ContextCPS clone() {
		ContextCPS clone = new ContextCPS();
		
		for (IStream<Context> source : this.sources){
			clone.addSource(source);
		}
		for (CompiledProductionSystem cps : this.systems){
				clone.addSystem(cps.clone());
		}
		for (JoinComponent<?> func : this.joinlist){
			clone.addJoinComponent(func);
		}
		for (IVFunction<Context,Context> func : this.predicates){
			clone.addPredicate(func);
		}
		for (IVFunction<List<Context>,Context> func : this.aggregations){
			// TODO aggregations
		}
		clone.setConsequence(this.consequence);
		
		return clone;
	}

}
