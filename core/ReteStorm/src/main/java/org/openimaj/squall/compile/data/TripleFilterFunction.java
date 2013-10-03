package org.openimaj.squall.compile.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openimaj.rdf.storm.utils.VariableIndependentReteRuleToStringUtils;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.TriplePattern;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * Filter a triple, return bindings against variables
 *
 */
public class TripleFilterFunction implements VariableFunction<Triple, Map<String, String>> {
	private TriplePattern clause;

	/**
	 * @param clause construct using a {@link TriplePattern}
	 */
	public TripleFilterFunction(TriplePattern clause) {
		this.clause = clause;
	}

	@Override
	public Map<String, String> apply(Triple in) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> variables() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String anonimised(Map<String, Integer> varmap) {
		return VariableIndependentReteRuleToStringUtils.clauseEntryToString(clause,varmap);
	}

	@Override
	public String anonimised() {
		return VariableIndependentReteRuleToStringUtils.clauseEntryToString(clause);
	}
}