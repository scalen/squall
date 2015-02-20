package org.openimaj.rifcore.conditions.data;

import java.net.URI;
import java.net.URISyntaxException;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Node_Concrete;

/**
 * @author david.monks
 *
 */
public class RIFXSDLiteralConst extends RIFConst<String> {

	private URI dtype = null;
	
	/**
	 * 
	 */
	public RIFXSDLiteralConst(){
		try {
			dtype = new URI(new XSDDatatype("string").getURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException("SHOULD NEVER HAPPEN, static uri should be valid", e);
		} 
	}
	
	@Override
	public String getDatatype(){
		return this.dtype.toString();
	}

	@Override
	public RIFXSDLiteralConst setData(String data) {
		String[] dataparts = data.split("\\^\\^");
		try {
			this.dtype = new URI(dataparts[1]);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Literal Value must have a valid xsd datatype, which " + dataparts[1] + " is not.", e);
		}
		
		this.node = (Node_Concrete) NodeFactory.createLiteral(dataparts[0],new XSDDatatype(this.dtype.getFragment()));
		return this;
	}

}
