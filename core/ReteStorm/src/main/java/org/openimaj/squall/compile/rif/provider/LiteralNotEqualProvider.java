package org.openimaj.squall.compile.rif.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openimaj.rifcore.conditions.atomic.RIFAtom;
import org.openimaj.rifcore.conditions.data.RIFExternalExpr;
import org.openimaj.rifcore.conditions.formula.RIFExternalValue;
import org.openimaj.squall.compile.data.IVFunction;
import org.openimaj.squall.functions.rif.predicates.BaseRIFPredicateFunction;
import org.openimaj.squall.functions.rif.predicates.BaseRIFPredicateFunction.RIFPredicateException;
import org.openimaj.util.data.Context;

import com.hp.hpl.jena.graph.Node;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 */
public class LiteralNotEqualProvider extends ExternalFunctionProvider {

	private static final class LiteralNotEqualFunction extends BaseRIFPredicateFunction {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 2256444262394116745L;
		
		private Node[] nodes;
		
		public LiteralNotEqualFunction(Node[] ns) throws RIFPredicateException {
			super(ns);
			this.nodes = ns;
		}
		
		@Override
		public List<Context> apply(Context in){
			logger .debug(String.format("Context(%s) sent to Predicate(neq(%s))" , in, Arrays.toString(this.nodes)));
			List<Context> ret = new ArrayList<Context>();
			Map<String,Node> binds = in.getTyped("bindings");
			Object first = extractBinding(binds, nodes[0]);
			Object second = extractBinding(binds, nodes[1]);
			if(!first.equals(second)) ret.add(in);
			return ret;
		}
		
	}

	private static final Logger logger = Logger.getLogger(LiteralNotEqualFunction.class);
	
	@Override
	public IVFunction<Context, Context> apply(RIFExternalExpr in) {
		RIFAtom atom = in.getExpr().getCommand();
		try {
			return new LiteralNotEqualFunction(extractNodes(atom));
		} catch (RIFPredicateException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	@Override
	public IVFunction<Context, Context> apply(RIFExternalValue in) {
		RIFAtom atom = in.getVal();
		try {
			return new LiteralNotEqualFunction(extractNodes(atom));
		} catch (RIFPredicateException e) {
			throw new UnsupportedOperationException(e);
		}
	}

}
