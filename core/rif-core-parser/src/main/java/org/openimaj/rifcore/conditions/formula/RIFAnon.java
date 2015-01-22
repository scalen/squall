package org.openimaj.rifcore.conditions.formula;

import java.net.URI;
import java.net.URISyntaxException;

import org.openimaj.rifcore.conditions.data.RIFDatum;
import org.openimaj.rifcore.conditions.data.RIFIRIConst;

/**
 * specifies membership of the generic anonymous class
 * 
 * @author david.monks
 *
 */
public class RIFAnon extends RIFMember {
	
	/**
	 * 
	 */
	public RIFAnon () {
		super();
		
		RIFIRIConst c = new RIFIRIConst();
		try {
			URI anonURI = new URI("http://www.w3.org/2007/rif#AnonymousClass");
			c.setData(anonURI);
			
			super.setInClass(c);
		} catch (URISyntaxException e) {
			throw new RuntimeException("SHOULD NOT BE POSSIBLE IN PRODUCTION.", e);
		}
	}
	
	/**
	 * @param c
	 */
	public void setInClass(RIFDatum c){
		throw new UnsupportedOperationException("Cannot specify membership of class "+ c.getNode() +" in an anonymous class membership statement.");
	}

}
