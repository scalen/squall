package org.openimaj.squall.tool;

import java.io.IOException;

import javax.swing.JFrame;

import org.openimaj.squall.compile.CompiledProductionSystem;
import org.openimaj.squall.orchestrate.OrchestratedProductionSystem;
import org.openimaj.squall.utils.OPSDisplayUtils;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SquallTool {
	private SquallToolOptions opts;

	/**
	 * @param opts
	 */
	public SquallTool(SquallToolOptions opts) {
		this.opts = opts;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		SquallTool st = new SquallTool(new SquallToolOptions(args));
		st.run();
	}

	private void run() {
		try{
			this.opts.setup();
			CompiledProductionSystem cps = opts.tmOp.cps();
			OrchestratedProductionSystem ops = opts.pmOp.ops(cps);
			if (opts.visualise){
				JFrame frame = OPSDisplayUtils.display(ops);
			}
			opts.bmOp.run(ops);
		} finally {
			this.opts.shutdown();
		}
	}
}
