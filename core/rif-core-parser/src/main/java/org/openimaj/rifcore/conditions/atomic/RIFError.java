package org.openimaj.rifcore.conditions.atomic;

import java.net.URI;
import java.net.URISyntaxException;

import org.openimaj.rifcore.conditions.data.RIFConst;
import org.openimaj.rifcore.conditions.data.RIFIRIConst;

/**
 * @author david.monks
 *
 */
public class RIFError extends RIFAtom {

	/**
	 * 
	 */
	public RIFError(){
		super();
		
		RIFIRIConst errorOp = new RIFIRIConst();
		try {
			URI errorURI = new URI("http://www.w3.org/2007/rif#error");
			errorOp.setData(errorURI);
			super.setOp(errorOp);
		} catch (URISyntaxException e) {
			throw new RuntimeException("SHOULD NOT ERROR HERE!",e);
		}
	}
	
	/**
	 * @param op
	 */
	public void setOp(RIFConst<?> op){
		throw new UnsupportedOperationException("Cannot change the operand of the error atom");
	}
	
}
