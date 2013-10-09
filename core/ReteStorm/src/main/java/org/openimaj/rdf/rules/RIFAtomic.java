package org.openimaj.rdf.rules;

/**
 * @author david.monks
 *
 */
public abstract class RIFAtomic implements RIFFormula, RIFStatement {

	@Override
	public void addFormula(RIFFormula formula){
		throw new UnsupportedOperationException("RIF: Cannot encapsulate any formuli within an atomic RIF statement.");
	}
	
}
