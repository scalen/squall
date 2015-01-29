package org.openimaj.rifcore.conditions.formula;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author david.monks
 *
 */
public class RIFAnd implements RIFFormula, Iterable<RIFFormula> {
	
	private Set<RIFFormula> formuli;
	
	/**
	 * 
	 */
	public RIFAnd(){
		this.formuli = new HashSet<RIFFormula>();
	}
	
	/**
	 * @param f
	 */
	public void addFormula(RIFFormula f){
		this.formuli.add(f);
	}
	
	@Override
	public Iterator<RIFFormula> iterator(){
		return this.formuli.iterator();
	}

	public String toString(String spacing) {
		StringBuilder string = new StringBuilder("And (\n");
		for (RIFFormula formula : this){
			string.append(spacing)
				  .append(" ")
				  .append(formula.toString(spacing + "  "))
				  .append("\n");
		}
		return string.append(spacing)
					 .append(")")
					 .toString();
	}
	
	public String toString() {
		StringBuilder string = new StringBuilder("And (\n");
		for (RIFFormula formula : this){
			string.append("  ")
				  .append(formula.toString("  "))
				  .append("\n");
		}
		return string.append(")")
					 .toString();
	}

}
