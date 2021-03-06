package org.openimaj.rifcore.rules;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openimaj.rifcore.conditions.data.RIFVar;

/**
 * @author david.monks
 *
 */
public class RIFForAll extends RIFSentence {
	
	private Set<RIFVar> universalVars;
	private RIFStatement statement;
	
	/**
	 * 
	 */
	public RIFForAll(){
		this.universalVars = new HashSet<RIFVar>();
	}
	
	/**
	 * @param s
	 */
	public void setStatement(RIFStatement s){
		this.statement = s;
	}
	
	/**
	 * @return
	 */
	public RIFStatement getStatement(){
		return this.statement;
	}
	
	/**
	 * @param var
	 */
	public void addUniversalVar(RIFVar var){
		this.universalVars.add(var);
	}
	
	/**
	 * @return
	 */
	public Iterable<RIFVar> universalVars(){
		return new Iterable<RIFVar>(){
			@Override
			public Iterator<RIFVar> iterator() {
				return RIFForAll.this.universalVars.iterator();
			}
		};
	}
	
	/**
	 * @param varName
	 * @return
	 */
	public boolean containsVar(String varName){
		for (RIFVar var : universalVars())
			if (var.getNode() != null && var.getNode().getName().equals(varName)) return true;
		return false;
	}

	/**
	 * @param varName
	 * @return
	 */
	public RIFVar getUniversalVar(String varName) {
		for (RIFVar var : universalVars())
			if (var.getNode().getName().equals(varName)) return var;
		return null;
	}

}
