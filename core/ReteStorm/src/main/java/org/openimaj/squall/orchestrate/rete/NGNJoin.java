package org.openimaj.squall.orchestrate.rete;


import java.util.List;

import org.openimaj.squall.orchestrate.WindowInformation;
import org.openimaj.squall.compile.data.IVFunction;
import org.openimaj.squall.orchestrate.NNIVFunction;
import org.openimaj.squall.orchestrate.NamedNode;
import org.openimaj.squall.orchestrate.NamedStream;
import org.openimaj.squall.orchestrate.OrchestratedProductionSystem;
import org.openimaj.util.data.Context;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 */
public class NGNJoin extends NNIVFunction {
	
	/**
	 * @param parent
	 * @param name 
	 * @param left
	 * @param right
	 * @param wi 
	 */
	public NGNJoin(OrchestratedProductionSystem parent, String name, NamedNode<? extends IVFunction<Context, Context>> left,NamedNode<? extends IVFunction<Context, Context>> right, WindowInformation wi) {
		super(parent, name, new StreamAwareFixedJoinFunction(left.getData(), left.getVariableHolder().identifier(), wi, right.getData(), right.getVariableHolder().identifier(), wi));
		
		List<String> lsv = ((StreamAwareFixedJoinFunction) this.getData()).leftSharedVars();
		String[] leftSharedVars = lsv.toArray(new String[lsv.size()]);
		
		List<String> rsv = ((StreamAwareFixedJoinFunction) this.getData()).rightSharedVars();
		String[] rightSharedVars = rsv.toArray(new String[rsv.size()]);
		
		left.connect(new NamedStream(left.getVariableHolder().identifier(), leftSharedVars), this);
		right.connect(new NamedStream(right.getVariableHolder().identifier(), rightSharedVars), this);
	}

}
