package org.openimaj.rifcore.conditions.formula;

import org.openimaj.rifcore.conditions.data.RIFDatum;
import org.openimaj.rifcore.rules.RIFStatement;

/**
 * @author david.monks
 *
 */
public class RIFMember extends RIFStatement implements RIFFormula {
	
	private RIFDatum instance;
	private RIFDatum inClass;
	
	/**
	 * 
	 */
	public RIFMember(){
		
	}
	
	/**
	 * @param i
	 */
	public void setInstance(RIFDatum i){
		this.instance = i;
	}
	
	/**
	 * @param c
	 */
	public void setInClass(RIFDatum c){
		this.inClass = c;
	}
	
	/**
	 * @return
	 */
	public RIFDatum getInstance(){
		return this.instance;
	}
	
	/**
	 * @return
	 */
	public RIFDatum getInClass(){
		return this.inClass;
	}

	@Override
	public void addFormula(RIFFormula formula) {
		throw new UnsupportedOperationException("RIF: Cannot encapsulate formuli within a RIF membership statement.");
	}
	
	@Override
	public String toString(String spacing) {
		return new StringBuilder(instance.getNode().toString())
						.append(" # ")
						.append(inClass.getNode().toString())
						.toString();
	}
	
	@Override
	public String toString() {
		return new StringBuilder(instance.getNode().toString())
						.append(" # ")
						.append(inClass.getNode().toString())
						.toString();
	}

}
