package org.openimaj.squall.compile.data.jena;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openimaj.rdf.storm.utils.VariableIndependentReteRuleToStringUtils;
import org.openimaj.squall.compile.data.IVFunction;
import org.openimaj.util.data.Context;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.Functor;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.reasoner.rulesys.impl.BindingVector;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TripleConsequence extends AbstractTripleFunction {

	/**
	 * @param r 
	 * @param clause
	 */
	public TripleConsequence(Rule r,TriplePattern clause) {
		super(r, clause);
	}

	@Override
	public List<Context> apply(Context in) {
		Map<String,Node> bindings = in.getTyped("bindings");
		BindingVector env = BindingsUtils.mapToBindings(bindings, ruleVariables);
		Triple t = env.instantiate(this.clause);
		List<Triple> ret = new ArrayList<Triple>();
		if (!Functor.isFunctor(t.getSubject())) {
			ret.add(t);
		}
		
		List<Context> ctxs = new ArrayList<Context>();
		for (Triple context : ret) {
			Context out = new Context();
			out.put("triple", context);			
			ctxs.add(out);
		}
		return ctxs;
	}
	
	@Override
	public String toString() {
		return this.clause.toString();
	}

}
