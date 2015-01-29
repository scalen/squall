package org.openimaj.rifcore.conditions.formula;

import org.openimaj.rifcore.conditions.RIFExternal;
import org.openimaj.rifcore.conditions.atomic.RIFAtom;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 */
public class RIFExternalValue implements RIFExternal, RIFFormula {
	
	private RIFAtom val;
	
	/**
	 * @param a 
	 */
	public void setVal(RIFAtom a){
		this.val = a;
	}
	
	/**
	 * @return
	 */
	public RIFAtom getVal(){
		return this.val;
	}

	@Override
	public void addFormula(RIFFormula formula) {
		throw new UnsupportedOperationException("RIF: Cannot encapsulate formuli within a RIF external statement.");
	}
	
	@Override
	public String toString(String spacing) {
		return new StringBuilder("External (\n")
						.append(spacing)
						.append("  ")
						.append(val.toString(spacing + "  "))
						.append("\n")
						.append(spacing)
						.append(")")
						.toString();
	}
	
	@Override
	public String toString() {
		return new StringBuilder("External (\n")
						.append("  ")
						.append(val.toString("  "))
						.append("\n")
						.append(")")
						.toString();
	}
	
}
