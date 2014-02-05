package org.openimaj.squall.functions.rif.consequences;

import java.util.HashMap;
import java.util.Map;

import org.openimaj.rdf.storm.utils.Count;
import org.openimaj.squall.compile.data.AnonimisedRuleVariableHolder;
import org.openimaj.squall.compile.data.IConsequence;
import org.openimaj.squall.compile.data.InheritsVariables;
import org.openimaj.squall.compile.data.RuleWrappedFunction;
import org.openimaj.squall.compile.data.rif.AbstractRIFFunction;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 */
public abstract class BaseConsequenceFunction extends AbstractRIFFunction implements IConsequence {

	private String ruleID;
	
	/**
	 * @param rID
	 */
	public BaseConsequenceFunction(String rID){
		this.ruleID = rID;
	}
	
	/**
	 * @return
	 */
	public String getRuleID(){
		return this.ruleID;
	}
	
	@Override
	public boolean isStateless() {
		return true;
	}

	@Override
	public boolean forcedUnique() {
		return false;
	}

	/**
	 * @param varMap
	 */
	public abstract void mapVarNames(Map<String, String> varMap);
	
	@Override
	public void write(Kryo kryo, Output output) {
		output.writeString(this.ruleID);
	}
	
	@Override
	public void read(Kryo kryo, Input input) {
		this.ruleID = input.readString();
	}
	
	/**
	 * @author David Monks <dm11g08@ecs.soton.ac.uk>
	 *
	 * @param <T>
	 */
	public static abstract class RuleWrappedConsequenceFunction<T extends BaseConsequenceFunction>
										extends RuleWrappedFunction<T>
										implements InheritsVariables {

		private ConsequenceARVH varHolder;
		
		/**
		 * @param arvh
		 */
		public RuleWrappedConsequenceFunction(ConsequenceARVH arvh) {
			super(arvh);
			this.varHolder = arvh;
		}
		
		@Override
		public boolean areSourceVariablesSet() {
			return this.varHolder.areSourceVariablesSet();
		}

		@Override
		public String getSourceVarHolderIdent() {
			return this.varHolder.getSourceVarHolderIdent();
		}

		@Override
		public String getSourceVarHolderIdent(Map<String, String> varMap) {
			return this.varHolder.getSourceVarHolderIdent(varMap);
		}

		@Override
		public boolean setSourceVariables(AnonimisedRuleVariableHolder arvh) {
			if (this.areSourceVariablesSet()){
				throw new RuntimeException("Cannot set more than one source variable template for a predicate function.");
			}
			Map<String, String> arvhVarMap = arvh.ruleToBaseVarMap();
			Map<String, String> thisVarMap = this.ruleToBaseVarMap();
			
			Map<String, String> directVarMap = new HashMap<String, String>();
			for (String ruleVar : thisVarMap.keySet()){
				if (arvhVarMap.containsKey(ruleVar)){
					directVarMap.put(thisVarMap.get(ruleVar), arvhVarMap.get(ruleVar));
				} else {
					return false;
				}
			}
			
			super.getWrapped().mapVarNames(directVarMap);
			
			return this.varHolder.setSourceVariables(arvh);
		}
		
		protected static abstract class ConsequenceARVH extends ARVHComponent implements InheritsVariables {
			
			private final String ruleID;
			private AnonimisedRuleVariableHolder sourceVarHolder;
			
			protected ConsequenceARVH(String rID){
				this.ruleID = rID;
			}
			
			protected Node registerVariable(Node n, Count count){
				if(n.isVariable()){
					this.addVariable(n.getName());
					this.putRuleToBaseVarMapEntry(n.getName(), n.getName());
				}
				return n;
			}
			
			@Override
			public boolean areSourceVariablesSet() {
				return this.sourceVarHolder != null;
			}
			
			@Override
			public boolean setSourceVariables(AnonimisedRuleVariableHolder arvh) {
				Map<String, String> arvhVarMap = arvh.ruleToBaseVarMap();
				for (String ruleVar : super.variables()){
					String baseVar = arvhVarMap.get(ruleVar);
					super.putRuleToBaseVarMapEntry(ruleVar, baseVar);
				}
				
				this.sourceVarHolder = arvh;
				return true;
			}
			
			@Override
			public String getSourceVarHolderIdent() {
				return this.areSourceVariablesSet() ? this.sourceVarHolder.identifier() : "No Source";
			}
			
			@Override
			public String getSourceVarHolderIdent(Map<String, String> varMap) {
				return this.areSourceVariablesSet() ? this.sourceVarHolder.identifier(varMap) : "No Source";
			}
			
			/**
			 * @return
			 */
			public StringBuilder getRuleBody(){
				return new StringBuilder(this.ruleID)
						.append(": ")
						.append(this.getSourceVarHolderIdent())
						.append(" -> ");
			}
			
			/**
			 * @param varmap
			 * @return
			 */
			public StringBuilder getRuleBody(Map<String, String> varmap){
				return new StringBuilder(this.ruleID)
						.append(": ")
						.append(this.getSourceVarHolderIdent(varmap))
						.append(" -> ");
			}
			
		}

	}

}
