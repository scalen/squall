package org.openimaj.squall.tool.modes.planner;

import java.util.concurrent.TimeUnit;

import org.kohsuke.args4j.Option;
import org.openimaj.squall.compile.CompiledProductionSystem;
import org.openimaj.squall.orchestrate.OrchestratedProductionSystem;
import org.openimaj.squall.orchestrate.tplo.TPLOOrchestrator;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TPLOPlannerMode extends PlannerMode {

	
	/**
	 * number of topology workers selected
	 */
	@Option(
			name = "--window-capacity",
			aliases = "-wcap",
			required = false,
			usage = "The number of triples held by each join")
	public int capacity = 1000;
	
	/**
	 * number of topology workers selected
	 */
	@Option(
			name = "--window-time",
			aliases = "-wtime",
			required = false,
			usage = "The time for which each window holds bindings")
	public long time = 1;
	
	/**
	 * number of topology workers selected
	 */
	@Option(
			name = "--window-time-unit",
			aliases = "-wtimeu",
			required = false,
			usage = "The time unit for the window time")
	public TimeUnit tu = TimeUnit.MINUTES;
	
	@Override
	public OrchestratedProductionSystem ops(CompiledProductionSystem cps) {
		TPLOOrchestrator gom = new TPLOOrchestrator(capacity,time,tu);
		return gom.orchestrate(cps, getOperation());
	}

	

}
