package org.openimaj.squall.tool.modes.planner;

import org.kohsuke.args4j.CmdLineOptionsProvider;

/**
 * Set the PlannerMode
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public enum PlannerModeOption implements CmdLineOptionsProvider{
	/**
	 * 
	 */
	GREEDY {
		@Override
		public PlannerMode getOptions() {
			return new GreedyPlannerMode();
		}
	},
	/**
	 * 
	 */
	GREEDYCS {
		@Override
		public PlannerMode getOptions() {
			return new CombinedSourceGreedyPlannerMode();
		}
	},
	/**
	 * 
	 */
	TPLO {
		@Override
		public PlannerMode getOptions() {
			return new TPLOPlannerMode();
		}
	},
	/**
	 * 
	 */
	TPLOCS {
		@Override
		public PlannerMode getOptions() {
			return new CombinedSourceTPLOPlannerMode();
		}
	};

	@Override
	public abstract PlannerMode getOptions() ;
	
}
