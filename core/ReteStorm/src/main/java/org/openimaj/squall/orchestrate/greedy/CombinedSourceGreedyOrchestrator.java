package org.openimaj.squall.orchestrate.greedy;

import java.util.concurrent.TimeUnit;

import org.openimaj.squall.compile.CompiledProductionSystem;
import org.openimaj.squall.compile.OptionalProductionSystems;
import org.openimaj.squall.data.ISource;
import org.openimaj.squall.orchestrate.NamedSourceNode;
import org.openimaj.squall.orchestrate.OrchestratedProductionSystem;
import org.openimaj.util.data.Context;
import org.openimaj.util.stream.Stream;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 */
public class CombinedSourceGreedyOrchestrator extends GreedyOrchestrator {
	
	/**
	 * @param capacity
	 * @param duration
	 * @param time
	 */
	public CombinedSourceGreedyOrchestrator(int capacity, long duration, TimeUnit time) {
		super(capacity, duration, time);
	}
	
	/**
	 * 
	 */
	public CombinedSourceGreedyOrchestrator() {
		super();
	}

	@Override
	protected void orchestrateSources(CompiledProductionSystem sys,
			OrchestratedProductionSystem root) {
		CombinedISource sources = new CombinedISource();
		orchestrateCombinedSources(sys, sources);
		root.root.add(new NamedSourceNode(root, nextSourceName(root), sources));
	}
	
	private void orchestrateCombinedSources(CompiledProductionSystem sys, CombinedISource cis){
		if(sys.getStreamSources().size()>0){
			for (ISource<Stream<Context>> sourceS: sys.getStreamSources()) {				
				cis.add(sourceS);
			}
		}
		for (OptionalProductionSystems opss : sys.getSystems()) {
			for (CompiledProductionSystem cps: opss) {
				orchestrateCombinedSources(cps, cis);
			}
		}
	}

}